package bobba.mod.client.keybinds

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object KeybindsStorage {
    private const val DEFAULT_PRESET_NAME = "Default"

    private val logger = LoggerFactory.getLogger("bobbamod/keybinds")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val file: Path by lazy {
        val dir = FabricLoader.getInstance().configDir.resolve("bobbamod")
        Files.createDirectories(dir)
        dir.resolve("keybinds.json")
    }

    fun load(): KeybindsData {
        if (!file.exists()) return defaultData()
        val text = file.readText()
        return try {
            val parsed = JsonParser.parseString(text)
            if (parsed.isJsonArray) {
                // Legacy v1 format: a flat array of entries.
                val entries = gson.fromJson(text, Array<KeybindEntry>::class.java)?.toMutableList() ?: mutableListOf()
                KeybindsData(DEFAULT_PRESET_NAME, mutableListOf(KeybindPreset(DEFAULT_PRESET_NAME, entries)))
            } else {
                gson.fromJson(text, KeybindsData::class.java) ?: defaultData()
            }
        } catch (e: Exception) {
            logger.error("Failed to load keybinds from {}", file, e)
            defaultData()
        }
    }

    fun save(data: KeybindsData) {
        try {
            file.writeText(gson.toJson(data))
        } catch (e: Exception) {
            logger.error("Failed to save keybinds to {}", file, e)
        }
    }

    private fun defaultData() = KeybindsData(
        DEFAULT_PRESET_NAME,
        mutableListOf(KeybindPreset(DEFAULT_PRESET_NAME, mutableListOf())),
    )
}
