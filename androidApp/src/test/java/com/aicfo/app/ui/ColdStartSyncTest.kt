package com.aicfo.app.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.aicfo.app.theme.AiCfoTheme
import com.aicfo.shared.sync.SyncTrigger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ColdStartSyncTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun firstCompositionRefreshesOnceWhenTheActivityIsAlreadyStarted() {
        val controller = Robolectric.buildActivity(LateSetContentActivity::class.java).setup()
        controller.get().show()
        rule.waitForIdle()

        val vm = ViewModelProvider(controller.get())[AiCfoViewModel::class.java]
        assertEquals(SyncTrigger.ColdStart, vm.controller.lastSyncTrigger())

        controller.pause().stop()
        rule.waitForIdle()
        controller.start().resume()
        rule.waitForIdle()

        assertEquals(SyncTrigger.Foreground, vm.controller.lastSyncTrigger())
    }
}

/** Composition after [setup] matches a first frame that lands once the activity is started. */
class LateSetContentActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun show() {
        setContent {
            AiCfoTheme {
                AiCfoRoot()
            }
        }
    }
}
