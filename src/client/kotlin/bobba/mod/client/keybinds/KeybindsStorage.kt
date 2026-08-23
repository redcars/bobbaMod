package bobba.mod.client.keybinds

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object KeybindsStorage {
    private const val DEFAULT_PRESET_NAME = "Default"

    private val logger = LoggerFactory.getLogger("bobbamod/keybinds")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    // Single background thread so disk writes stay ordered/serialized; daemon so it never blocks JVM exit.
    private val writeExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "bobbamod-keybinds-writer").apply { isDaemon = true }
    }

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
                sanitize(gson.fromJson(text, KeybindsData::class.java)) ?: defaultData()
            }
        } catch (e: Exception) {
            logger.error("Failed to load keybinds from {}", file, e)
            defaultData()
        }
    }

    /**
     * Backfills fields that older config files omit. Gson instantiates Kotlin data classes without
     * running constructors, so absent collection fields come back null rather than as their
     * declared defaults; normalize them here so the rest of the code can rely on non-null sets.
     */
    @Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
    private fun sanitize(data: KeybindsData?): KeybindsData? {
        data ?: return null
        if (data.presets == null) return null
        var data = data
        // Gson may leave `active` null/blank; anchor it to the first preset so load() isn't the only guard.
        if (data.active.isNullOrBlank() && data.presets.isNotEmpty()) {
            data = data.copy(active = data.presets.first().name)
        }
        data.presets.forEachIndexed { i, preset ->
            if (preset.keybinds == null || preset.islands == null) {
                data.presets[i] = preset.copy(
                    keybinds = preset.keybinds ?: mutableListOf(),
                    islands = preset.islands ?: mutableSetOf(),
                )
            }
        }
        return data
    }

    fun save(data: KeybindsData) {
        // Serialize on the calling thread for a consistent snapshot of the mutable data, then hand
        // off only the file write to the background executor (avoids a torn/concurrent read).
        val json = gson.toJson(data)
        writeExecutor.execute {
            try {
                file.writeText(json)
            } catch (e: Exception) {
                logger.error("Failed to save keybinds to {}", file, e)
            }
        }
    }

    private fun defaultData() = KeybindsData(
        DEFAULT_PRESET_NAME,
        mutableListOf(KeybindPreset(DEFAULT_PRESET_NAME, mutableListOf())),
    )
}
