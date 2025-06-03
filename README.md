# StateEvent - Android Compose 상태 이벤트 자동화 라이브러리

StateEvent는 단일 이벤트를 상태로 관리하는 안드로이드 공식문서의 이벤트 관리 기법에 의거하여 KSP(Kotlin Symbol Processing)를 활용한 상태 이벤트 처리 자동화 라이브러리입니다.
ViewModel의 일회성 이벤트(one-time events)를 소비하는 함수를 만들고 UI에서 액션을 진행한 후 소비하는 반복적인 보일러플레이트 코드를 대폭 줄여줍니다.

### 참조 문서: 
- https://developer.android.com/topic/architecture/ui-layer/events#handle-viewmodel-events
- https://medium.com/androiddevelopers/viewmodel-one-off-event-antipatterns-16a1da869b95

## 🎯 목적

- **보일러플레이트 제거**: ViewModel 내부에서 State를 null로 해제하는 로직과 UI에서 LaunchedEffect와 상태 소비 함수의 반복적인 코드를 자동 생성
- **이벤트 소비 자동화**: `HandleStateEvent`를 활용하여 trigger된 이벤트를 consume된 상태로 되돌리는 로직을 자동화하여 이벤트 소비 누락을 방지
- **타입 안전성**: 컴파일 타임에 안전한 코드 생성으로 런타임 오류 방지
- **UDF 패턴 지원**: Unidirectional Data Flow 패턴을 유지하며 side effect 처리
- **네비게이션 이벤트 최적화**: 안드로이드 공식 가이드라인에 따른 올바른 이벤트 소비 순서 자동 적용

## 🚀 주요 기능

### 1. 자동 Consume 함수 생성
`@StateEvent` 어노테이션이 붙은 프로퍼티에 대해 타입 안전한 확장 함수를 자동 생성합니다.

### 2. 통합 Compose 헬퍼 생성
모든 state event를 한 번에 처리할 수 있는 `HandleStateEvent` 함수를 자동 생성합니다.

### 3. 이벤트 타입별 최적화
- **STANDARD 이벤트**: 액션 실행 → 이벤트 소비 (메시지, 토스트 등)
- **NAVIGATION 이벤트**: 이벤트 소비 → 액션 실행 (네비게이션)

### 4. StateEventHandler 인터페이스
리플렉션 없이 타입 안전한 상태 관리를 위한 인터페이스 기반 설계

### 5. 커스텀 함수명 지원
`consumeFunctionName` 파라미터로 생성될 함수명을 커스터마이징할 수 있습니다.

## 📦 프로젝트 구조

```
StateEventSample/
├── state-event-annotations/          # 어노테이션 정의
│   └── src/main/kotlin/
│       └── io/github/kez/state/event/annotations/
│           ├── StateEvent.kt         # State Event 어노테이션
│           ├── EventType.kt         # 이벤트 타입 enum
│           ├── StateEventHandler.kt # 타입 안전한 핸들러 인터페이스
│           └── UIState.kt           # UI State 마커 어노테이션
├── state-event-processor/           # KSP 프로세서
│   └── src/main/kotlin/
│       └── io/github/kez/state/event/processor/
│           └── StateEventProcessor.kt # 코드 생성 로직
└── app/                            # 샘플 앱
    └── src/main/java/.../sample/
        ├── SampleUiState.kt        # @StateEvent 사용 예시
        ├── SampleViewModel.kt      # StateEventHandler 구현
        └── MainActivity.kt         # UI에서 HandleStateEvent 사용
```

## 🛠️ 설정

### 1. 의존성 추가

**Root build.gradle.kts**
```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.1.21-2.0.1" apply false
}
```

**App module build.gradle.kts**
```kotlin
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(project(":state-event-annotations"))
    ksp(project(":state-event-processor"))
}
```

## 📝 사용법

### 1. UiState 정의

```kotlin
import io.github.kez.state.event.annotations.EventType
import io.github.kez.state.event.annotations.StateEvent
import io.github.kez.state.event.annotations.UIState

@UIState
data class SampleUiState(
    val isLoading: Boolean = false,
    val data: List<String> = emptyList(),
    
    // STANDARD 이벤트: 액션 → 소비 순서
    @StateEvent(eventType = EventType.STANDARD)
    val showSuccessMessage: String? = null,
    
    // 커스텀 함수명 + STANDARD 이벤트
    @StateEvent(consumeFunctionName = "clearError", eventType = EventType.STANDARD)
    val errorMessage: String? = null,
    
    // NAVIGATION 이벤트: 소비 → 액션 순서 (안드로이드 가이드라인 준수)
    @StateEvent(eventType = EventType.NAVIGATION)
    val navigateToDetail: String? = null
)
```

