package nl.frankkoornstra.elasticsearchgistoperator

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class TemplateStatusTest {
    @Test
    fun `can change vars`() {
        val status = TemplateStatus("foo", "bar")
        status.status = "bloo"
        status.message = "bla"

        Assertions.assertEquals("bloo", status.status)
        Assertions.assertEquals("bla", status.message)
    }
}
