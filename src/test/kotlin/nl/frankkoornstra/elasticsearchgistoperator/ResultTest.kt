package nl.frankkoornstra.elasticsearchgistoperator

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
internal class ResultTest {
    @Test
    fun `create success`() {
        Assertions.assertNotNull(SuccessResult())
    }

    @Test
    fun `create failure with reason`() {
        Assertions.assertEquals("foo", FailedResult(
            "foo"
        ).reason)
    }
}