### 2. ViewModel 구현 - StateEventHandler 인터페이스 사용

```kotlin
import io.github.kez.state.event.annotations.StateEventHandler

class SampleViewModel : ViewModel(), StateEventHandler<SampleUiState> {
    
    private val _uiState = MutableStateFlow(SampleUiState())
    val uiState: StateFlow<SampleUiState> = _uiState.asStateFlow()
    
    // StateEventHandler 인터페이스 구현
    override fun updateUiState(update: (SampleUiState) -> SampleUiState) {
        _uiState.update(update)
    }
    
    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // 네트워크 호출 시뮬레이션
                delay(2000)
                val data = listOf("Item 1", "Item 2", "Item 3")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        data = data,
                        showSuccessMessage = "데이터를 성공적으로 로드했습니다!"
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "데이터 로드 실패: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun onItemClick(item: String) {
        _uiState.update {
            it.copy(navigateToDetail = item)
        }
    }
}
```

### 3. Compose UI에서 사용

```kotlin
@Composable
fun SampleScreen(
    viewModel: SampleViewModel = viewModel(),
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 🔥 자동 생성된 HandleStateEvent 함수 사용
    // 모든 이벤트가 자동으로 처리되고 올바른 순서로 소비됩니다
    HandleStateEvent(
        uiState = uiState,
        stateEventHandler = viewModel,
        onShowSuccessMessage = { message ->
            // STANDARD: 액션 먼저 → 소비
            snackbarHostState.showSnackbar(message)
        },
        onErrorMessage = { message ->
            // STANDARD: 액션 먼저 → 소비  
            snackbarHostState.showSnackbar("오류: $message")
        },
        onNavigateToDetail = { item ->
            // NAVIGATION: 소비 먼저 → 액션 (안드로이드 가이드라인 준수)
            onNavigateToDetail(item)
        }
    )
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(uiState.data) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onItemClick(item) }
                    ) {
                        Text(
                            text = item,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
```

## 🔧 자동 생성되는 코드

### 1. StateEventHandler 확장 함수들

```kotlin
// SampleUiStateStateEventExtensions.kt (자동 생성)

/**
 * Auto-generated consume function for state event: showSuccessMessage
 * This function is type-safe and obfuscation-proof.
 * Usage: this.consumeShowSuccessMessage() in your StateEventHandler<SampleUiState> implementing ViewModel
 */
fun StateEventHandler<SampleUiState>.consumeShowSuccessMessage() {
    updateUiState { copy(showSuccessMessage = null) }
}

/**
 * Auto-generated consume function for state event: errorMessage
 * This function is type-safe and obfuscation-proof.
 * Usage: this.clearError() in your StateEventHandler<SampleUiState> implementing ViewModel
 */
fun StateEventHandler<SampleUiState>.clearError() {
    updateUiState { copy(errorMessage = null) }
}

/**
 * Auto-generated consume function for state event: navigateToDetail
 * This function is type-safe and obfuscation-proof.
 * Usage: this.consumeNavigateToDetail() in your StateEventHandler<SampleUiState> implementing ViewModel
 */
fun StateEventHandler<SampleUiState>.consumeNavigateToDetail() {
    updateUiState { copy(navigateToDetail = null) }
}
```

### 2. Compose 헬퍼 함수

```kotlin
// SampleUiStateComposeHelpers.kt (자동 생성)

@Composable
fun HandleStateEvent(
    uiState: SampleUiState,
    stateEventHandler: StateEventHandler<SampleUiState>,
    onShowSuccessMessage: suspend (String) -> Unit,
    onErrorMessage: suspend (String) -> Unit,
    onNavigateToDetail: suspend (String) -> Unit
) {
    // Handle showSuccessMessage state event (STANDARD)
    uiState.showSuccessMessage?.let { value ->
        LaunchedEffect(value) {
            onShowSuccessMessage(value)           // 액션 먼저
            stateEventHandler.consumeShowSuccessMessage()  // 그 다음 소비
        }
    }

    // Handle errorMessage state event (STANDARD)
    uiState.errorMessage?.let { value ->
        LaunchedEffect(value) {
            onErrorMessage(value)                 // 액션 먼저
            stateEventHandler.clearError()        // 그 다음 소비
        }
    }

    // Handle navigateToDetail state event (NAVIGATION)
    uiState.navigateToDetail?.let { value ->
        LaunchedEffect(value) {
            stateEventHandler.consumeNavigateToDetail()  // 소비 먼저
            onNavigateToDetail(value)                    // 그 다음 액션
        }
    }
}
```

