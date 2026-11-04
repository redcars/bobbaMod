package bobba.mod.client.media

import net.minecraft.client.Minecraft
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/** Native "open file" dialog (tinyfd ships with Minecraft). */
object MediaFilePicker {
    private val open = AtomicBoolean(false)

    fun pickImages(onPicked: (List<Path>) -> Unit) = pick(MediaKind.IMAGE, "Choose images or GIFs", "Images", onPicked)

    /**
     * Shows the dialog and passes the chosen files to [onPicked] on the render thread. Does nothing
     * if a dialog is already open. The dialog blocks its thread, so it runs on its own to keep the
     * game rendering.
     */
    fun pick(kind: MediaKind, title: String, filterName: String, onPicked: (List<Path>) -> Unit) {
        if (!open.compareAndSet(false, true)) return
        Thread({
            try {
                val result = MemoryStack.stackPush().use { stack ->
                    val patterns = stack.mallocPointer(kind.extensions.size)
                    kind.extensions.forEach { patterns.put(stack.UTF8("*.$it")) }
                    patterns.flip()
                    TinyFileDialogs.tinyfd_openFileDialog(title, null, patterns, filterName, true)
                }
                // Multiple selections come back joined with '|'.
                val paths = result?.split('|')?.filter { it.isNotBlank() }?.map { Path.of(it) }.orEmpty()
                if (paths.isNotEmpty()) Minecraft.getInstance().execute { onPicked(paths) }
            } finally {
                open.set(false)
            }
        }, "bobbamod-file-picker").apply { isDaemon = true }.start()
    }
}
