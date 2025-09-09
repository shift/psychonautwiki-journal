package com.isaakhanimann.journal.desktop

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BasicTest : StringSpec({
    "basic test should pass" {
        val result = 2 + 2
        result shouldBe 4
    }
})