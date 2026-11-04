package bobba.mod.client.media

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/**
 * Imported media files, copied into `config/bobbamod/media/` so features keep working after the
 * original file is moved or deleted. Features reference files by their stored name only.
 */
object MediaLibrary {
    val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "bmp")

    // Decoded frames live in RAM and VRAM; a file this big is almost certainly a mistake.
    private const val MAX_FILE_BYTES = 64L * 1024 * 1024

    private val logger = LoggerFactory.getLogger("bobbamod/media")

    val dir: Path by lazy {
        val dir = FabricLoader.getInstance().configDir.resolve("bobbamod").resolve("media")
        Files.createDirectories(dir)
        dir
    }

    fun isImage(path: Path): Boolean = path.extension.lowercase() in IMAGE_EXTENSIONS

    /** Path of a stored file. Rejects names that would escape the media folder. */
    fun resolve(name: String): Path {
        val path = dir.resolve(name).normalize()
        require(path.parent == dir.normalize()) { "Invalid media file name: $name" }
        return path
    }

    /** Copies [source] into the library and returns its stored name. Blocking; call off-thread. */
    private fun import(source: Path): String {
        if (!source.isRegularFile()) throw IOException("not a file")
        if (source.fileSize() > MAX_FILE_BYTES) throw IOException("larger than 64 MB")

        val base = sanitize(source.name.substringBeforeLast('.'))
        val ext = source.extension.lowercase()
        var name = "$base.$ext"
        var n = 2
        while (Files.exists(dir.resolve(name))) {
            name = "$base ($n).$ext"
            n++
        }
        Files.copy(source, dir.resolve(name))
        return name
    }

    private val referenceSources = mutableListOf<() -> Collection<String>>()

    /** Registers the file names a feature uses, so [deleteIfUnused] keeps them. */
    fun addReferences(source: () -> Collection<String>) {
        referenceSources += source
    }

    /** Frees and deletes [name] unless some feature still uses it. Render thread only. */
    fun deleteIfUnused(name: String) {
        if (referenceSources.any { name in it() }) return
        MediaTextures.release(name)
        try {
            Files.deleteIfExists(resolve(name))
        } catch (e: Exception) {
            logger.warn("Failed to delete media file {}", name, e)
        }
    }

    /**
     * Copies image files into the library off-thread. [onDone] runs on the render thread with the
     * stored names and a message per file that was skipped.
     */
    fun importImages(paths: List<Path>, onDone: (names: List<String>, errors: List<String>) -> Unit) {
        Thread({
            val names = mutableListOf<String>()
            val errors = mutableListOf<String>()
            for (path in paths) {
                if (!isImage(path)) {
                    errors += "${path.name}: not a PNG, JPG, GIF or BMP"
                    continue
                }
                try {
                    names += import(path)
                } catch (e: Exception) {
                    logger.warn("Failed to import {}", path, e)
                    errors += "${path.name}: ${e.message}"
                }
            }
            Minecraft.getInstance().execute { onDone(names, errors) }
        }, "bobbamod-media-import").apply { isDaemon = true }.start()
    }

    private fun sanitize(base: String): String =
        base.replace(Regex("[^A-Za-z0-9 ._()-]"), "_").trim().ifEmpty { "image" }.take(64)
}
