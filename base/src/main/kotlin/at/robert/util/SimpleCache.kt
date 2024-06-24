package at.robert.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class SimpleCache(
    private val defaultLifetime: Duration?
) {
    private val mutexes = ConcurrentHashMap<String, Mutex>()
    private fun mutex(key: String) = mutexes.computeIfAbsent(key) { Mutex() }

    private data class CacheEntry(
        val value: Any?,
        val createdOn: Instant,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    suspend fun <T> cached(
        key: String,
        lifetime: Duration? = defaultLifetime,
        block: suspend CoroutineScope.() -> T
    ): T {
        mutex(key).withLock {
            val cachedValue = cache[key]
            val earliestCreationDate = lifetime?.let { Instant.now().minus(it) }
            if (cachedValue != null && cachedValue.createdOn.isAfter(earliestCreationDate)) {
                @Suppress("UNCHECKED_CAST")
                return cachedValue.value as T
            } else {
                val computedValue = coroutineScope(block)
                cache[key] = CacheEntry(computedValue, Instant.now())
                return computedValue
            }
        }
    }
}
