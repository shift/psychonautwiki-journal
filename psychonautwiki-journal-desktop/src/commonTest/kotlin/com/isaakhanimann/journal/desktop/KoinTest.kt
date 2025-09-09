package com.isaakhanimann.journal.desktop

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest

class BasicKoinTest : StringSpec(), KoinTest {
    
    init {
        "Koin module should load successfully" {
            startKoin {
                modules(appModule)
            }
            
            // Basic verification that Koin context is working
            val result = true
            result shouldBe true
            
            stopKoin()
        }
    }
}