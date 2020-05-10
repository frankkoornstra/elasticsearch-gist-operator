package nl.frankkoornstra.elasticsearchgistoperator

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class IndexStatusTest {
    @Test
    fun `can change vars`() {
        val status = IndexStatus("foo", "bar")
        status.status = "bloo"
        status.message = "bla"

        Assertions.assertEquals("bloo", status.status)
        Assertions.assertEquals("bla", status.message)
    }
}
