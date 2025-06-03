package io.github.kez.state.event.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import io.github.kez.state.event.annotations.EventType
import io.github.kez.state.event.annotations.StateEvent

class StateEventProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {


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
        if (!uiStateClass.modifiers.contains(Modifier.DATA)) {
            logger.error("StateEvent can only be used on properties of data classes")
            return
        }

        val packageName = uiStateClass.packageName.asString()
        val className = uiStateClass.simpleName.asString()

        logger.warn("StateEventProcessor: Generating safe extensions for $className with ${properties.size} properties")

        val code = buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import io.github.kez.state.event.annotations.StateEventHandler")
            appendLine()

            properties.forEach { property ->
                val functionName = getFunctionName(property)
                val propertyName = property.simpleName.asString()

                appendLine("/**")
                appendLine(" * Auto-generated consume function for state event: $propertyName")
                appendLine(" * This function is type-safe and obfuscation-proof.")
                appendLine(" * Usage: this.$functionName() in your StateEventHandler<$className> implementing ViewModel")
                appendLine(" */")
                appendLine("fun StateEventHandler<$className>.$functionName() {")
                appendLine("    updateUiState { copy($propertyName = null) }")
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

        // Compose helper functions 생성
        generateComposeHelpers(packageName, className, properties)

        logger.warn("StateEventProcessor: Generated extension functions and Compose helpers for $className")
    }

    private fun generateComposeHelpers(
        packageName: String,
        className: String,
        properties: List<KSPropertyDeclaration>
    ) {
        val composeCode = buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import androidx.compose.runtime.Composable")
            appendLine("import androidx.compose.runtime.LaunchedEffect")
            appendLine("import io.github.kez.state.event.annotations.StateEventHandler")
            appendLine()

            // 단일 통합 함수 생성
            appendLine("/**")
            appendLine(" * Auto-generated Compose helper for all state events in $className")
            appendLine(" * Automatically handles all events with appropriate consumption order based on event type.")
            appendLine(" * ")
            appendLine(" * Usage:")
            appendLine(" * ```")
            appendLine(" * HandleStateEvent(")
            appendLine(" *     uiState = viewModel.uiState.collectAsState().value,")
            appendLine(" *     stateEventHandler = viewModel,")

            // 각 property에 대한 사용법 예시 추가
            properties.forEach { property ->
                val propertyName = property.simpleName.asString()
                val propertyType = property.type.resolve().declaration.simpleName.asString()
                val isNullable = property.type.resolve().isMarkedNullable
                val baseType = if (isNullable) propertyType.removeSuffix("?") else propertyType

                appendLine(" *     on${propertyName.replaceFirstChar { it.uppercase() }} = { ${propertyName.lowercase()}: $baseType ->")
                appendLine(" *         // Your action here (e.g., show snackbar, navigate, etc.)")
                appendLine(" *     },")
            }
            appendLine(" * )")
            appendLine(" * ```")
            appendLine(" */")

            appendLine("@Composable")
            appendLine("fun HandleStateEvent(")
            appendLine("    uiState: $className,")
            appendLine("    stateEventHandler: StateEventHandler<$className>,")

            // 각 state event에 대한 파라미터 생성 (필수 파라미터로 변경)
            properties.forEachIndexed { index, property ->
                val propertyName = property.simpleName.asString()
                val propertyType = property.type.resolve().declaration.simpleName.asString()
                val isNullable = property.type.resolve().isMarkedNullable
                val baseType = if (isNullable) propertyType.removeSuffix("?") else propertyType

                val comma = if (index == properties.size - 1) "" else ","
                appendLine("    on${propertyName.replaceFirstChar { it.uppercase() }}: suspend ($baseType) -> Unit$comma")
            }

            appendLine(") {")

            // 각 state event에 대한 LaunchedEffect 생성
            properties.forEach { property ->
                val propertyName = property.simpleName.asString()
                val functionName = getFunctionName(property)
                val parameterName = "on${propertyName.replaceFirstChar { it.uppercase() }}"
                val eventType = getEventType(property)

                appendLine("    // Handle $propertyName state event (${eventType.name})")
                appendLine("    uiState.$propertyName?.let { value ->")
                appendLine("        LaunchedEffect(value) {")

                when (eventType) {
                    EventType.NAVIGATION -> {
                        // NAVIGATION: consume first, then action
                        appendLine("            stateEventHandler.$functionName()")
                        appendLine("            $parameterName(value)")
                    }

                    EventType.STANDARD -> {
                        // STANDARD: action first, then consume
                        appendLine("            $parameterName(value)")
                        appendLine("            stateEventHandler.$functionName()")
                    }
                }

                appendLine("        }")
                appendLine("    }")
                appendLine()
            }

            appendLine("}")
        }

