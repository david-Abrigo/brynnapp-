package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.parser.ParseResult
import com.example.service.parser.YapeNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Brynn", appName)
  }

  @Test
  fun `parse standard yape payment successfully`() {
    val result = YapeNotificationParser.parse(
      title = "Yape",
      text = "¡Te yapearon! Juan Perez te envió S/ 25.00",
      packageName = "com.bcp.innovacxion.yapeapp"
    )

    assertTrue(result is ParseResult.Success)
    val success = result as ParseResult.Success
    assertEquals("Juan Perez", success.senderName)
    assertEquals(25.0, success.amount, 0.001)
  }

  @Test
  fun `ignore outbound payments`() {
    val result = YapeNotificationParser.parse(
      title = "Yape",
      text = "Enviaste S/ 30.00 a Maria Flores",
      packageName = "com.bcp.innovacxion.yapeapp"
    )

    assertTrue(result is ParseResult.Ignored)
  }
}

