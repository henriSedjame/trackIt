package io.github.hsedjame.trackIt

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel


class TrackingClient {

    suspend fun <R> track(
        block: suspend TrackingRequestBuilder.() -> R,
        store: ThreadLocal<TrackingRequestBuilder?>
    ): R {
        val builder = if (store.isPresent()) {
            store.get()
        } else {
            TrackingRequestBuilder()
        }
        return coroutineScope {

            val channel = Channel<R>(1)

            launch(Dispatchers.Default + store.asContextElement(builder)) {
                channel.send(store.get()!!.block())
            }

            channel.receive().apply {
                launch {
                    try {
                        (store.get())?.let {
                            // TODO send tracking
                            println("${Thread.currentThread().name} ::: Tracking request => ${it.build()}")
                        }
                    } catch (e: Exception) {
                        println("Tracking failed for reason => ${e.message}")
                    }

                }
            }
        }
    }

}