package dev.parez.sidekick.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import dev.parez.sidekick.ksp.model.PreferenceProperty

class AccessorGenerator(
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
        val storeName = title.lowercase().replace(" ", "_")

        val storeType = ClassName("dev.parez.sidekick.preferences", "PreferenceStore")
        val stateFlowClass = ClassName("kotlinx.coroutines.flow", "StateFlow")

        val storeParam = ParameterSpec.builder("_store", storeType)
            .defaultValue("createPreferenceStore(%S)", storeName)
            .build()

        val classBuilder = TypeSpec.classBuilder(accessorName)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(storeParam)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("_store", storeType)
                    .initializer("_store")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )

        for (prop in properties) {
            val kotlinType = prop.kotlinTypeName()
            val stateFlowType = stateFlowClass.parameterizedBy(kotlinType)

            classBuilder.addProperty(
                prop.buildObserveProperty(stateFlowType)
            )

            val setterName = "set${prop.name.replaceFirstChar { it.uppercaseChar() }}"
            classBuilder.addFunction(
                FunSpec.builder(setterName)
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter("value", kotlinType)
                    .addStatement("_store.set(%S, value)", prop.name)
                    .build()
            )
        }

        val fileSpec = FileSpec.builder(packageName, accessorName)
            .addImport("dev.parez.sidekick.preferences", "createPreferenceStore")
            .addType(classBuilder.build())
            .build()

        codeGenerator.createNewFile(
            Dependencies(false),
            packageName,
            accessorName,
        ).bufferedWriter().use { fileSpec.writeTo(it) }
    }
}

private fun PreferenceProperty.kotlinTypeName(): ClassName = when (type) {
    "Boolean" -> Boolean::class.asClassName()
    "Int"     -> Int::class.asClassName()
    "Long"    -> Long::class.asClassName()
    "Float"   -> Float::class.asClassName()
    "Double"  -> Double::class.asClassName()
    else      -> String::class.asClassName()
}

private fun PreferenceProperty.buildObserveProperty(
    stateFlowType: com.squareup.kotlinpoet.TypeName,
): PropertySpec {
    // Use KotlinPoet %S for String (adds quotes), %L for numeric/Boolean literals
    return when (type) {
        "Boolean" -> PropertySpec.builder(name, stateFlowType)
            .initializer("_store.observe(%S, %L)", name, defaultValue.toBooleanStrictOrNull() ?: false)
            .build()
        "Int" -> PropertySpec.builder(name, stateFlowType)
            .initializer("_store.observe(%S, %L)", name, defaultValue.toIntOrNull() ?: 0)
            .build()
        "Long" -> PropertySpec.builder(name, stateFlowType)
            .initializer("_store.observe(%S, %LL)", name, defaultValue.toLongOrNull() ?: 0L)
            .build()
        "Float" -> PropertySpec.builder(name, stateFlowType)
            .initializer("_store.observe(%S, %Lf)", name, defaultValue.toFloatOrNull() ?: 0f)
            .build()
        "Double" -> PropertySpec.builder(name, stateFlowType)
            .initializer("_store.observe(%S, %L)", name, defaultValue.toDoubleOrNull() ?: 0.0)
            .build()
        else -> PropertySpec.builder(name, stateFlowType)
            .initializer("_store.observe(%S, %S)", name, defaultValue)
            .build()
    }
}
