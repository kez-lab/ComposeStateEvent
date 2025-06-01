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
                processStateEventClass(containingClass, properties)
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
    
    private fun processStateEventClass(
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
        
        logger.warn("StateEventProcessor: Generating extensions for $className with ${properties.size} properties")
        
        // 모든 함수들을 한 파일에 생성
        val functions = properties.map { property ->
            generateConsumeFunction(property)
        }
        
        val code = buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import androidx.lifecycle.ViewModel")
            appendLine("import kotlinx.coroutines.flow.MutableStateFlow")
            appendLine("import kotlinx.coroutines.flow.update")
            appendLine()
            
            functions.forEach { functionCode ->
                appendLine(functionCode)
                appendLine()
            }
        }
        
        // 파일 생성
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false, *properties.mapNotNull { it.containingFile }.toTypedArray()),
            packageName = packageName,
            fileName = "${className}Extensions"
        )
        
        file.write(code.toByteArray())
        file.close()
        
        logger.warn("StateEventProcessor: Generated file ${className}Extensions.kt")
    }
    
    private fun generateConsumeFunction(property: KSPropertyDeclaration): String {
        val propertyName = property.simpleName.asString()
        val uiStateClass = getContainingClass(property)!!
        val className = uiStateClass.simpleName.asString()
        
        // @StateEvent 어노테이션에서 커스텀 함수명 가져오기
        val annotation = property.annotations.first { 
            it.annotationType.resolve().declaration.qualifiedName?.asString() == StateEvent::class.qualifiedName 
        }
        
        val customFunctionName = annotation.arguments
            .find { it.name?.asString() == "consumeFunctionName" }
            ?.value as? String
        
        val functionName = if (customFunctionName.isNullOrEmpty()) {
            "consume${propertyName.replaceFirstChar { it.uppercase() }}"
        } else {
            customFunctionName
        }
        
        return "fun ViewModel.$functionName(_uiState: MutableStateFlow<$className>) {\n" +
               "    _uiState.update { it.copy($propertyName = null) }\n" +
               "}"
    }
}