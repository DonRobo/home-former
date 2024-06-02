package at.robert.provider

import at.robert.Diff

interface Provider<T> {
    fun currentState(): T
    fun applyDiff(diff: Diff)
}

fun getProvider(providerName: String, providerConfig: Any?): Provider<*> {
    val clazz = if (providerName.contains(".")) {
        Class.forName(providerName)
    } else {
        Class.forName("at.robert.provider.$providerName")
    }
    return if (providerConfig == null) {
        clazz.getConstructor().newInstance() as Provider<*>
    } else {
        clazz.getConstructor(providerConfig::class.java).newInstance(providerConfig) as Provider<*>
    }
}
