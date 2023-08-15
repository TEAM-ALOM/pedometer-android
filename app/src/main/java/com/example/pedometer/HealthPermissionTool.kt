package com.example.pedometer
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.health.connect.client.HealthConnectClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HealthPermissionTool(private val activity: FragmentActivity) {

    private val providerPackageName = "com.google.android.apps.healthdata"

    suspend fun checkSdkStatusAndPromptForInstallation(): Boolean {
        return when (HealthConnectClient.getSdkStatus(activity, providerPackageName)) {
            HealthConnectClient.SDK_AVAILABLE -> true
            HealthConnectClient.SDK_UNAVAILABLE -> {
                showInstallHealthConnectDialog()
                false
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                showProviderUpdateDialog()
                false
            }
            else -> false
        }
    }

    private suspend fun showInstallHealthConnectDialog() {
        val installUriString = "market://details?id=$providerPackageName"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(installUriString)
        }
        withContext(Dispatchers.Main) {
            activity.startActivity(intent)
        }
    }

    private suspend fun showProviderUpdateDialog() {
        val updateUriString = "market://details?id=$providerPackageName&url=https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata&hl=ko"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setPackage("com.android.vending")
            data = Uri.parse(updateUriString)
            putExtra("overlay", true)
            putExtra("callerId", activity.packageName)
        }
        withContext(Dispatchers.Main) {
            activity.startActivity(intent)
        }
    }
}
