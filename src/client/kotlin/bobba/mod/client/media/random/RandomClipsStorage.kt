package bobba.mod.client.media.random

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

object RandomClipsStorage {
    private val logger = LoggerFactory.getLogger("bobbamod/random-media")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    // Single background thread so disk writes stay ordered; daemon so it never blocks JVM exit.
    private val writeExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "bobbamod-random-media-writer").apply { isDaemon = true }
    }

    private val file: Path by lazy {
        val dir = FabricLoader.getInstance().configDir.resolve("bobbamod")
        Files.createDirectories(dir)
        dir.resolve("random_media.json")
    }

    fun load(): List<RandomClip> {
        if (!file.exists()) return emptyList()
        return try {
            gson.fromJson(file.readText(), Array<RandomClip>::class.java)?.mapNotNull(::sanitize) ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to load random media from {}", file, e)
            emptyList()
        }
    }

    /**
     * Gson skips Kotlin constructors, so fields missing from the file come back null or 0 instead
     * of their defaults. Drop entries without a file and repair the rest.
     */
    @Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
    private fun sanitize(clip: RandomClip?): RandomClip? {
        if (clip == null || clip.file == null) return null
        return clip.copy(
            id = clip.id ?: UUID.randomUUID().toString(),
            trigger = clip.trigger ?: Trigger.INTERVAL,
            minMinutes = clip.minMinutes.coerceIn(RandomClipRanges.MINUTES),
            maxMinutes = clip.maxMinutes.coerceIn(RandomClipRanges.MINUTES).coerceAtLeast(clip.minMinutes),
            chancePercent = clip.chancePercent.coerceIn(RandomClipRanges.CHANCE),
            periodSeconds = clip.periodSeconds.coerceIn(RandomClipRanges.PERIOD),
            volume = clip.volume.coerceIn(0.0, 1.0),
            size = clip.size.coerceIn(RandomClipRanges.SIZE),
        )
    }

    fun save(clips: List<RandomClip>) {
        // Serialize on the calling thread for a consistent snapshot; only the write goes async.
        val json = gson.toJson(clips)
        writeExecutor.execute {
            try {
                file.writeText(json)
            } catch (e: Exception) {
                logger.error("Failed to save random media to {}", file, e)
            }
        }
    }
}

/** Allowed values for the clip settings, shared by storage and the editor's sliders. */
object RandomClipRanges {
    val MINUTES = 0.5..600.0
    val CHANCE = 0.001..100.0
    val PERIOD = 1.0..3600.0
    val SIZE = 0.1..1.0
}
