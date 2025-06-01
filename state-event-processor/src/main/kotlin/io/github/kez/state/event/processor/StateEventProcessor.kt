package io.github.kez.state.event.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate
import io.github.kez.state.event.annotations.StateEvent

class StateEventProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val processedClasses = mutableSetOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.warn("StateEventProcessor: Starting processing...")

        val symbols = resolver.getSymbolsWithAnnotation(StateEvent::class.qualifiedName!!)
        val validSymbols = symbols.filter { it.validate() }.toList()
        val invalidSymbols = symbols.filterNot { it.validate() }.toList()

        logger.warn("StateEventProcessor: Found ${validSymbols.size} valid symbols and ${invalidSymbols.size} invalid symbols")

        // 클래스별로 그룹화하여 처리
        val propertiesByClass = validSymbols
            .filterIsInstance<KSPropertyDeclaration>()
            .groupBy { property ->
                getContainingClass(property)
            }
            .filterKeys { it != null }
            .mapKeys { it.key!! }

        propertiesByClass.forEach { (containingClass, properties) ->
            logger.warn("StateEventProcessor: Processing class ${containingClass.simpleName.asString()} with ${properties.size} state event properties")
            try {
                generateStateEventExtensions(containingClass, properties)
            } catch (e: Exception) {
                logger.error("StateEventProcessor: Error processing class ${containingClass.simpleName.asString()}: ${e.message}")
                e.printStackTrace()
            }
        }

        return invalidSymbols
    }

    private fun getContainingClass(property: KSPropertyDeclaration): KSClassDeclaration? {
        // primary constructor parameters를 처리하기 위해 다른 방법 시도
        return when (val parent = property.parent) {
            is KSClassDeclaration -> parent
            else -> {
                // primary constructor parameter인 경우, 한 단계 더 올라가서 찾기
                var current = property.parent
                while (current != null && current !is KSClassDeclaration) {
                    current = current.parent
                }
                current as? KSClassDeclaration
            }
        }
    }

    private fun generateStateEventExtensions(
        uiStateClass: KSClassDeclaration,
        properties: List<KSPropertyDeclaration>
    ) {
        // UiState 클래스가 data class인지 확인
        if (!uiStateClass.modifiers.contains(com.google.devtools.ksp.symbol.Modifier.DATA)) {
            logger.error("StateEvent can only be used on properties of data classes")
            return
        }

        val packageName = uiStateClass.packageName.asString()
        val className = uiStateClass.simpleName.asString()

        // Check if UIState class has @UIState annotation
        val hasUIStateAnnotation = uiStateClass.annotations.any { annotation ->
            annotation.annotationType.resolve().declaration.qualifiedName?.asString() == "io.github.kez.state.event.annotations.UIState"
        }

        logger.warn("StateEventProcessor: Generating extensions for $className with ${properties.size} properties (hasUIStateAnnotation: $hasUIStateAnnotation)")

        val code = buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import androidx.lifecycle.ViewModel")
            appendLine("import kotlinx.coroutines.flow.MutableStateFlow")
            appendLine("import kotlinx.coroutines.flow.update")
            appendLine("import kotlin.reflect.KMutableProperty1")
            appendLine()

            properties.forEach { property ->
                val functionName = getFunctionName(property)
                val propertyName = property.simpleName.asString()

                appendLine("/**")
                appendLine(" * Auto-generated consume function for state event: $propertyName")
                appendLine(" * Usage: this.$functionName() in your ViewModel")
                appendLine(" */")
                appendLine("fun ViewModel.$functionName() {")

                if (hasUIStateAnnotation) {
                    // @UIState 어노테이션이 있는 경우: 어노테이션 기반으로 찾기
                    appendLine("    // Find MutableStateFlow field with @UIState annotated type")
                    appendLine("    val uiStateField = this::class.java.declaredFields.find { field ->")
                    appendLine("        val fieldType = field.type.toString()")
                    appendLine("        fieldType.contains(\"MutableStateFlow\") && ")
                    appendLine("        try {")
                    appendLine("            val genericType = field.genericType.toString()")
                    appendLine("            genericType.contains(\"$className\")")
                    appendLine("        } catch (e: Exception) {")
                    appendLine("            fieldType.contains(\"$className\")")
                    appendLine("        }")
                    appendLine("    }")
                } else {
                    // @UIState 어노테이션이 없는 경우: 기존 방식
                    logger.error(
                        "No @UIState annotation found for property $propertyName in class $className",
                        property
                    )
                }

                appendLine("    ")
                appendLine("    if (uiStateField != null) {")
                appendLine("        uiStateField.isAccessible = true")
                appendLine("        @Suppress(\"UNCHECKED_CAST\")")
                appendLine("        val uiState = uiStateField.get(this) as? MutableStateFlow<$className>")
                appendLine("        uiState?.update { it.copy($propertyName = null) }")
                appendLine("            ?: throw IllegalStateException(\"Found MutableStateFlow field but failed to cast to MutableStateFlow<$className>\")")
                appendLine("    } else {")
                appendLine("        // Fallback: try Kotlin reflection for properties")
                appendLine("        try {")
                appendLine("            val prop = this::class.members.find { member ->")
                appendLine("                member.returnType.toString().contains(\"MutableStateFlow<$className>\")")
                appendLine("            } as? kotlin.reflect.KMutableProperty1<ViewModel, MutableStateFlow<$className>>")
                appendLine("            prop?.get(this)?.update { it.copy($propertyName = null) }")
                appendLine("                ?: throw IllegalStateException(\"No MutableStateFlow<$className> property found in ViewModel${if (hasUIStateAnnotation) " (with @UIState annotation)" else ""}\")")
                appendLine("        } catch (e: Exception) {")
                appendLine("            throw IllegalStateException(\"Failed to find MutableStateFlow<$className> property. Make sure your ViewModel has a MutableStateFlow<$className> property${if (hasUIStateAnnotation) " (with @UIState annotation)" else ""}\", e)")
                appendLine("        }")
                appendLine("    }")
                appendLine("}")
                appendLine()
            }
        }

        // ViewModel 확장 함수들 생성
        val extensionFile = codeGenerator.createNewFile(
            dependencies = Dependencies(
                false,
                *properties.mapNotNull { it.containingFile }.toTypedArray()
            ),
            packageName = packageName,
            fileName = "${className}StateEventExtensions"
        )

        extensionFile.write(code.toByteArray())
        extensionFile.close()

        logger.warn("StateEventProcessor: Generated extension functions for $className")
    }

    private fun getFunctionName(property: KSPropertyDeclaration): String {
        val propertyName = property.simpleName.asString()

        // @StateEvent 어노테이션에서 커스텀 함수명 가져오기
        val annotation = property.annotations.first {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == StateEvent::class.qualifiedName
        }

        val customFunctionName = annotation.arguments
            .find { it.name?.asString() == "consumeFunctionName" }
            ?.value as? String

        return if (customFunctionName.isNullOrEmpty()) {
            "consume${propertyName.replaceFirstChar { it.uppercase() }}"
        } else {
            customFunctionName
        }
    }
}