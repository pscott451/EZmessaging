package com.scott.app.ui.activity

import com.scott.app.R
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.scott.app.domain.RequestPermissionsUseCase
import com.scott.ezmessaging.manager.ContentManager
import com.scott.ezmessaging.model.Initializable
import com.scott.app.ui.theme.EZmessagingTheme
import com.scott.app.viewmodel.SetupViewModel
import com.scott.app.viewmodel.SetupViewModel.SetupState
import com.scott.ezmessaging.model.MessageData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class SetupActivity: ComponentActivity() {

    private val setupViewModel by viewModels<SetupViewModel>()

    @Inject
    lateinit var requestPermissionsUseCase: RequestPermissionsUseCase

    @Inject
    lateinit var contentManager: ContentManager

    private val openPermissionSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        checkPermissions()
    }

    private val openDefaultAppSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (isDefaultMessagingApp()) {
            setupViewModel.defaultAppGranted()
        } else {
            setupViewModel.defaultAppDenied()
        }
    }

    private val defaultAppLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            setupViewModel.defaultAppGranted()
        } else {
            setupViewModel.defaultAppDenied()
        }
    }

    @SuppressLint("MissingPermission") // buildContent is only called once permission has been granted.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EZmessagingTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    val initState by setupViewModel.setupState.collectAsState()

                    when (initState) {
                        SetupState.PermissionsNotChecked -> checkPermissions()
                        SetupState.PermissionsDenied -> {
                            Text(text = "Uh oh, you need to open the app settings and allow all permissions")
                            Button(onClick = { openPermissionSettings() }) {
                                Text(text = "Open Settings")
                            }
                        }
                        SetupState.DefaultAppNotChecked -> {
                            if (isDefaultMessagingApp()) setupViewModel.defaultAppGranted() else requestAsDefaultMessagingApp()
                        }
                        SetupState.DefaultAppDenied -> {
                            Text(text = "Uh oh, you need to set as default app")
                            Button(onClick = { openDefaultAppSettings() }) {
                                Text(text = "Set as Default")
                            }
                        }
                        SetupState.Ready -> {
                            initDeviceManager()
                        }
                        is SetupState.Error -> {
                            Text(text = "Something bad happened!")
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initDeviceManager() {
        contentManager.initializedState.onEach {
            if (it is Initializable.Initialized) {
                sendMmsMessage()
            }
        }.launchIn(lifecycleScope)
        contentManager.initialize()
    }

    private fun sendSmsMessage() {
        contentManager.sendSmsMessage(
            address = "3077605312",
            text = "11 sms message",
            onSent = { isSuccess ->
                println("testingg sms sent 1: $isSuccess")
            },
            onDelivered = { isSuccess ->
                println("testingg sms delivered 1: $isSuccess")
            }
        )
    }

    private fun sendMmsMessage() {
        val jpeg = BitmapFactory.decodeResource(resources, R.drawable.android)
        lifecycleScope.launch {
            contentManager.sendMmsMessage(
                message = MessageData.Image(
                    bitmap = jpeg,
                    mimeType = ContentManager.SupportedMessageTypes.CONTENT_TYPE_JPEG
                ),
                arrayOf("3077605312")
            ) {
                println("testingg on mms sent: $it")
            }
            //contentManager.getAllMessages()
            println("testingg done")
        }
    }

    private fun isDefaultMessagingApp() = Telephony.Sms.getDefaultSmsPackage(this) == packageName

    private fun requestAsDefaultMessagingApp() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            val roleManager = getSystemService(RoleManager::class.java)
            val roleRequestIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
            defaultAppLauncher.launch(roleRequestIntent)
        } else {
            val setSmsAppIntent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            setSmsAppIntent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            defaultAppLauncher.launch(setSmsAppIntent)
        }
    }

    private fun checkPermissions() {
        requestPermissionsUseCase { permissionGranted ->
            if (permissionGranted) {
                setupViewModel.permissionGranted()
            } else {
                setupViewModel.permissionDenied()
            }
        }
    }

    private fun openPermissionSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        openPermissionSettingsLauncher.launch(intent)
    }

    private fun openDefaultAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        openDefaultAppSettingsLauncher.launch(intent)
    }
}