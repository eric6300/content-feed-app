package com.eric.contentfeed

import android.app.Application
import com.eric.contentfeed.core.di.coreModule
import com.eric.contentfeed.feed.di.feedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ContentFeedApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ContentFeedApplication)
            modules(coreModule, feedModule)
        }
    }
}
