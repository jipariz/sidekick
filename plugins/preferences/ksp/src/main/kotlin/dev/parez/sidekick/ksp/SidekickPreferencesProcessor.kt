package dev.parez.sidekick.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import dev.parez.sidekick.ksp.generator.AccessorGenerator
import dev.parez.sidekick.ksp.generator.PluginGenerator
import dev.parez.sidekick.ksp.model.PreferenceProperty

class SidekickPreferencesProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {

    private val annotationName = "dev.parez.sidekick.preferences.SidekickPreferences"

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

        val properties = classDecl.getAllProperties()
            .filter { prop ->
                prop.annotations.any { it.shortName.asString() == "Preference" }
            }
            .map { prop ->
                val prefAnnotation = prop.annotations.first {
                    it.shortName.asString() == "Preference"
                }
                val label = (prefAnnotation.arguments.firstOrNull {
                    it.name?.asString() == "label"
                }?.value as? String)?.takeIf { it.isNotEmpty() } ?: prop.simpleName.asString()

                val description = prefAnnotation.arguments.firstOrNull {
                    it.name?.asString() == "description"
                }?.value as? String ?: ""

                val defaultValue = prefAnnotation.arguments.firstOrNull {
                    it.name?.asString() == "defaultValue"
                }?.value as? String ?: ""

                val typeName = prop.type.resolve().declaration.simpleName.asString()

                PreferenceProperty(
                    name = prop.simpleName.asString(),
                    type = typeName,
                    defaultValue = defaultValue,
                    label = label,
                    description = description,
                )
            }
            .toList()

        val packageName = classDecl.packageName.asString()
        val className = classDecl.simpleName.asString()

        logger.info("Generating Sidekick classes for $className (title=$title)")

        AccessorGenerator(codeGenerator, logger).generate(packageName, className, title, properties)
        PluginGenerator(codeGenerator, logger).generate(packageName, className, title, properties)
    }
}
