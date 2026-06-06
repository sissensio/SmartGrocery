package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GroceryViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ShoppingScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.StoresScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SmartGrocery", appName)
  }

  @Test
  fun `verify viewModel instantiation`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = GroceryViewModel(app)
    assertNotNull(viewModel)
  }

  @Test
  fun `verify HomeScreen rendering`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = GroceryViewModel(app)
    composeTestRule.setContent {
      MyApplicationTheme {
        HomeScreen(viewModel = viewModel, onNavigateToScanner = {}, onNavigateToReport = {})
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `verify ShoppingScreen rendering`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = GroceryViewModel(app)
    composeTestRule.setContent {
      MyApplicationTheme {
        ShoppingScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `verify ScannerScreen rendering`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = GroceryViewModel(app)
    composeTestRule.setContent {
      MyApplicationTheme {
        ScannerScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `verify LedgerScreen rendering`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = GroceryViewModel(app)
    composeTestRule.setContent {
      MyApplicationTheme {
        LedgerScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `verify StoresScreen rendering`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = GroceryViewModel(app)
    composeTestRule.setContent {
      MyApplicationTheme {
        StoresScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
  }
}

