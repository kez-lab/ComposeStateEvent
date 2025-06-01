# StateEvent - Android Compose 상태 이벤트 자동화 라이브러리

StateEvent는 단일 이벤트를 상태로 관리하는 안드로이드 공식문서의 이벤트 관리 기법에 의거하여 KSP(Kotlin Symbol Processing)를 활용한 상태 이벤트 처리 자동화 라이브러리입니다.
ViewModel의 일회성 이벤트(one-time events)를 소비하는 함수를 만들고 UI에서 액션을 진행한 후 소비하는 반복적인 보일러플레이트 코드를 대폭 줄여줍니다.

### 참조 문서: 
- https://developer.android.com/topic/architecture/ui-layer/events#handle-viewmodel-events
- https://medium.com/androiddevelopers/viewmodel-one-off-event-antipatterns-16a1da869b95


## 🎯 목적

- **보일러플레이트 제거**: ViewModel 내부에서 State를 null로 해제하는 로직과 UI에서 LaunchedEffect와 상태 소비 함수의 반복적인 코드를 자동 생성
- **이벤트 소비 자동화**: HandleStateEvent 를 활용하여 trigger 된 이벤트를 consume 된 상태로 되돌리는 로직을 자동화하여 보일러 플레이트 코드를 제거
- **타입 안전성**: 컴파일 타임에 안전한 코드 생성으로 런타임 오류 방지
- **UDF 패턴 지원**: Unidirectional Data Flow 패턴을 유지하며 side effect 처리

## 🚀 주요 기능

### 1. 자동 Consume 함수 생성
`@StateEvent` 어노테이션이 붙은 프로퍼티에 대해 ViewModel 확장 함수를 자동 생성합니다.

### 2. 통합 Compose 헬퍼 생성
모든 state event를 한 번에 처리할 수 있는 `HandleStateEvent` 함수를 자동 생성합니다.

### 3. 커스텀 함수명 지원
`consumeFunctionName` 파라미터로 생성될 함수명을 커스터마이징할 수 있습니다.

## 📦 프로젝트 구조

```
StateEventSample/
├── state-event-annotations/          # 어노테이션 정의
│   └── src/main/kotlin/
│       └── io/github/kez/state/event/annotations/
│           ├── StateEvent.kt         # State Event 어노테이션
│           └── UIState.kt           # UI State 마커 어노테이션
├── state-event-processor/           # KSP 프로세서
│   └── src/main/kotlin/
│       └── io/github/kez/state/event/processor/
│           └── StateEventProcessor.kt # 코드 생성 로직
└── app/                            # 샘플 앱
    └── src/main/java/.../sample/
        ├── SampleUiState.kt        # @StateEvent 사용 예시
        ├── SampleViewModel.kt      # ViewModel 구현
        └── MainActivity.kt         # UI에서 HandleStateEvent 사용
```

## 🛠️ 설정

### 1. 의존성 추가

**Root build.gradle.kts**
```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
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
import io.github.kez.state.event.annotations.StateEvent
import io.github.kez.state.event.annotations.UIState

@UIState
data class SampleUiState(
    val isLoading: Boolean = false,
    val data: List<String> = emptyList(),
    
    // 기본 consume 함수명: consumeShowSuccessMessage
    @StateEvent
    val showSuccessMessage: String? = null,
    
    // 커스텀 consume 함수명: clearError
    @StateEvent(consumeFunctionName = "clearError")
    val errorMessage: String? = null,
    
    // 기본 consume 함수명: consumeNavigateToDetail
    @StateEvent
    val navigateToDetail: String? = null
)
```

### 2. ViewModel 구현

```kotlin
class SampleViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(SampleUiState())
    val uiState: StateFlow<SampleUiState> = _uiState.asStateFlow()
    
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
    viewModel: SampleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 🔥 자동 생성된 HandleStateEvent 함수 사용
    HandleStateEvent(
        uiState = uiState,
        viewModel = viewModel,
        onShowSuccessMessage = { message ->
            snackbarHostState.showSnackbar(message)
        },
        onErrorMessage = { message ->
            snackbarHostState.showSnackbar("오류: $message")
        },
        onNavigateToDetail = { item ->
            snackbarHostState.showSnackbar("상세화면으로 이동: $item")
        }
    )
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        // UI 컨텐츠
        // ...
    }
}
```

## 🔧 자동 생성되는 코드

### 1. ViewModel 확장 함수들

