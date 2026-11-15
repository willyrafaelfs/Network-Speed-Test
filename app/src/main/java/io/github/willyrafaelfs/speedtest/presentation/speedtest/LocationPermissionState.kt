package io.github.willyrafaelfs.speedtest.presentation.speedtest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.willyrafaelfs.speedtest.presentation.component.LocationAccess

/**
 * State izin lokasi untuk membaca nama Wi-Fi.
 *
 * Alurnya: user menekan tombol → dialog penjelasan ([isRationaleVisible]) → dialog izin
 * sistem. Bila user menolak dan sistem tidak lagi mau menampilkan dialognya ("Don't ask
 * again"), status menjadi [LocationAccess.BLOCKED] dan tombol mengarah ke Setelan aplikasi.
 */
@Stable
class LocationPermissionState(
    val access: LocationAccess,
    val isRationaleVisible: Boolean,
    val onRequest: () -> Unit,
    val onConfirmRationale: () -> Unit,
    val onDismissRationale: () -> Unit,
    val onOpenSettings: () -> Unit,
)

@Composable
fun rememberLocationPermissionState(): LocationPermissionState {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var granted by remember { mutableStateOf(context.hasFineLocation()) }
    var blocked by rememberSaveable { mutableStateOf(false) }
    var rationaleVisible by rememberSaveable { mutableStateOf(false) }

    // FINE dan COARSE diminta bersamaan (wajib sejak Android 12). SSID hanya bisa
    // dibaca dengan lokasi presisi, jadi yang menentukan adalah hasil FINE.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        blocked = !granted &&
            activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // User bisa mengubah izin dari Setelan lalu kembali ke aplikasi; cek ulang setiap resume.
    LifecycleResumeEffect(Unit) {
        granted = context.hasFineLocation()
        if (granted) blocked = false
        onPauseOrDispose { }
    }

    return LocationPermissionState(
        access = when {
            granted -> LocationAccess.GRANTED
            blocked -> LocationAccess.BLOCKED
            else -> LocationAccess.REQUESTABLE
        },
        isRationaleVisible = rationaleVisible,
        onRequest = { rationaleVisible = true },
        onConfirmRationale = {
            rationaleVisible = false
            launcher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        },
        onDismissRationale = { rationaleVisible = false },
        onOpenSettings = { context.openAppSettings() },
    )
}

private fun Context.hasFineLocation(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
