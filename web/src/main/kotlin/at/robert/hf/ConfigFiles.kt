package at.robert.hf

import at.robert.Config
import at.robert.util.yamlObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

object ConfigFiles {
    private val baseFolder = System.getenv("DATA_FOLDER")?.let { File(it) } ?: File("config")

    init {
        if (!baseFolder.exists()) {
            baseFolder.mkdirs()
        }
    }

    fun getConfigs(): List<String> {
        return baseFolder.listFiles()?.map { it.name } ?: emptyList()
    }

    fun getConfig(name: String): Config {
        return yamlObjectMapper.readValue(File(baseFolder, name).also {
            require(it.exists() && it.isFile) { "Config file does not exist" }
        })
    }

    fun uploadConfig(name: String, content: String) {
        File(baseFolder, name).writeText(content)
    }

    fun deleteConfig(name: String) {
        File(baseFolder, name).also {
            require(it.exists() && it.isFile) { "Config file does not exist" }
        }.delete()
    }
}
