
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.health.connect.client.HealthConnectClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HealthPermissionTool(private val activity: FragmentActivity) {

    private val healthConnectClient: HealthConnectClient = HealthConnectClient.getOrCreate(activity)
    private val providerPackageName = "com.google.android.apps.healthdata"

    suspend fun checkSdkStatusAndPromptForInstallation(): Boolean {//헬스 커넥트 앱 설치 확인
        val availabilityStatus = HealthConnectClient.getSdkStatus(activity, providerPackageName)
        return when (availabilityStatus) {
            HealthConnectClient.SDK_AVAILABLE -> true
            HealthConnectClient.SDK_UNAVAILABLE -> false
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                showProviderUpdateDialog()
                false
            }
            else -> false
        }
    }

    private suspend fun showProviderUpdateDialog() = withContext(Dispatchers.Main) {
        val uriString = "market://details?id=$providerPackageName&url=https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata&hl=en-KR"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setPackage("com.android.vending")
            data = Uri.parse(uriString)
            putExtra("overlay", true)
            putExtra("callerId", activity.packageName)
        }
        activity.startActivity(intent)
    }

}
