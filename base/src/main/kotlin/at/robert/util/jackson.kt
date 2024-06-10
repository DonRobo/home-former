package at.robert.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule

val jsonObjectMapper = jacksonObjectMapper().also {
    it.setSerializationInclusion(JsonInclude.Include.NON_NULL)
}
val yamlObjectMapper = JsonMapper
    .builder(YAMLFactory())
    .addModule(kotlinModule())
    .serializationInclusion(JsonInclude.Include.NON_NULL)
    .build()
