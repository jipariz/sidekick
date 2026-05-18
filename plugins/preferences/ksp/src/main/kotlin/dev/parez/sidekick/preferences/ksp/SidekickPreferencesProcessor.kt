package dev.parez.sidekick.preferences.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate
import dev.parez.sidekick.preferences.ksp.generator.AccessorGenerator
import dev.parez.sidekick.preferences.ksp.generator.PluginGenerator
import dev.parez.sidekick.preferences.ksp.model.PreferenceProperty
import java.io.File

class SidekickPreferencesProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {

    private val annotationName = "dev.parez.sidekick.preferences.SidekickPreferences"

    /** Primitive types the KSP code generator can map to a `PreferenceDefinition`. Enums are handled separately. */
    private val supportedPrimitiveTypes = setOf("Boolean", "String", "Int", "Long", "Float", "Double")

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(annotationName)
        val unprocessed = symbols.filter { !it.validate() }.toList()

        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { processClass(it as KSClassDeclaration) }

        return unprocessed
    }

    private fun processClass(classDecl: KSClassDeclaration) {
        val annotation = classDecl.annotations.first {
            it.shortName.asString() == "SidekickPreferences"
        }
        val title = (annotation.arguments.firstOrNull { it.name?.asString() == "title" }
            ?.value as? String)
            ?.takeIf { it.isNotEmpty() }
            ?: classDecl.simpleName.asString()
                .replace(Regex("([A-Z])"), " $1")
                .trim()

        val storeName = (annotation.arguments.firstOrNull { it.name?.asString() == "storeName" }
            ?.value as? String)
            ?.takeIf { it.isNotEmpty() }

        val candidateProps = classDecl.getAllProperties()
            .filter { prop ->
                prop.annotations.none { it.shortName.asString() == "IgnorePreference" }
            }
            .toList()

        // ── Type validation ──────────────────────────────────────────────────
        // Fail loudly on unsupported property types instead of silently
        // generating a StateFlow<String> that mis-stores the user's value.
        var hasErrors = false
        candidateProps.forEach { prop ->
            val resolvedType = prop.type.resolve()
            val typeDecl = resolvedType.declaration
            val typeName = typeDecl.simpleName.asString()
            val isEnum = typeDecl is KSClassDeclaration &&
                typeDecl.classKind == ClassKind.ENUM_CLASS
            if (!isEnum && typeName !in supportedPrimitiveTypes) {
                logger.error(
                    "Unsupported preference type `$typeName` on " +
                        "${classDecl.qualifiedName?.asString()}.${prop.simpleName.asString()}. " +
                        "Supported types: Boolean, String, Int, Long, Float, Double, " +
                        "or any enum class. Annotate the property with @IgnorePreference " +
                        "to keep it out of the generated accessor.",
                    prop,
                )
                hasErrors = true
            }
        }
        if (hasErrors) return

        val properties = candidateProps.map { buildPreferenceProperty(it) }

        val packageName = classDecl.packageName.asString()
        val className = classDecl.simpleName.asString()

        logger.info("Generating Sidekick classes for $className (title=$title, storeName=${storeName ?: "<derived>"})")

        AccessorGenerator(codeGenerator, logger).generate(packageName, className, title, properties, storeName)
        PluginGenerator(codeGenerator, logger).generate(packageName, className, title, properties)
    }

    private fun buildPreferenceProperty(prop: KSPropertyDeclaration): PreferenceProperty {
        val prefAnnotation = prop.annotations.firstOrNull {
            it.shortName.asString() == "Preference"
        }
        val annotationLabel = (prefAnnotation?.arguments?.firstOrNull {
            it.name?.asString() == "label"
        }?.value as? String)?.takeIf { it.isNotEmpty() }
        val annotationDescription = (prefAnnotation?.arguments?.firstOrNull {
            it.name?.asString() == "description"
        }?.value as? String) ?: ""

        val resolvedType = prop.type.resolve()
        val typeDecl = resolvedType.declaration
        val typeName = typeDecl.simpleName.asString()
        val qualifiedName = typeDecl.qualifiedName?.asString()

        val isEnum = typeDecl is KSClassDeclaration &&
            typeDecl.classKind == ClassKind.ENUM_CLASS

        val enumValues: List<String> = if (isEnum) {
            (typeDecl as KSClassDeclaration).declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.ENUM_ENTRY }
                .map { it.simpleName.asString() }
                .toList()
        } else emptyList()

        val rawInitializer = readPropertyInitializerSource(prop, prop.simpleName.asString())
        val defaultValue = normalisedDefault(rawInitializer, typeName, isEnum, enumValues)

        return PreferenceProperty(
            name = prop.simpleName.asString(),
            type = typeName,
            qualifiedType = qualifiedName,
            isEnum = isEnum,
            enumValues = enumValues,
            defaultValue = defaultValue,
            label = annotationLabel ?: humaniseName(prop.simpleName.asString()),
            description = annotationDescription,
        )
    }

    /**
     * KSP doesn't expose property initializer expressions. Read the line of source the
     * property starts on (and a small forward window for short multi-line initializers),
     * locate `var <name>` / `val <name>`, and extract the substring after the first
     * un-typed `=` that follows. The `var <name>` anchor is critical: KSP's `location`
     * for an annotated property points at the annotation's line, so naively scanning
     * for `=` would otherwise hit the `=` inside an annotation argument first.
     * Returns null when the initializer can't be located — callers fall back to a
     * type-zero default.
     */
    private fun readPropertyInitializerSource(prop: KSPropertyDeclaration, propName: String): String? {
        val location = prop.location as? FileLocation ?: return null
        val file = File(location.filePath)
        if (!file.isFile) return null

        val lines = runCatching { file.readLines() }.getOrNull() ?: return null
        val startIdx = location.lineNumber - 1
        if (startIdx !in lines.indices) return null

        // Concatenate up to 8 lines starting at the declaration line. Strip line comments
        // before joining. This handles trivial multi-line initializers without trying to
        // parse a real Kotlin expression.
        val windowEnd = (startIdx + 8).coerceAtMost(lines.size)
        val joined = (startIdx until windowEnd)
            .joinToString(" ") { stripLineComment(lines[it]) }

        // Anchor at `var <propName>` or `val <propName>` — only after that do we trust
        // the first `=` to be the initializer separator.
        val anchorRegex = Regex("\\b(?:var|val)\\s+${Regex.escape(propName)}\\b")
        val anchorMatch = anchorRegex.find(joined) ?: return null

        var i = anchorMatch.range.last + 1
        while (i < joined.length) {
            val c = joined[i]
            if (c == '=' &&
                joined.getOrNull(i + 1) != '=' &&
                joined.getOrNull(i - 1).let { it != '=' && it != '<' && it != '>' && it != '!' }
            ) {
                return joined.substring(i + 1).trim().takeIf { it.isNotEmpty() }
            }
            i++
        }
        return null
    }

    private fun stripLineComment(line: String): String {
        val idx = line.indexOf("//")
        return if (idx >= 0) line.substring(0, idx) else line
    }

    /**
     * Convert the raw initializer expression to the value-string the generators expect
     * (e.g. `"false"`, `"42"`, `DEFAULT`, the unquoted string contents). The raw text
     * may include trailing source we don't care about (rest-of-line after the literal,
     * comments, next-property declarations); `firstLiteralToken` peels off only the
     * leading expression token before we type-classify it.
     */
    private fun normalisedDefault(
        raw: String?,
        typeName: String,
        isEnum: Boolean,
        enumValues: List<String>,
    ): String {
        if (raw.isNullOrBlank()) return typeZero(typeName, isEnum, enumValues)
        val token = firstLiteralToken(raw.trim()) ?: return typeZero(typeName, isEnum, enumValues)
        return when {
            isEnum -> {
                // Accept "ColorTheme.DEFAULT" or "DEFAULT"; take the last dot-separated segment.
                val candidate = token.substringAfterLast('.')
                if (candidate.isNotEmpty() && (enumValues.isEmpty() || candidate in enumValues)) candidate
                else enumValues.firstOrNull() ?: ""
            }
            typeName == "Boolean" -> when (token) {
                "true", "false" -> token
                else -> "false"
            }
            typeName == "String" -> {
                // Strip surrounding quotes from a Kotlin string literal; leave non-literals to type-zero.
                if (token.length >= 2 && token.first() == '"' && token.last() == '"') {
                    decodeStringLiteral(token.substring(1, token.length - 1))
                } else ""
            }
            typeName == "Int" -> token.removeSuffix("L").removeSuffix("l")
                .toIntOrNull()?.toString() ?: "0"
            typeName == "Long" -> token.removeSuffix("L").removeSuffix("l")
                .toLongOrNull()?.toString() ?: "0"
            typeName == "Float" -> token.removeSuffix("f").removeSuffix("F")
                .toFloatOrNull()?.toString() ?: "0"
            typeName == "Double" -> token.removeSuffix("f").removeSuffix("F")
                .toDoubleOrNull()?.toString() ?: "0"
            else -> typeZero(typeName, isEnum, enumValues)
        }
    }

    /**
     * Extract the leading expression token. Handles two shapes:
     *  - A `"..."` string literal, returned with quotes intact (escape-aware).
     *  - A sequence of identifier / number characters (`[A-Za-z0-9._+\-]`), good enough
     *    for primitives (`42L`, `0.5f`, `-3`) and qualified identifiers (`ColorTheme.DEFAULT`).
     */
    private fun firstLiteralToken(s: String): String? {
        if (s.isEmpty()) return null
        if (s.first() == '"') {
            var i = 1
            while (i < s.length) {
                when (s[i]) {
                    '\\' -> i += 2
                    '"' -> return s.substring(0, i + 1)
                    else -> i++
                }
            }
            return null
        }
        var i = 0
        while (i < s.length && (s[i].isLetterOrDigit() || s[i] in "._+-")) i++
        return if (i > 0) s.substring(0, i) else null
    }

    private fun decodeStringLiteral(content: String): String = buildString(content.length) {
        var i = 0
        while (i < content.length) {
            val c = content[i]
            if (c == '\\' && i + 1 < content.length) {
                when (val esc = content[i + 1]) {
                    'n' -> append('\n')
                    't' -> append('\t')
                    'r' -> append('\r')
                    '\\' -> append('\\')
                    '"' -> append('"')
                    '\'' -> append('\'')
                    else -> { append('\\'); append(esc) }
                }
                i += 2
            } else {
                append(c); i++
            }
        }
    }

    private fun typeZero(typeName: String, isEnum: Boolean, enumValues: List<String>): String = when {
        isEnum -> enumValues.firstOrNull() ?: ""
        typeName == "Boolean" -> "false"
        typeName == "String" -> ""
        else -> "0"
    }

    private fun humaniseName(name: String): String =
        name.replace(Regex("([A-Z])"), " $1")
            .trim()
            .replaceFirstChar { it.uppercaseChar() }
}