## 📋 어노테이션 참조

### @StateEvent

일회성 이벤트 프로퍼티에 붙이는 어노테이션입니다.

```kotlin
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class StateEvent(
    val consumeFunctionName: String = "",
    val eventType: EventType = EventType.STANDARD
)
```

**매개변수:**
- `consumeFunctionName`: 생성될 consume 함수의 이름. 기본값은 "consume{PropertyName}"
- `eventType`: 이벤트 타입. `EventType.STANDARD` 또는 `EventType.NAVIGATION`

### EventType

이벤트 처리 순서를 결정하는 enum입니다.

```kotlin
enum class EventType {
    STANDARD,    // 액션 → 소비 (메시지, 토스트 등)
    NAVIGATION   // 소비 → 액션 (네비게이션)
}
```

### StateEventHandler

타입 안전한 상태 업데이트를 위한 인터페이스입니다.

```kotlin
interface StateEventHandler<T> {
    fun updateUiState(update: (T) -> T)
}
```

### @UIState

UiState data class에 붙이는 마커 어노테이션입니다.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class UIState
```

## ⚡ 라이브러리의 편의성과 장점

### 🔥 Before vs After 비교

#### ❌ Before (수동 구현) - 45줄의 반복적인 코드

```kotlin
@Composable
fun SampleScreen(viewModel: SampleViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 성공 메시지 처리 - 수동으로 매번 작성해야 함
    uiState.showSuccessMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeShowSuccessMessage()  // 소비 함수도 수동 구현 필요
        }
    }

    // 에러 메시지 처리 - 수동으로 매번 작성해야 함  
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar("Error: $error")
            viewModel.clearError()  // 수동 구현 필요
        }
    }

    // 네비게이션 처리 - 수동으로 매번 작성해야 함
    uiState.navigateToDetail?.let { item ->
        LaunchedEffect(item) {
            // ⚠️ 잘못된 순서: 네비게이션은 소비를 먼저 해야 함!
            onNavigateToDetail(item)
            viewModel.consumeNavigateToDetail()  // 수동 구현 필요
        }
    }
    
    // UI 코드...
}

// ViewModel에서도 각 consume 함수를 수동으로 구현해야 함
class SampleViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SampleUiState())
    val uiState: StateFlow<SampleUiState> = _uiState.asStateFlow()
    
    // 각 consume 함수를 수동으로 작성해야 함
    fun consumeShowSuccessMessage() {
        _uiState.update { it.copy(showSuccessMessage = null) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }  
    }
    
    fun consumeNavigateToDetail() {
        _uiState.update { it.copy(navigateToDetail = null) }
    }
    
    // 비즈니스 로직...
}
```

#### ✅ After (StateEvent 라이브러리) - 8줄의 간결한 코드

```kotlin
@Composable
fun SampleScreen(viewModel: SampleViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 🚀 모든 이벤트가 자동으로 처리됨!
    HandleStateEvent(
        uiState = uiState,
        stateEventHandler = viewModel,
        onShowSuccessMessage = { snackbarHostState.showSnackbar(it) },
        onErrorMessage = { snackbarHostState.showSnackbar("Error: $it") },
        onNavigateToDetail = { onNavigateToDetail(it) }  // ✅ 올바른 순서 자동 적용
    )
    
    // UI 코드...
}

// ViewModel - StateEventHandler만 구현하면 끝!
class SampleViewModel : ViewModel(), StateEventHandler<SampleUiState> {
    private val _uiState = MutableStateFlow(SampleUiState())
    val uiState: StateFlow<SampleUiState> = _uiState.asStateFlow()
    
    // 🔥 단 1줄만 구현하면 모든 consume 함수가 자동 생성됨!
    override fun updateUiState(update: (SampleUiState) -> SampleUiState) {
        _uiState.update(update)
    }
    
