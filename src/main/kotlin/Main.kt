import io.github.hsedjame.trackIt.Attribute
import io.github.hsedjame.trackIt.TrackingClient
import io.github.hsedjame.trackIt.TrackingContext
import io.github.hsedjame.trackIt.Value
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

class ServiceA(private val ctx: TrackingContext, private val serviceB: ServiceB) {
    suspend fun call(i: Int): String =
        ctx.track {
            set(
                Value.StringValue("Event#$i") to Attribute.TealiumAttrs.Event,
                Value.StringValue("Home") to Attribute.PageAttrs.Name,
                Value.BooleanValue(true) to Attribute.UserAttrs.Email.Hidden,
                Value.StringValue("env") to Attribute.RootAttrs.Environment
            )

            serviceB.call(i)
        }
}

class ServiceB(private val ctx: TrackingContext) {
    suspend fun call(i: Int): String =
            ctx.track {
                this.set(Attribute.PageAttrs.Url, Value.StringValue("${i}_https://test.com"))
                "test"
            }
}

fun main() {
    runBlocking {
        val client = TrackingClient()
        val ctx = TrackingContext(client)
        val serviceB = ServiceB(ctx)
        val serviceA = ServiceA(ctx, serviceB)
        repeat(1) {
            launch {
                //println("Thread => ${Thread.currentThread().name}")
                measureTimeMillis {
                    serviceA.call(it)
                    //println("response #$it => ${service.doSomething(it)}");
                }.also { t -> println("$it -> time: $t") }
            }
        }
    }

}