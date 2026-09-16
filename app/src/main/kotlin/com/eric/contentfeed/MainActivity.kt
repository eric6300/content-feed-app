@file:Suppress("ktlint:standard:function-naming")

package com.eric.contentfeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.eric.contentfeed.navigation.ContentFeedApp
import com.eric.contentfeed.ui.theme.ContentFeedTheme

class MainActivity : ComponentActivity() {
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