        val composeFile = codeGenerator.createNewFile(
            dependencies = Dependencies(
                false,
                *properties.mapNotNull { it.containingFile }.toTypedArray()
            ),
            packageName = packageName,
            fileName = "${className}ComposeHelpers"
        )

        composeFile.write(composeCode.toByteArray())
        composeFile.close()
    }

    private fun getFunctionName(property: KSPropertyDeclaration): String {
        val propertyName = property.simpleName.asString()

        // @StateEvent 어노테이션에서 커스텀 함수명 가져오기
        val annotation = property.annotations.find {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == StateEvent::class.qualifiedName
        }
            ?: return "consume${propertyName.replaceFirstChar { it.uppercase() }}" // 어노테이션이 없으면 기본 함수명 반환

        val customFunctionName = annotation.arguments
            .find { it.name?.asString() == "consumeFunctionName" }
            ?.value as? String

        return if (customFunctionName.isNullOrEmpty()) {
            "consume${propertyName.replaceFirstChar { it.uppercase() }}"
        } else {
            customFunctionName
        }
    }

    private fun getEventType(property: KSPropertyDeclaration): EventType {
        try {
            // @StateEvent 어노테이션에서 eventType 가져오기
            val annotation = property.annotations.find {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == StateEvent::class.qualifiedName
            } ?: return EventType.STANDARD // 어노테이션이 없으면 기본값 반환


            // KSP에서 enum 값을 읽는 더 정확한 방법 - 소스 코드 분석 기반
            val eventTypeArgument = annotation.arguments.find { it.name?.asString() == "eventType" }

            if (eventTypeArgument == null) {
                return EventType.STANDARD
            }

            val value = eventTypeArgument.value ?: return EventType.STANDARD

            // KSP에서 enum은 KSClassDeclaration으로 처리됨
            return when (value) {
                is KSClassDeclaration -> {
                    val enumName = value.simpleName.asString()
                    when (enumName) {
                        "NAVIGATION" -> EventType.NAVIGATION
                        "STANDARD" -> EventType.STANDARD
                        else -> EventType.STANDARD
                    }
                }

                is KSType -> {
                    val enumName = value.declaration.simpleName.asString()
                    when (enumName) {
                        "NAVIGATION" -> EventType.NAVIGATION
                        "STANDARD" -> EventType.STANDARD
                        else -> EventType.STANDARD
                    }
                }

                else -> {
                    // 문자열로 변환 후 enum 이름 추출
                    val stringValue = value.toString()

                    // EventType.NAVIGATION -> NAVIGATION 추출
                    val enumName = if (stringValue.contains("EventType.")) {
                        stringValue.substringAfterLast(".")
                    } else if (stringValue.contains("NAVIGATION") || stringValue.contains("STANDARD")) {
                        if (stringValue.contains("NAVIGATION")) "NAVIGATION" else "STANDARD"
                    } else {
                        "STANDARD" // 기본값
                    }

                    when (enumName) {
                        "NAVIGATION" -> EventType.NAVIGATION
                        "STANDARD" -> EventType.STANDARD
                        else -> EventType.STANDARD
                    }
                }
            }

        } catch (e: Exception) {
            logger.error("Property ${property.simpleName.asString()}: Error reading eventType: ${e.message}")
            return EventType.STANDARD // 에러 시 기본값
        }
    }
}