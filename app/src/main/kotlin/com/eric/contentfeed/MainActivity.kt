@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.eric.contentfeed.navigation.ContentFeedApp
import com.eric.contentfeed.ui.theme.ContentFeedTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val foregroundCoordinator: ForegroundCoordinator by inject()

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            foregroundCoordinator.onStart()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContentFeedTheme {
                ContentFeedApp()
            }
        }
    }
}
