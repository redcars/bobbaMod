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

/** What a feature accepts, and how big a file may be. */
enum class MediaKind(val extensions: Set<String>, val maxBytes: Long, val label: String) {
    // Decoded image frames live in RAM and VRAM; a file this big is almost certainly a mistake.
    IMAGE(setOf("png", "jpg", "jpeg", "gif", "bmp"), 64L * 1024 * 1024, "PNG, JPG, GIF or BMP"),

    // Audio and video stream from disk, so only a sanity cap.
    CLIP(
        setOf("mp3", "ogg", "wav", "m4a", "flac", "mp4", "webm", "mkv", "mov"),
        512L * 1024 * 1024,
        "MP3, OGG, WAV, M4A, FLAC, MP4, WEBM, MKV or MOV",
    );

    fun accepts(path: Path) = path.extension.lowercase() in extensions
}

/**
 * Imported media files, copied into `config/bobbamod/media/` so features keep working after the
 * original file is moved or deleted. Features reference files by their stored name only.
 */
object MediaLibrary {
    private val logger = LoggerFactory.getLogger("bobbamod/media")

    val dir: Path by lazy {
        val dir = FabricLoader.getInstance().configDir.resolve("bobbamod").resolve("media")
        Files.createDirectories(dir)
        dir
    }

    /** Path of a stored file. Rejects names that would escape the media folder. */
    fun resolve(name: String): Path {
        val path = dir.resolve(name).normalize()
        require(path.parent == dir.normalize()) { "Invalid media file name: $name" }
        return path
    }

    /** Copies [source] into the library and returns its stored name. Blocking; call off-thread. */
    private fun import(source: Path, kind: MediaKind): String {
        if (!source.isRegularFile()) throw IOException("not a file")
        if (source.fileSize() > kind.maxBytes) throw IOException("larger than ${kind.maxBytes / (1024 * 1024)} MB")

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
     * Copies files of [kind] into the library off-thread. [onDone] runs on the render thread with the
     * stored names and a message per file that was skipped.
     */
    fun importFiles(paths: List<Path>, kind: MediaKind, onDone: (names: List<String>, errors: List<String>) -> Unit) {
        Thread({
            val names = mutableListOf<String>()
            val errors = mutableListOf<String>()
            for (path in paths) {
                if (!kind.accepts(path)) {
                    errors += "${path.name}: not a ${kind.label} file"
                    continue
                }
                try {
                    names += import(path, kind)
                } catch (e: Exception) {
                    logger.warn("Failed to import {}", path, e)
                    errors += "${path.name}: ${e.message}"
                }
            }
            Minecraft.getInstance().execute { onDone(names, errors) }
        }, "bobbamod-media-import").apply { isDaemon = true }.start()
    }

    fun importImages(paths: List<Path>, onDone: (names: List<String>, errors: List<String>) -> Unit) =
        importFiles(paths, MediaKind.IMAGE, onDone)

    private fun sanitize(base: String): String =
        base.replace(Regex("[^A-Za-z0-9 ._()-]"), "_").trim().ifEmpty { "media" }.take(64)
}
