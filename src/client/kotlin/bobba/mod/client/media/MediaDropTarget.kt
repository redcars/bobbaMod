package bobba.mod.client.media

import java.nio.file.Path

/**
 * Routes files dropped onto the game window to the media editor currently on screen. MoulConfig's
 * screen doesn't forward drops, so editors re-register every frame they render and a drop only
 * counts if one rendered just now (see ScreenFilesDropMixin).
 */
object MediaDropTarget {
    private const val FRESH_MS = 250

    private var handler: ((List<Path>) -> Unit)? = null
    private var seenAt = 0L

    fun offer(handler: (List<Path>) -> Unit) {
        this.handler = handler
        seenAt = System.currentTimeMillis()
    }

    /** True if a visible editor took the files. */
    fun drop(paths: List<Path>): Boolean {
        val target = handler ?: return false
        if (System.currentTimeMillis() - seenAt > FRESH_MS) return false
        target(paths)
        return true
    }
}
