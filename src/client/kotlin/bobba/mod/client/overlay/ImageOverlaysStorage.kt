package bobba.mod.client.overlay

import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object ImageOverlaysStorage {
    private val logger = LoggerFactory.getLogger("bobbamod/overlays")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    // Single background thread so disk writes stay ordered; daemon so it never blocks JVM exit.
    private val writeExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "bobbamod-overlays-writer").apply { isDaemon = true }
    }

    private val file: Path by lazy {
        val dir = FabricLoader.getInstance().configDir.resolve("bobbamod")
        Files.createDirectories(dir)
        dir.resolve("overlays.json")
    }

    fun load(): List<ImageOverlay> {
        if (!file.exists()) return emptyList()
        return try {
            gson.fromJson(file.readText(), Array<ImageOverlay>::class.java)
                ?.mapNotNull(::sanitize)
                ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to load overlays from {}", file, e)
            emptyList()
        }
    }

    /**
     * Gson skips Kotlin constructors, so fields missing from the file come back null or 0 instead
     * of their defaults. Drop entries without a file and repair the rest.
     */
    @Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
    private fun sanitize(overlay: ImageOverlay?): ImageOverlay? {
        if (overlay == null || overlay.file == null) return null
        return overlay.copy(
            id = overlay.id ?: UUID.randomUUID().toString(),
            width = if (overlay.width > 0) overlay.width else 0.2,
            opacity = overlay.opacity.coerceIn(0.0, 1.0),
        )
    }

    fun save(overlays: List<ImageOverlay>) {
        // Serialize on the calling thread for a consistent snapshot; only the write goes async.
        val json = gson.toJson(overlays)
        writeExecutor.execute {
            try {
                file.writeText(json)
            } catch (e: Exception) {
                logger.error("Failed to save overlays to {}", file, e)
            }
        }
    }
}
