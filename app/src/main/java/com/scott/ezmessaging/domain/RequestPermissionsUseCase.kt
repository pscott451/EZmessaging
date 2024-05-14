package com.scott.ezmessaging.domain

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject

/**
 * A use case responsible for checking if the user has granted the required permissions.
 * Should only be used from a [ComponentActivity].
 *
 * When invoked, returns a lambda indicating the permission status.
 */
class RequestPermissionsUseCase @Inject constructor(
    @ActivityContext context: Context
) {

    private val requestPermissionsLauncher =
        (context as? ComponentActivity)?.let {
            it.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { grantedMap ->
                permissionIsGranted?.invoke(!grantedMap.values.contains(false))
            }
        } ?: run {
            null
        }

    private var permissionIsGranted: ((Boolean) -> Unit)? = null

    /**
     * Contains a list of all required permissions needed for the app to work.
     * Versions < Tiramisu don't require the Read Media permissions.
     */
    private val allRequiredPermissionsPreTiramisu = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_MMS,
        Manifest.permission.RECEIVE_WAP_PUSH,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE
    )

    /**
     * Contains a list of all required permissions needed for the app to work.
     * Versions >= Tiramisu also require the Read Media permissions.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val allRequiredPermissionsTiramisu = allRequiredPermissionsPreTiramisu + arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_VIDEO
    )

    /**
     * Requests the provided [permissions].
     * @param permissions If null, will request all permissions needed for the app.
     */
    operator fun invoke(permissions: Array<String>? = null, permissionIsGranted: (Boolean) -> Unit) {
        this.permissionIsGranted = permissionIsGranted

        // If the user refuses the permissions twice (or they select the box to 'Don't ask again'), the dialog will fail to display when requested and the 'grantedMap`
        // will always return with false values.
        val permissionsToRequest = when {
            permissions != null -> permissions
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> allRequiredPermissionsPreTiramisu
            else -> allRequiredPermissionsTiramisu
        }
        requestPermissionsLauncher?.launch(permissionsToRequest)
    }
}