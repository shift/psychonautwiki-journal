package com.isaakhanimann.journal.ui.viewmodel

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Simple ViewModel pattern for Compose Multiplatform
abstract class BaseViewModel {
    protected val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    open fun onCleared() {
        viewModelScope.cancel()
    }
}

// UI State management pattern
data class UiState<T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isSuccess: Boolean get() = data != null && error == null
    val isEmpty: Boolean get() = data == null && !isLoading && error == null
}

// Composable function to remember ViewModels with Koin
@Composable
inline fun <reified T : BaseViewModel> rememberViewModel(): T {
    return remember { org.koin.core.context.GlobalContext.get().get<T>() }
}

// Extension to collect Flow as State in Compose
@Composable
fun <T> Flow<T>.collectAsStateWithLifecycle(
    initialValue: T,
    lifecycle: androidx.compose.runtime.State<Boolean> = remember { mutableStateOf(true) }
): State<T> {
    val state = remember(this) { mutableStateOf(initialValue) }
    
    LaunchedEffect(this, lifecycle.value) {
        if (lifecycle.value) {
            this@collectAsStateWithLifecycle.collect { state.value = it }
        }
    }
    
    return state
}

// StateFlow collector for Compose
@Composable
fun <T> StateFlow<T>.collectAsState(): State<T> = collectAsState(context = Dispatchers.Main.immediate)