package at.robert.provider

import at.robert.Diff

interface Provider<T> {
    var config: Any

    fun currentState(): T
    fun applyDiff(diff: Diff)
}

fun getProvider(providerName: String, config: Any?): Provider<*> {
    val clazz = if (providerName.contains(".")) {
        Class.forName(providerName)
    } else {
        Class.forName("at.robert.provider.$providerName")
    }
    val instance = clazz.getConstructor().newInstance() as Provider<*>
    if (config != null) {
        instance.config = config
    }
    return instance
}