```kotlin
// SampleUiStateStateEventExtensions.kt (자동 생성)

fun ViewModel.consumeShowSuccessMessage() {
    // MutableStateFlow<SampleUiState> 필드를 찾아서 상태 업데이트
    val uiStateField = this::class.java.declaredFields.find { field ->
        // ... 필드 검색 로직
    }
    uiState?.update { it.copy(showSuccessMessage = null) }
}

fun ViewModel.clearError() {
    // 커스텀 함수명으로 생성
    uiState?.update { it.copy(errorMessage = null) }
}

fun ViewModel.consumeNavigateToDetail() {
    uiState?.update { it.copy(navigateToDetail = null) }
}
```

### 2. Compose 헬퍼 함수

```kotlin
// SampleUiStateComposeHelpers.kt (자동 생성)

@Composable
fun HandleStateEvent(
    uiState: SampleUiState,
    viewModel: ViewModel,
    onShowSuccessMessage: suspend (String) -> Unit,
    onErrorMessage: suspend (String) -> Unit,
    onNavigateToDetail: suspend (String) -> Unit
) {
    // showSuccessMessage 처리
    uiState.showSuccessMessage?.let { value ->
        LaunchedEffect(value) {
            onShowSuccessMessage(value)
            viewModel.consumeShowSuccessMessage()
        }
    }

    // errorMessage 처리
    uiState.errorMessage?.let { value ->
        LaunchedEffect(value) {
            onErrorMessage(value)
            viewModel.clearError()
        }
    }

    // navigateToDetail 처리
    uiState.navigateToDetail?.let { value ->
        LaunchedEffect(value) {
            onNavigateToDetail(value)
            viewModel.consumeNavigateToDetail()
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
    val consumeFunctionName: String = ""
)
```

**매개변수:**
- `consumeFunctionName`: 생성될 consume 함수의 이름. 기본값은 "consume{PropertyName}"

### @UIState

UiState data class에 붙이는 마커 어노테이션입니다.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class UIState
```

**목적:**
- ViewModel에서 여러 StateFlow가 있을 때 올바른 UiState StateFlow를 식별
- 타입 기반 필드 검색을 통한 안전한 코드 생성

## ⚡ 성능상 이점

### Before (수동 구현)
```kotlin
// 각 state event마다 15줄의 반복 코드
uiState.showSuccessMessage?.let { message ->
    LaunchedEffect(message) {
        snackbarHostState.showSnackbar(message)
        viewModel.consumeShowSuccessMessage()
    }
}

uiState.errorMessage?.let { error ->
    LaunchedEffect(error) {
        snackbarHostState.showSnackbar("Error: $error")
        viewModel.clearError()
    }
}

uiState.navigateToDetail?.let { item ->
    LaunchedEffect(item) {
        snackbarHostState.showSnackbar("Navigating: $item")
        viewModel.consumeNavigateToDetail()
    }
}
```

### After (자동 생성)
```kotlin
// 단 8줄로 모든 state event 처리
HandleStateEvent(
    uiState = uiState,
    viewModel = viewModel,
    onShowSuccessMessage = { message -> snackbarHostState.showSnackbar(message) },
    onErrorMessage = { error -> snackbarHostState.showSnackbar("Error: $error") },
    onNavigateToDetail = { item -> snackbarHostState.showSnackbar("Navigating: $item") }
)
```

**결과**: 코드량 **87% 감소** (45줄 → 8줄)

## 🔍 고급 기능

### 1. 타입 안전성
- 컴파일 타임에 모든 타입이 검증됨
- 존재하지 않는 프로퍼티나 잘못된 타입 사용 시 빌드 오류

### 2. 리플렉션 기반 필드 검색
- Java 리플렉션과 Kotlin 리플렉션을 조합하여 안전한 필드 접근
- `@UIState` 어노테이션을 통한 정확한 StateFlow 식별

### 3. 유연한 함수명 지정
```kotlin
@StateEvent(consumeFunctionName = "dismissDialog")
val showDialog: Boolean? = null

@StateEvent(consumeFunctionName = "resetNavigation") 
val navigationEvent: NavEvent? = null
```

## 🐛 문제 해결

### 빌드 오류가 발생하는 경우
1. KSP 버전이 Kotlin 버전과 호환되는지 확인
2. `clean` 후 다시 빌드 시도
3. Generated 폴더가 IDE에서 인식되는지 확인

### 함수가 생성되지 않는 경우
1. `@StateEvent` 어노테이션이 올바르게 적용되었는지 확인
2. data class에 `@UIState` 어노테이션이 있는지 확인
3. ViewModel에 `MutableStateFlow<YourUiState>` 필드가 있는지 확인

## 📄 라이센스
https://github.com/kez-lab/StateEventSample/blob/main/LICENSE
