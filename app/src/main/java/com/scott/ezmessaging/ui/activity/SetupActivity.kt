package com.scott.ezmessaging.ui.activity

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.scott.ezmessaging.domain.RequestPermissionsUseCase
import com.scott.ezmessaging.manager.ContentManager
import com.scott.ezmessaging.manager.DeviceManager
import com.scott.ezmessaging.model.Initializable
import com.scott.ezmessaging.model.MessageData
import com.scott.ezmessaging.receiver.SmsSendCallbacks
import com.scott.ezmessaging.ui.theme.EZmessagingTheme
import com.scott.ezmessaging.viewmodel.SetupViewModel
import com.scott.ezmessaging.viewmodel.SetupViewModel.SetupState
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
    lateinit var deviceManager: DeviceManager

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
                            sendMessage()
                        }
                        is SetupState.Error -> {
                            Text(text = "Something bad happened!")
                        }
                    }
                }
            }
        }
    }

    private fun sendMessage() {
        deviceManager.initializedState.onEach {
            if (it is Initializable.Initialized) {
               /* contentManager.sendMmsMessage(
                    MessageData.Text("Hellooo"), arrayOf("3077605312"))*/
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
                    /*object : SmsSendCallbacks.SendSmsListener {
                    override fun onSent(isSuccess: Boolean) {
                        println("tesitngg sms sent: $isSuccess")
                    }

                    override fun onDelivered(isSuccess: Boolean) {
                        println("testingg sms delivered: $isSuccess")
                    }
                })*/
            }
        }.launchIn(lifecycleScope)
        deviceManager.initialize()
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