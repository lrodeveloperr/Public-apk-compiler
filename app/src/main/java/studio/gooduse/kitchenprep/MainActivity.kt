package studio.gooduse.kitchenprep

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import studio.gooduse.kitchenprep.monetization.BillingController
import studio.gooduse.kitchenprep.monetization.ConsentController

class MainActivity : AppCompatActivity() {
    lateinit var billingController: BillingController
        private set
    lateinit var consentController: ConsentController
        private set

    private val kitchenViewModel: KitchenViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* optional */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        billingController = BillingController(this, lifecycleScope)
        consentController = ConsentController(this)

        setContent {
            KitchenPrepClosedTestApp(
                activity = this,
                billingController = billingController,
                consentController = consentController,
                viewModel = kitchenViewModel,
            )
        }

        billingController.attach(this)
        consentController.attach(this)
        handleShareIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (::billingController.isInitialized) billingController.reconcile()
        kitchenViewModel.reconcileTimers()
    }

    override fun onDestroy() {
        if (::billingController.isInitialized) billingController.close()
        super.onDestroy()
    }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        if (text.isNotBlank()) kitchenViewModel.handleSharedText(text)
    }
}
