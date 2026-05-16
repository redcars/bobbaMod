package bobba.mod.client.watchlist

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object WatchlistStorage {
    private val logger = LoggerFactory.getLogger("bobbamod/watchlist")

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Instant::class.java,
            JsonSerializer<Instant> { src, _, _ -> JsonPrimitive(src.toString()) })
        .registerTypeAdapter(Instant::class.java,
            JsonDeserializer { json, _, _ -> Instant.parse(json.asString) })
        .registerTypeAdapter(UUID::class.java,
            JsonSerializer<UUID> { src, _, _ -> JsonPrimitive(src.toString()) })
        .registerTypeAdapter(UUID::class.java,
            JsonDeserializer { json, _, _ -> UUID.fromString(json.asString) })
        .create()

    private val file: Path by lazy {
        val dir = FabricLoader.getInstance().configDir.resolve("bobbamod")
        Files.createDirectories(dir)
        dir.resolve("watchlist.json")
    }

    fun load(): List<WatchlistEntry> {
        if (!file.exists()) return emptyList()
        return try {
            gson.fromJson(file.readText(), Array<WatchlistEntry>::class.java)
                ?.toList()
                ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to load watchlist from {}", file, e)
            emptyList()
        }
    }

    fun save(entries: Collection<WatchlistEntry>) {
        try {
            file.writeText(gson.toJson(entries))
        } catch (e: Exception) {
            logger.error("Failed to save watchlist to {}", file, e)
        }
    }
}
