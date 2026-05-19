package bobba.mod.client.watchlist.gui

import bobba.mod.client.gui.BobbaScreen
import bobba.mod.client.hypixel.HypixelApi
import bobba.mod.client.hypixel.HypixelRank
import bobba.mod.client.watchlist.MojangApi
import bobba.mod.client.watchlist.Watchlist
import bobba.mod.client.watchlist.WatchlistEntry
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class WatchlistEditorScreen(parent: Screen?) :
    BobbaScreen(Component.literal("Watchlist Editor"), parent) {

    companion object {
        private const val MAX_VISIBLE = 12

        fun open() {
            val mc = Minecraft.getInstance()
            mc.execute { mc.setScreen(WatchlistEditorScreen(mc.screen)) }
        }
    }

    override val panelWidth: Int = 320

    private lateinit var addInput: EditBox

    override fun init() {
        val centerX = width / 2

        addInput = EditBox(font, centerX - 100, panelContentTop, 160, 20, Component.literal("ign"))
        addInput.setHint(Component.literal("Player IGN..."))
        addInput.setMaxLength(16)
        addRenderableWidget(addInput)

        addRenderableWidget(
            Button.builder(Component.literal("Add")) { addCurrentInput() }
                .bounds(centerX + 65, panelContentTop, 35, 20)
                .build()
        )

        val entries = Watchlist.entries.sortedBy { it.ign.lowercase() }
        entries.take(MAX_VISIBLE).forEachIndexed { i, entry ->
            val y = panelContentTop + 30 + i * 22
            addRenderableWidget(
                Button.builder(Component.literal("X")) {
                    Watchlist.remove(entry.ign)
                    rebuildWidgets()
                }.bounds(centerX + 65, y, 20, 20).build()
            )
        }

        addDoneButton()
    }

    private fun addCurrentInput() {
        val ign = addInput.value.trim()
        if (ign.isEmpty()) return
        if (Watchlist.add(WatchlistEntry(ign = ign))) {
            MojangApi.resolveProfile(ign).thenAccept { profile ->
                if (profile == null) return@thenAccept
                Watchlist.attachProfile(ign, profile.uuid, profile.name)
                HypixelApi.resolveRank(profile.uuid).thenAccept { rank ->
                    if (rank != null) Watchlist.attachRank(profile.uuid, rank)
                }
            }
        }
        addInput.value = ""
        rebuildWidgets()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(graphics, mouseX, mouseY, delta)
        val centerX = width / 2

        val entries = Watchlist.entries.sortedBy { it.ign.lowercase() }
        if (entries.isEmpty()) {
            graphics.drawCenteredString(
                font,
                Component.literal("No players on your watchlist."),
                centerX, panelContentTop + 70, 0xFFAAAAAA.toInt()
            )
            return
        }
        entries.take(MAX_VISIBLE).forEachIndexed { i, entry ->
            val y = panelContentTop + 30 + i * 22 + 6
            val rank = entry.rank ?: HypixelRank.NONE
            val prefix = if (rank.prefix.isNotEmpty()) "${rank.prefix} " else ""
            val nameComponent = Component.literal("$prefix${entry.ign}").withStyle(rank.color)
            val line = Component.empty().append(nameComponent)
            if (entry.uuid == null) {
                line.append(Component.literal(" (pending)").withStyle(ChatFormatting.GRAY))
            }
            graphics.drawString(font, line, centerX - 100, y, 0xFFFFFFFF.toInt())
        }
        if (entries.size > MAX_VISIBLE) {
            graphics.drawCenteredString(
                font,
                Component.literal("Showing $MAX_VISIBLE of ${entries.size} — use /watchlist list to see all."),
                centerX, panelFooterTop - 12, 0xFFAAAAAA.toInt()
            )
        }
    }
}
