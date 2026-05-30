package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.GroceryViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CrashReproTest {

    @Test
    fun testStartupCrash() = runTest {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val prefs = app.getSharedPreferences("smart_grocery_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("user_token", "fake-token-test").apply()
        
        val viewModel = GroceryViewModel(app)
        viewModel.startPollingActiveSessions()
        
        delay(1000)
    }
}
