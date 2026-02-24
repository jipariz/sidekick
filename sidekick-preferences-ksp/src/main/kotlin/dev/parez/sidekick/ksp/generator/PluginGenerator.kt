package dev.parez.sidekick.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import dev.parez.sidekick.ksp.model.PreferenceProperty

class PluginGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) {

    fun generate(
        packageName: String,
        className: String,
        title: String,
        properties: List<PreferenceProperty>,
    ) {
        val accessorName = "${className}Accessor"
        val pluginName = "${className}Plugin"
        val accessorType = ClassName(packageName, accessorName)
        val prefsPluginType = ClassName("dev.parez.sidekick.preferences", "PreferencesPlugin")

        val accessorParam = com.squareup.kotlinpoet.ParameterSpec.builder("accessor", accessorType)
            .defaultValue("%T()", accessorType)
            .build()

        // definitions = listOf(...)
        val definitionsBlock = CodeBlock.builder().apply {
            add("listOf(\n")
            indent()
            for (prop in properties) {
                val prefClass = ClassName("dev.parez.sidekick.preferences", prop.definitionClassName())
                add("%T(%S, %S, %S, ${prop.defaultLiteral()}),\n", prefClass, prop.name, prop.label, prop.description)
            }
            unindent()
            add(")")
        }.build()

        // valueFlows = mapOf(...)
        val valueFlowsBlock = CodeBlock.builder().apply {
            add("mapOf(\n")
            indent()
            for (prop in properties) {
                add("%S to accessor.%N,\n", prop.name, prop.name)
            }
            unindent()
            add(")")
        }.build()

        // onSet = { key, value -> when(key) { ... } }
        val onSetBlock = CodeBlock.builder().apply {
            add("{ key, value ->\n")
            indent()
            add("@Suppress(%S)\n", "UNCHECKED_CAST")
            add("when (key) {\n")
            indent()
            for (prop in properties) {
                val setterName = "set${prop.name.replaceFirstChar { it.uppercaseChar() }}"
                add("%S -> accessor.%N(value as %T)\n", prop.name, setterName, prop.kotlinTypeName())
            }
            unindent()
            add("}\n")
            unindent()
            add("}")
        }.build()

        val classBuilder = TypeSpec.classBuilder(pluginName)
            .superclass(prefsPluginType)
            .addSuperclassConstructorParameter("pluginTitle = %S", title)
            .addSuperclassConstructorParameter("definitions = %L", definitionsBlock)
            .addSuperclassConstructorParameter("valueFlows = %L", valueFlowsBlock)
            .addSuperclassConstructorParameter("onSet = %L", onSetBlock)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(accessorParam)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("accessor", accessorType)
                    .initializer("accessor")
                    .build()
            )

        val fileSpec = FileSpec.builder(packageName, pluginName)
            .addType(classBuilder.build())
            .build()

        codeGenerator.createNewFile(
            Dependencies(false),
            packageName,
            pluginName,
        ).bufferedWriter().use { fileSpec.writeTo(it) }
    }
}

private fun PreferenceProperty.definitionClassName(): String = when (type) {
    "Boolean" -> "BooleanPref"
    "Int"     -> "IntPref"
    "Long"    -> "LongPref"
    "Float"   -> "FloatPref"
    "Double"  -> "DoublePref"
    else      -> "StringPref"
}

private fun PreferenceProperty.kotlinTypeName(): ClassName = when (type) {
    "Boolean" -> Boolean::class.asClassName()
    "Int"     -> Int::class.asClassName()
    "Long"    -> Long::class.asClassName()
    "Float"   -> Float::class.asClassName()
    "Double"  -> Double::class.asClassName()
    else      -> String::class.asClassName()
}

// Returns a KotlinPoet literal string (NOT a %S-style format—embedded directly in CodeBlock)
private fun PreferenceProperty.defaultLiteral(): String = when (type) {
    "Boolean" -> defaultValue.toBooleanStrictOrNull()?.toString() ?: "false"
    "Int"     -> defaultValue.toIntOrNull()?.toString() ?: "0"
    "Long"    -> (defaultValue.toLongOrNull()?.toString() ?: "0") + "L"
    "Float"   -> (defaultValue.toFloatOrNull()?.toString() ?: "0.0") + "f"
    "Double"  -> defaultValue.toDoubleOrNull()?.toString() ?: "0.0"
    else      -> "\"${defaultValue.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}
