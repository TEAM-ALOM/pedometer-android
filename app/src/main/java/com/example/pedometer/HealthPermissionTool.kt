
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class HealthPermissionTool(private val activity: FragmentActivity) {

    private val healthConnectClient: HealthConnectClient = HealthConnectClient.getOrCreate(activity)
    private val providerPackageName = "com.google.android.apps.healthdata"

    suspend fun checkSdkStatusAndPromptForInstallation(): Boolean {
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

    suspend fun getStepsForDate(startTime: Instant, endTime: Instant): AggregationResult {
        return healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(
                    startTime = startTime,
                    endTime = endTime
                )
            )
        )
    }
}
