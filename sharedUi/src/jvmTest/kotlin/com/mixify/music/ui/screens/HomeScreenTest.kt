package com.mixify.music.ui.screens

import com.mixify.music.sharedui.resources.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class HomeScreenTest {

    @Test
    fun `greeting morning bounds`() {
        val early = LocalDateTime.of(2026, 1, 1, 5, 0)
        assertEquals(Res.string.greeting_morning, getGreetingForTime(early).first)
        assertEquals(Res.string.greeting_msg_05_07, getGreetingForTime(early).second)

        val lateMorning = LocalDateTime.of(2026, 1, 1, 11, 59)
        assertEquals(Res.string.greeting_morning, getGreetingForTime(lateMorning).first)
        assertEquals(Res.string.greeting_msg_10_11, getGreetingForTime(lateMorning).second)
    }

    @Test
    fun `greeting afternoon bounds`() {
        val noon = LocalDateTime.of(2026, 1, 1, 12, 0)
        assertEquals(Res.string.greeting_afternoon, getGreetingForTime(noon).first)
        assertEquals(Res.string.greeting_msg_12_13, getGreetingForTime(noon).second)

        val lateAfternoon = LocalDateTime.of(2026, 1, 1, 16, 59)
        assertEquals(Res.string.greeting_afternoon, getGreetingForTime(lateAfternoon).first)
        assertEquals(Res.string.greeting_msg_16, getGreetingForTime(lateAfternoon).second)
    }

    @Test
    fun `greeting evening bounds`() {
        val evening = LocalDateTime.of(2026, 1, 1, 17, 0)
        assertEquals(Res.string.greeting_evening, getGreetingForTime(evening).first)
        assertEquals(Res.string.greeting_msg_17_18, getGreetingForTime(evening).second)

        val lateEvening = LocalDateTime.of(2026, 1, 1, 20, 59)
        assertEquals(Res.string.greeting_evening, getGreetingForTime(lateEvening).first)
        assertEquals(Res.string.greeting_msg_19_20, getGreetingForTime(lateEvening).second)
    }

    @Test
    fun `greeting night bounds including midnight`() {
        val night = LocalDateTime.of(2026, 1, 1, 21, 0)
        assertEquals(Res.string.greeting_night, getGreetingForTime(night).first)
        assertEquals(Res.string.greeting_msg_21_22, getGreetingForTime(night).second)

        val midnight = LocalDateTime.of(2026, 1, 1, 0, 0)
        assertEquals(Res.string.greeting_night, getGreetingForTime(midnight).first)
        assertEquals(Res.string.greeting_msg_23_00, getGreetingForTime(midnight).second)

        val lateNight = LocalDateTime.of(2026, 1, 1, 4, 59)
        assertEquals(Res.string.greeting_night, getGreetingForTime(lateNight).first)
        assertEquals(Res.string.greeting_msg_03_04, getGreetingForTime(lateNight).second)
    }
}
