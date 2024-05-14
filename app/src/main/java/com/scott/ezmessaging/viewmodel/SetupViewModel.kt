package com.scott.ezmessaging.viewmodel

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

    fun permissionDenied() {
        setState(SetupState.PermissionsDenied)
    }

    fun permissionGranted() {
        // All permissions were granted. Need to check if default messaging app
        setState(SetupState.DefaultAppNotChecked)
    }

    fun defaultAppGranted() {
        // Set as default. Permissions should have already been granted as well.
        setState(SetupState.Ready)
    }

    fun defaultAppDenied() {
        setState(SetupState.DefaultAppDenied)
    }

    private fun setState(state: SetupState) {
        _setupState.update { state }
    }

    sealed interface SetupState {
        object PermissionsNotChecked: SetupState
        object PermissionsDenied: SetupState
        object DefaultAppNotChecked: SetupState
        object DefaultAppDenied: SetupState
        object Ready: SetupState
        data class Error(val throwable: Throwable): SetupState
    }
}