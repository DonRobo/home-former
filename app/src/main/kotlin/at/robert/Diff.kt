package at.robert

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

data class Change(
    val path: List<String>,
    val oldValue: JsonNode?,
    val newValue: JsonNode?,
) {
    override fun toString(): String {
        val operation = when {
            oldValue == null -> "+"
            newValue == null -> "-"
            else -> "~"
        }
        val path = path.joinToString(".")
        return when (operation) {
            "+" -> "$operation$path=${newValue}"
            "-" -> "$operation$path=${oldValue}"
            "~" -> "$operation$path=${newValue} (was ${oldValue})"
            else -> error("Unreachable")
        }
    }
}

data class Diff(
    val changes: List<Change>,
)

fun <T : Any> diff(old: T?, new: T?, path: List<String> = emptyList()): Diff {
    if (old == new) return Diff(emptyList())

    val oldJson = jsonObjectMapper.valueToTree<JsonNode>(old)
    val newJson = jsonObjectMapper.valueToTree<JsonNode>(new)

    return when {
        oldJson == newJson -> Diff(emptyList())
        oldJson.isNull -> Diff(listOf(Change(path, null, newJson)))
        newJson.isNull -> Diff(listOf(Change(path, oldJson, null)))
        else -> Diff(jsonDiff(oldJson, newJson, path))
    }
}

fun jsonDiff(old: JsonNode, new: JsonNode, path: List<String>): List<Change> {
    if (old.javaClass != new.javaClass) {
        return listOf(Change(path, old, new))
    }

    return when (old) {
        is ObjectNode -> diffObject(old, new as ObjectNode, path).changes
        is ArrayNode -> diffArray(old, new as ArrayNode, path).changes
        else -> return listOf(Change(path, old, new))

    }
}

private fun diffArray(old: ArrayNode, new: ArrayNode, path: List<String>): Diff {
    val indices = 0 until maxOf(old.size(), new.size())
    fun ArrayNode.getOrNull(index: Int): JsonNode? {
        return if (index in 0 until size()) get(index) else null
    }

    val changes = buildList {
        indices.forEach { index ->
            addAll(
                diff(old.getOrNull(index), new.getOrNull(index), path + "[$index]").changes
            )
        }
    }
    return Diff(changes)
}

private fun diffObject(old: ObjectNode, new: ObjectNode, path: List<String>): Diff {
    val oldFieldNames = old.fieldNames().asSequence().toHashSet()
    val newFieldNames = new.fieldNames().asSequence().toHashSet()
    val addedProperties = newFieldNames - oldFieldNames
    val removedProperties = oldFieldNames - newFieldNames
    val sameProperties = oldFieldNames intersect newFieldNames
    val changes = buildList {
        sameProperties.forEach { key ->
            addAll(
                diff(old[key], new[key], path + key).changes
            )
        }
        addAll(addedProperties.map { key ->
            Change(path + key, null, new[key])
        })
        addAll(removedProperties.map { key ->
            Change(path + key, old[key], null)
        })
    }
    return Diff(changes)
}
