package bobba.mod.client.watchlist.gui

import bobba.mod.client.watchlist.MojangApi
import bobba.mod.client.watchlist.Watchlist
import bobba.mod.client.watchlist.WatchlistEntry
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class WatchlistEditorScreen(private val parent: Screen?) :
    Screen(Component.literal("Watchlist Editor")) {

    companion object {
        private const val MAX_VISIBLE = 12

        fun open() {
            val mc = Minecraft.getInstance()
            mc.execute { mc.setScreen(WatchlistEditorScreen(mc.screen)) }
        }
    }

    private lateinit var addInput: EditBox

    override fun init() {
        val centerX = width / 2

        addInput = EditBox(font, centerX - 100, 36, 160, 20, Component.literal("ign"))
        addInput.setHint(Component.literal("Player IGN..."))
        addInput.setMaxLength(16)
        addRenderableWidget(addInput)

        addRenderableWidget(
            Button.builder(Component.literal("Add")) { addCurrentInput() }
                .bounds(centerX + 65, 36, 35, 20)
                .build()
        )

        val entries = Watchlist.entries.sortedBy { it.ign.lowercase() }
        entries.take(MAX_VISIBLE).forEachIndexed { i, entry ->
            val y = 70 + i * 22
            addRenderableWidget(
                Button.builder(Component.literal("X")) {
                    Watchlist.remove(entry.ign)
                    rebuildWidgets()
                }.bounds(centerX + 65, y, 20, 20).build()
            )
        }

        addRenderableWidget(
            Button.builder(Component.literal("Done")) { onClose() }
                .bounds(centerX - 50, height - 30, 100, 20)
                .build()
        )
    }

    private fun addCurrentInput() {
        val ign = addInput.value.trim()
        if (ign.isEmpty()) return
        if (Watchlist.add(WatchlistEntry(ign = ign))) {
            MojangApi.resolveUuid(ign).thenAccept { uuid ->
                if (uuid != null) Watchlist.attachUuid(ign, uuid)
            }
        }
        addInput.value = ""
        rebuildWidgets()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(graphics, mouseX, mouseY, delta)
        val centerX = width / 2
        graphics.drawCenteredString(font, title, centerX, 14, 0xFFFFFFFF.toInt())

        val entries = Watchlist.entries.sortedBy { it.ign.lowercase() }
        if (entries.isEmpty()) {
            graphics.drawCenteredString(
                font,
                Component.literal("No players on your watchlist."),
                centerX, 100, 0xFFAAAAAA.toInt()
            )
            return
        }
        entries.take(MAX_VISIBLE).forEachIndexed { i, entry ->
            val y = 70 + i * 22 + 6
            val suffix = if (entry.uuid == null) " (pending)" else ""
            graphics.drawString(font, "${entry.ign}$suffix", centerX - 100, y, 0xFFFFFFFF.toInt())
        }
        if (entries.size > MAX_VISIBLE) {
            graphics.drawCenteredString(
                font,
                Component.literal("Showing $MAX_VISIBLE of ${entries.size} — use /watchlist list to see all."),
                centerX, height - 50, 0xFFAAAAAA.toInt()
            )
        }
    }

    override fun onClose() {
        Minecraft.getInstance().setScreen(parent)
    }
}
