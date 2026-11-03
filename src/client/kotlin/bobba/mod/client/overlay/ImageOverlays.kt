package bobba.mod.client.overlay

import bobba.mod.client.media.MediaLibrary
import bobba.mod.client.media.MediaTextures
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.name

/** The user's image overlays. Render-thread only. */
object ImageOverlays {
    private val logger = LoggerFactory.getLogger("bobbamod/overlays")

    private val list = mutableListOf<ImageOverlay>()

    /** In draw order: later entries are drawn on top. */
    val overlays: List<ImageOverlay> get() = list

    fun load() {
        list.clear()
        list.addAll(ImageOverlaysStorage.load())
    }

    fun save() = ImageOverlaysStorage.save(list)

    /**
     * Copies image files into the media library off-thread, then adds one overlay per file.
     * [onDone] runs on the render thread with the number added and a message per failure.
     */
    fun importImages(paths: List<Path>, onDone: (added: Int, errors: List<String>) -> Unit) {
        Thread({
            val names = mutableListOf<String>()
            val errors = mutableListOf<String>()
            for (path in paths) {
                if (!MediaLibrary.isImage(path)) {
                    errors += "${path.name}: not a PNG, JPG, GIF or BMP"
                    continue
                }
                try {
                    names += MediaLibrary.import(path)
                } catch (e: Exception) {
                    logger.warn("Failed to import {}", path, e)
                    errors += "${path.name}: ${e.message}"
                }
            }
            Minecraft.getInstance().execute {
                // Cascade new overlays so several imports don't land exactly on top of each other.
                names.forEach { name ->
                    val offset = (list.size % 8) * 0.03
                    list += ImageOverlay(file = name, x = 0.05 + offset, y = 0.05 + offset)
                }
                if (names.isNotEmpty()) save()
                onDone(names.size, errors)
            }
        }, "bobbamod-media-import").apply { isDaemon = true }.start()
    }

    fun remove(overlay: ImageOverlay) {
        if (!list.remove(overlay)) return
        save()
        // Nothing else references media files yet, so an unused file is just clutter.
        if (list.none { it.file == overlay.file }) {
            MediaTextures.release(overlay.file)
            try {
                MediaLibrary.delete(overlay.file)
            } catch (e: Exception) {
                logger.warn("Failed to delete media file {}", overlay.file, e)
            }
        }
    }
}
