package at.robert

import kotlin.reflect.KProperty

data class Change(
    val path: List<String>,
    val oldValue: Any?,
    val newValue: Any?,
)

data class Diff(
    val changes: List<Change>,
)

fun <T : Any> diff(old: T?, new: T?, path: List<String> = emptyList()): Diff {
    @Suppress("UNCHECKED_CAST") // lots of casting happening here. Should be fine though
    return when {
        old == new -> Diff(emptyList())
        old == null -> Diff(listOf(Change(path, null, new)))
        new == null -> Diff(listOf(Change(path, old, null)))
        else -> when (old) {
            is String, is Int, is Float, is Double, is Long, is Short, is Byte, is Char -> Diff(
                listOf(Change(path, old, new))
            )

            is List<*> -> diffList(old as List<Any?>, new as List<Any?>, path)
            is Map<*, *> -> diffMap(old as Map<Any?, Any?>, new as Map<Any?, Any?>, path)
            else -> diffDataClass(old, new, path)
        }
    }
}

private fun diffDataClass(old: Any, new: Any, path: List<String>): Diff {
    return diffMap(old.toMap(), new.toMap(), path)
}

private fun diffList(old: List<Any?>, new: List<Any?>, path: List<String>): Diff {
    val indices = 0 until maxOf(old.size, new.size)
    val changes = buildList {
        indices.forEach { index ->
            addAll(
                diff(old.getOrNull(index), new.getOrNull(index), path + "[$index]").changes
            )
        }
    }
    return Diff(changes)
}

private fun diffSet(old: Set<Any?>, new: Set<Any?>, path: List<String>): Diff {
    val added = new - old
    val removed = old - new

    return Diff(
        added.map { Change(path, null, it) } +
                removed.map { Change(path, it, null) }
    )
}

private fun diffMap(old: Map<out Any?, Any?>, new: Map<out Any?, Any?>, path: List<String>): Diff {
    val addedProperties = new.keys - old.keys
    val removedProperties = old.keys - new.keys
    val sameProperties = old.keys.intersect(new.keys)
    val changes = buildList {
        sameProperties.forEach { key ->
            addAll(
                diff(old[key], new[key], path + key.toString()).changes
            )
        }
        addAll(addedProperties.map { key ->
            Change(path + key.toString(), null, new[key])
        })
        addAll(removedProperties.map { key ->
            Change(path + key.toString(), old[key], null)
        })
    }
    return Diff(changes)
}

private fun Any.toMap(): Map<String, Any?> {
    val properties = this::class.members.mapNotNull { it as? KProperty<*> }
    require(properties.isNotEmpty()) {
        "Can't analyze $this, no properties found"
    }
    return properties.associate {
        it.name to it.getter.call(this)
    }
}

