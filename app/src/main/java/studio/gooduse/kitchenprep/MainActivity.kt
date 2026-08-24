package studio.gooduse.kitchenprep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import studio.gooduse.kitchenprep.monetization.BillingController
import studio.gooduse.kitchenprep.monetization.ConsentController

class MainActivity : ComponentActivity() {
    lateinit var billingController: BillingController
        private set
    lateinit var consentController: ConsentController
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The approved HTML contains its own platform-status chrome. Hiding the real
        // status bar prevents duplicate chrome and preserves the frozen review layout.
        WindowInsetsControllerCompat(window, window.decorView)
            .hide(WindowInsetsCompat.Type.statusBars())

        billingController = BillingController(this, lifecycleScope)
        consentController = ConsentController(this)

        setContent {
            KitchenPrepClosedTestApp(
                activity = this,
                billingController = billingController,
                consentController = consentController,
            )
        }

        billingController.attach(this)
        consentController.attach(this)
    }

    override fun onResume() {
        super.onResume()
        if (::billingController.isInitialized) billingController.reconcile()
    }

    override fun onDestroy() {
        if (::billingController.isInitialized) billingController.close()
        super.onDestroy()
    }
}
