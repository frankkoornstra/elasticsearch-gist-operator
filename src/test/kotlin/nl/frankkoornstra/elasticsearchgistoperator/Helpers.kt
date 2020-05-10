package nl.frankkoornstra.elasticsearchgistoperator

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.junit.jupiter.api.Assertions

fun ListAppender<ILoggingEvent>.assertLevel(level: Level, index: Int = 0) {
    val actualLevel = list[index].level
    Assertions.assertEquals(level, actualLevel, "$actualLevel is not $level")
}

fun ListAppender<ILoggingEvent>.assertMessageContains(partial: String, index: Int = 0) {
    val message = list[index].message
    Assertions.assertTrue(message.contains(partial, true), "Partial '$partial' not found in '$message'")
}

fun ListAppender<ILoggingEvent>.assertHasMessageThatContains(partial: String) {
    val present = list.any { it.message.contains(partial, true) }
    Assertions.assertTrue(present, "Partial '$partial' not found in any message")
}

fun ListAppender<ILoggingEvent>.assertNoMessageThatContains(partial: String) {
    val message = list.firstOrNull { it.message.contains(partial, true) }
    Assertions.assertNull(message, "Partial '$partial' found in '$message'")
}
