package com.scott.app.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(): ViewModel() {

    private val _setupState = MutableStateFlow<SetupState>(SetupState.PermissionsNotChecked)
    val setupState = _setupState.asStateFlow()

    private val preRequisites = PreRequisites()

    fun permissionDenied() {
        setState(SetupState.PermissionsDenied)
    }

    fun permissionGranted() {
        preRequisites.permissionsGranted = true
        checkPreRequisites()
    }

    fun defaultAppGranted() {
        preRequisites.setAsDefaultApp = true
        checkPreRequisites()
    }

    fun defaultAppDenied() {
        setState(SetupState.DefaultAppDenied)
    }

    private fun checkPreRequisites() {
        val state = when {
            !preRequisites.permissionsGranted -> SetupState.PermissionsNotChecked
            !preRequisites.setAsDefaultApp -> SetupState.DefaultAppNotChecked
            else -> SetupState.Ready
        }
        setState(state)
    }

    private fun setState(state: SetupState) {
        _setupState.update { state }
    }

    sealed interface SetupState {
        data object PermissionsNotChecked: SetupState
        data object PermissionsDenied: SetupState
        data object DefaultAppNotChecked: SetupState
        data object DefaultAppDenied: SetupState
        data object Ready: SetupState
        data class Error(val throwable: Throwable): SetupState
    }

    private data class PreRequisites(
        var permissionsGranted: Boolean = false,
        var setAsDefaultApp: Boolean = false
    )
}