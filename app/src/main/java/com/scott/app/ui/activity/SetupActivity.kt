package com.scott.app.ui.activity

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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.scott.app.R
import com.scott.app.domain.RequestPermissionsUseCase
import com.scott.app.ui.theme.TestAppTheme
import com.scott.app.viewmodel.SetupViewModel
import com.scott.app.viewmodel.SetupViewModel.SetupState
import com.scott.ezmessaging.manager.ContentManager
import com.scott.ezmessaging.model.Initializable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
            TestAppTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    val initState by setupViewModel.setupState.collectAsState()

                    when (initState) {
                        SetupState.PermissionsNotChecked -> checkPermissions()
                        SetupState.PermissionsDenied -> {
                            Text(text = stringResource(id = R.string.setupactivity_allowpermissions))
                            Button(onClick = { openPermissionSettings() }) {
                                Text(text = stringResource(id = R.string.setupactivity_opensettings))
                            }
                        }
                        SetupState.DefaultAppNotChecked -> {
                            if (isDefaultMessagingApp()) setupViewModel.defaultAppGranted() else requestAsDefaultMessagingApp()
                        }
                        SetupState.DefaultAppDenied -> {
                            Text(text = stringResource(id = R.string.setupactivity_setasdefaultapp))
                            Button(onClick = { openDefaultAppSettings() }) {
                                Text(text = stringResource(id = R.string.setupactivity_setasdefault))
                            }
                        }
                        SetupState.Ready -> {
                            initContentManager()
                        }
                        is SetupState.Error -> {
                            Text(text = stringResource(id = R.string.setupactivity_error))
                        }
                    }
                }
            }
        }
    }

    private fun initContentManager() {
        contentManager.initializedState.onEach {
            if (it is Initializable.Initialized) {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }.launchIn(lifecycleScope)
        contentManager.initialize()
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