    // 비즈니스 로직에만 집중!
}
```

### 📊 성능 및 개발 효율성 향상

| 항목 | Before (수동) | After (StateEvent) | 개선율 |
|-----|--------------|-------------------|-------|
| **코드 라인 수** | 45줄 | 8줄 | **87% 감소** |
| **반복 코드** | 매 이벤트마다 LaunchedEffect + consume 패턴 반복 | 자동 생성 | **100% 제거** |
| **타입 안전성** | 런타임 오류 가능 | 컴파일 타임 검증 | **완전 안전** |
| **이벤트 소비 누락** | 수동 관리로 누락 위험 | 자동 처리 | **0% 위험** |
| **네비게이션 순서** | 개발자가 직접 관리 | 자동으로 올바른 순서 | **가이드라인 준수** |

### 🎯 핵심 편의성

#### 1. **보일러플레이트 완전 제거**
- 매번 작성해야 하는 `LaunchedEffect` + `?.let` + `consume` 패턴을 자동 생성
- ViewModel의 consume 함수들을 자동 생성

#### 2. **안드로이드 가이드라인 자동 준수**
- STANDARD 이벤트: 액션 → 소비 (토스트, 스낵바 등)
- NAVIGATION 이벤트: 소비 → 액션 (네비게이션)
- 개발자가 순서를 외우거나 실수할 필요 없음

#### 3. **타입 안전성 보장**
- 컴파일 타임에 모든 타입 검증
- 리플렉션 없는 안전한 코드 생성
- IDE의 자동완성과 타입 체크 지원

#### 4. **개발자 경험 향상**
- 비즈니스 로직에만 집중 가능
- 이벤트 처리 코드의 일관성 보장
- 실수로 인한 버그 제거

#### 5. **유지보수성 향상**
- 표준화된 이벤트 처리 패턴
- 새로운 이벤트 추가 시 어노테이션만 추가하면 완료
- 코드 리뷰 시 이벤트 처리 로직을 확인할 필요 없음

## 🔍 고급 기능

### 1. 다양한 이벤트 타입 지원

```kotlin
@UIState
data class MyUiState(
    // 토스트 메시지 - STANDARD
    @StateEvent(eventType = EventType.STANDARD)
    val showToast: String? = null,
    
    // 다이얼로그 - STANDARD  
    @StateEvent(eventType = EventType.STANDARD)
    val showDialog: String? = null,
    
    // 네비게이션 - NAVIGATION (소비 먼저)
    @StateEvent(eventType = EventType.NAVIGATION)
    val navigateToSettings: Boolean? = null,
    
    // 백 네비게이션 - NAVIGATION (소비 먼저)
    @StateEvent(eventType = EventType.NAVIGATION) 
    val navigateBack: Boolean? = null,
    
    // 커스텀 함수명
    @StateEvent(consumeFunctionName = "dismissError", eventType = EventType.STANDARD)
    val errorMessage: String? = null
)
```

### 2. 복잡한 이벤트 데이터 지원

```kotlin
// 복잡한 데이터 타입도 지원
@StateEvent(eventType = EventType.NAVIGATION)
val navigateToDetailScreen: DetailScreenData? = null

data class DetailScreenData(
    val itemId: String,
    val title: String,
    val metadata: Map<String, String>
)
```

### 3. 오류 방지 메커니즘

```kotlin
// 컴파일 타임에 다음 오류들을 방지:
// ❌ 존재하지 않는 프로퍼티 참조
// ❌ 타입 불일치  
// ❌ 누락된 이벤트 처리
// ❌ 잘못된 소비 순서
```

## 🐛 문제 해결

### 빌드 오류가 발생하는 경우
1. KSP 버전이 Kotlin 버전과 호환되는지 확인
2. `./gradlew clean build` 후 다시 빌드 시도
3. Generated 폴더가 IDE에서 인식되는지 확인

### 함수가 생성되지 않는 경우
1. `@StateEvent` 어노테이션이 올바르게 적용되었는지 확인
2. data class에 `@UIState` 어노테이션이 있는지 확인
3. ViewModel이 `StateEventHandler<YourUiState>`를 구현했는지 확인

### 이벤트가 처리되지 않는 경우
1. `HandleStateEvent` 함수에 모든 이벤트 핸들러가 제공되었는지 확인
2. `stateEventHandler` 파라미터에 올바른 ViewModel 인스턴스가 전달되었는지 확인

## 📄 라이센스
https://github.com/kez-lab/StateEventSample/blob/main/LICENSE