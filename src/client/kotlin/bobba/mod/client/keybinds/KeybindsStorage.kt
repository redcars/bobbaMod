package bobba.mod.client.keybinds

import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object KeybindsStorage {
    private val logger = LoggerFactory.getLogger("bobbamod/keybinds")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val file: Path by lazy {
        val dir = FabricLoader.getInstance().configDir.resolve("bobbamod")
        Files.createDirectories(dir)
        dir.resolve("keybinds.json")
    }

    fun load(): List<KeybindEntry> {
        if (!file.exists()) return emptyList()
        return try {
            gson.fromJson(file.readText(), Array<KeybindEntry>::class.java)?.toList() ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to load keybinds from {}", file, e)
            emptyList()
        }
    }

    fun save(entries: Collection<KeybindEntry>) {
        try {
            file.writeText(gson.toJson(entries))
        } catch (e: Exception) {
            logger.error("Failed to save keybinds to {}", file, e)
        }
    }
}
