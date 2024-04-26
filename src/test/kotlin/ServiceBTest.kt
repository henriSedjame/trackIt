import io.github.hsedjame.trackIt.TrackingContext
import io.github.imagineDevit.giwt.core.annotations.Test
import io.github.imagineDevit.giwt.kt.TestCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest


class ServiceBTest {

    val ctx = mockk<TrackingContext>()
    val service = ServiceB(ctx)

    @Test
    fun `test service B`(tc: TestCase<Int, String>) {
            tc
                .given("", 1)
                .and(""){
                    coEvery { ctx.track<String>(any()) } returns "result"
                    it
                }
                .`when`("") {
                    runBlocking {  service.call(it) }
                }
                .then("") {
                    result {
                        shouldBe equalTo "result"
                    }
                }

    }
}