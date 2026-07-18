package bobba.mod.client.watchlist.gui

import bobba.mod.client.gui.BobbaScreen
import bobba.mod.client.hypixel.HypixelApi
import bobba.mod.client.watchlist.MojangApi
import bobba.mod.client.watchlist.Watchlist
import bobba.mod.client.watchlist.WatchlistEntry
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class ChatActionsScreen(parent: Screen?, private val ign: String) :
    BobbaScreen(Component.literal("Watchlist"), parent) {

    override val panelWidth: Int = 280
    override val panelMargin: Int = 120

    override fun init() {
        val centerX = width / 2

        addRenderableWidget(
            Button.builder(Component.literal("Add to watchlist").withStyle(ChatFormatting.GREEN)) {
                doAdd()
                onClose()
            }.bounds(centerX - 90, panelContentTop + 30, 180, 20).build()
        )

        addRenderableWidget(
            Button.builder(Component.literal("Cancel")) { onClose() }
                .bounds(centerX - 50, panelFooterTop, 100, 20).build()
        )
    }

    private fun doAdd() {
        if (!Watchlist.add(WatchlistEntry(ign = ign))) return
        MojangApi.resolveProfile(ign).thenAccept { profile ->
            if (profile == null) return@thenAccept
            Watchlist.attachProfile(ign, profile.uuid, profile.name)
            HypixelApi.resolveRank(profile.uuid).thenAccept { rank ->
                if (rank != null) Watchlist.attachRank(profile.uuid, rank)
            }
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(graphics, mouseX, mouseY, delta)
        graphics.drawCenteredString(
            font,
            Component.literal("Add ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(ign).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" to your watchlist?").withStyle(ChatFormatting.WHITE)),
            width / 2,
            panelContentTop + 10,
            0xFFFFFFFF.toInt()
        )
    }
}
