package com.landmusic.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.landmusic.app.ui.AppRoot
import com.landmusic.app.ui.LibraryViewModel
import com.landmusic.app.ui.theme.LandMusicTheme

class MainActivity : ComponentActivity() {

    private var hasAllFilesAccess by mutableStateOf(false)
    private var libraryViewModel: LibraryViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        libraryViewModel = ViewModelProvider(this)[LibraryViewModel::class.java]

        setContent {
            LandMusicTheme {
                val audioPermission = if (Build.VERSION.SDK_INT >= 33)
                    Manifest.permission.READ_MEDIA_AUDIO
                else
                    Manifest.permission.READ_EXTERNAL_STORAGE

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { grants ->
                    if (grants[audioPermission] == true) {
                        libraryViewModel?.refresh()
                    }
                }

                AppRoot(
                    hasAllFilesAccess = hasAllFilesAccess,
                    onGrantAllFiles = { requestAllFilesAccess() },
                )

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity, audioPermission
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        libraryViewModel?.refresh()
                    } else {
                        val perms = buildList {
                            add(audioPermission)
                            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        launcher.launch(perms.toTypedArray())
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val was = hasAllFilesAccess
        hasAllFilesAccess = if (Build.VERSION.SDK_INT >= 30) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
        if (hasAllFilesAccess && !was) {
            libraryViewModel?.refresh()
        }
    }

    private fun requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }.onFailure {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }
}
