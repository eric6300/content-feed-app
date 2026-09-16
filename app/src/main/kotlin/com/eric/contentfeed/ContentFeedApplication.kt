package com.eric.contentfeed

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.eric.contentfeed.core.di.coreModule
import com.eric.contentfeed.di.appModule
import com.eric.contentfeed.feed.di.feedModule
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ContentFeedApplication : Application() {
    private val foregroundCoordinator: ForegroundCoordinator by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ContentFeedApplication)
            modules(coreModule, feedModule, appModule)
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    owner.lifecycleScope.launch {
                        foregroundCoordinator.onForeground()
                    }
                }
            },
        )
    }
}
