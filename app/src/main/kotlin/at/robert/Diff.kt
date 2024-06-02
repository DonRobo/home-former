package at.robert

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlin.reflect.KProperty

data class Change(
    val path: List<String>,
    val oldValue: Any?,
    val newValue: Any?,
) {
    override fun toString(): String {
        val operation = when {
            oldValue == null -> "+"
            newValue == null -> "-"
            else -> "~"
        }
        val path = path.joinToString(".")
        fun Any?.diffToString() = when (this) {
            null -> "null"
            is String -> "\"$this\""
            else -> this.toString()
        }
        return when (operation) {
            "+" -> "$operation$path=${newValue.diffToString()}"
            "-" -> "$operation$path=${oldValue.diffToString()}"
            "~" -> "$operation$path=${newValue.diffToString()} (was ${oldValue.diffToString()})"
            else -> error("Unreachable")
        }
    }
}

data class Diff(
    val changes: List<Change>,
)

fun <T : Any> diff(old: T?, new: T?, path: List<String> = emptyList()): Diff {
    if (old == new) return Diff(emptyList())

    val simplifiedOld = simplify(old)
    val simplifiedNew = simplify(new)

    if (simplifiedOld != null && simplifiedNew != null) {
        require(simplifiedOld::class.java == simplifiedNew::class.java) {
            "Can't diff $simplifiedOld and $simplifiedNew, different types"
        }
    }

    @Suppress("UNCHECKED_CAST") // lots of casting, but the previous checks should make them safe
    return when {
        simplifiedOld == simplifiedNew -> Diff(emptyList())
        simplifiedOld == null -> Diff(listOf(Change(path, null, simplifiedNew)))
        simplifiedNew == null -> Diff(listOf(Change(path, simplifiedOld, null)))
        else -> when (simplifiedOld) {
            is List<*> -> diffList(simplifiedOld as List<Any?>, simplifiedNew as List<Any?>, path)
            is Set<*> -> diffSet(simplifiedOld as Set<Any?>, simplifiedNew as Set<Any?>, path)
            is Map<*, *> -> diffMap(simplifiedOld as Map<Any?, Any?>, simplifiedNew as Map<Any?, Any?>, path)
            else -> Diff(
                listOf(Change(path, simplifiedOld, simplifiedNew))
            )
        }
    }
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

private fun ObjectNode.toMap(): Map<String, Any?> {
    return fields().asSequence().associate { (key, value) ->
        key to value
    }
}

private fun Any.toMap(): Map<String, Any?> {
    if (this is JsonNode) {
        return this.toMap()
    } else {
        require(this::class.java.packageName.startsWith("at.robert")) {
            "Can't analyze $this, not a home-former class"
        }
    }

    val properties = this::class.members.mapNotNull { it as? KProperty<*> }
    require(properties.isNotEmpty()) {
        "Can't analyze $this, no properties found"
    }
    return properties.associate {
        it.name to it.getter.call(this)
    }
}

private fun simplify(value: Any?): Any? {
    return when (value) {
        null -> null
        is String, is Int, is Float, is Double, is Long, is Short, is Byte, is Char -> value
        is Array<*> -> value.toList()
        is ObjectNode -> value.toMap()
        is Collection<*> -> value
        else -> if (value::class.java.packageName.startsWith("at.robert")) {
            value.toMap()
        } else {
            error("Unsupported type: $value (${value.javaClass.name})")
        }
    }
}
