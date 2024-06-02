package at.robert

import com.fasterxml.jackson.databind.JsonNode

fun mergeStates(currentState: JsonNode, update: JsonNode): JsonNode {
    return jsonObjectMapper.readerForUpdating(currentState).readValue(update)
}
