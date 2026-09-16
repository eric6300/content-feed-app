package com.eric.contentfeed.di

import com.eric.contentfeed.ForegroundCoordinator
import org.koin.dsl.module

val appModule =
    module {
        single { ForegroundCoordinator(get(), get()) }
    }
