package io.github.hsedjame.trackIt


class TrackingContext(private val client: TrackingClient) {

    private val store = ThreadLocal<TrackingRequestBuilder?>()

    suspend fun <R> track(block: suspend TrackingRequestBuilder.() -> R): R = client.track(block, store)
}