package bobba.mod.client.overlay

import bobba.mod.client.media.MediaLibrary
import java.nio.file.Path

/** The user's image overlays. Render-thread only. */
object ImageOverlays {
    private val list = mutableListOf<ImageOverlay>()

    /** In draw order: later entries are drawn on top. */
    val overlays: List<ImageOverlay> get() = list

    init {
        MediaLibrary.addReferences { list.map { it.file } }
    }

    fun load() {
        list.clear()
        list.addAll(ImageOverlaysStorage.load())
    }

    fun save() = ImageOverlaysStorage.save(list)

    /**
     * Imports image files and adds one overlay per file. [onDone] runs on the render thread with
     * the number added and a message per failure.
     */
    fun importImages(paths: List<Path>, onDone: (added: Int, errors: List<String>) -> Unit) {
        MediaLibrary.importImages(paths) { names, errors ->
            // Cascade new overlays so several imports don't land exactly on top of each other.
            names.forEach { name ->
                val offset = (list.size % 8) * 0.03
                list += ImageOverlay(file = name, x = 0.05 + offset, y = 0.05 + offset)
            }
            if (names.isNotEmpty()) save()
            onDone(names.size, errors)
        }
    }

    fun remove(overlay: ImageOverlay) {
        if (!list.remove(overlay)) return
        save()
        MediaLibrary.deleteIfUnused(overlay.file)
    }
}
