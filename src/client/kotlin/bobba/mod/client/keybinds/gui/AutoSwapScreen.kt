package bobba.mod.client.keybinds.gui

import bobba.mod.client.gui.BobbaScreen
import bobba.mod.client.keybinds.Keybinds
import bobba.mod.client.skyblock.AutoPresetSwitcher
import bobba.mod.client.skyblock.SkyblockIsland
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Configures automatic keybind-preset switching per SkyBlock island.
 *
 * Each island cycles through [none, ...presets]; the master toggle and default-preset fallback sit
 * at the top. Islands are paged since the full list is longer than the panel.
 */
class AutoSwapScreen(parent: Screen?) :
    BobbaScreen(Component.literal("Auto-swap Presets"), parent) {

    companion object {
        private const val ROW_HEIGHT = 22
        private const val CYCLE_WIDTH = 150
        private const val NAME_LEFT_INSET = 15

        fun open() {
            val mc = Minecraft.getInstance()
            mc.execute { mc.setScreen(AutoSwapScreen(mc.screen)) }
        }
    }

    override val panelWidth: Int = 340

    private val islands = SkyblockIsland.entries
    private var page = 0

    private val listTop get() = panelContentTop + 52
    private val perPage: Int get() = ((panelFooterTop - 6 - listTop) / ROW_HEIGHT).coerceAtLeast(1)
    private val pageCount: Int get() = ((islands.size + perPage - 1) / perPage).coerceAtLeast(1)

    override fun init() {
        page = page.coerceIn(0, pageCount - 1)
        addHeaderControls()
        addIslandRows()
        addFooterControls()
    }

    private fun addHeaderControls() {
        val enabled = Keybinds.isAutoSwapEnabled()
        val toggleLabel = Component.literal("Auto-swap: ")
            .append(
                if (enabled) Component.literal("ON").withStyle(ChatFormatting.GREEN)
                else Component.literal("OFF").withStyle(ChatFormatting.RED)
            )
        addRenderableWidget(
            Button.builder(toggleLabel) {
                Keybinds.setAutoSwap(!enabled)
                if (!enabled) AutoPresetSwitcher.applyCurrent()
                rebuildWidgets()
            }.bounds(panelLeft + NAME_LEFT_INSET, panelContentTop, 150, 20).build()
        )

        val default = Keybinds.defaultPreset()
        val defaultLabel = Component.literal("Default: ")
            .append(
                Component.literal(default ?: "None").withStyle(
                    if (default != null) ChatFormatting.AQUA else ChatFormatting.GRAY
                )
            )
        addRenderableWidget(
            Button.builder(defaultLabel) {
                Keybinds.setDefaultPreset(cyclePreset(default))
                rebuildWidgets()
            }.bounds(panelRight - NAME_LEFT_INSET - CYCLE_WIDTH, panelContentTop, CYCLE_WIDTH, 20).build()
        )
    }

    private fun addIslandRows() {
        val start = page * perPage
        val pageItems = islands.drop(start).take(perPage)
        pageItems.forEachIndexed { i, island ->
            val rowY = listTop + i * ROW_HEIGHT
            val assigned = Keybinds.presetForIsland(island.id)
            val cycleLabel = Component.literal(assigned ?: "— None —").withStyle(
                if (assigned != null) ChatFormatting.AQUA else ChatFormatting.DARK_GRAY
            )
            addRenderableWidget(
                Button.builder(cycleLabel) {
                    val next = cyclePreset(assigned)
                    if (next == null) Keybinds.unassignIsland(island.id)
                    else Keybinds.assignIsland(island.id, next)
                    rebuildWidgets()
                }.bounds(panelRight - NAME_LEFT_INSET - CYCLE_WIDTH, rowY - 2, CYCLE_WIDTH, 20).build()
            )
        }
    }

    private fun addFooterControls() {
        if (pageCount > 1) {
            addRenderableWidget(
                Button.builder(Component.literal("<")) {
                    page = (page - 1 + pageCount) % pageCount
                    rebuildWidgets()
                }.bounds(panelLeft + NAME_LEFT_INSET, panelFooterTop, 20, 20).build()
            )
            addRenderableWidget(
                Button.builder(Component.literal(">")) {
                    page = (page + 1) % pageCount
                    rebuildWidgets()
                }.bounds(panelLeft + NAME_LEFT_INSET + 24, panelFooterTop, 20, 20).build()
            )
        }
        addDoneButton()
    }

    /** Returns the next preset in the cycle [null -> preset1 -> ... -> null]. */
    private fun cyclePreset(current: String?): String? {
        val options = buildList {
            add(null)
            addAll(Keybinds.presets().map { it.name })
        }
        val idx = options.indexOf(current).coerceAtLeast(0)
        return options[(idx + 1) % options.size]
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)

        graphics.text(
            font,
            "Island → preset when auto-swap is on:",
            panelLeft + NAME_LEFT_INSET, panelContentTop + 30, 0xFFAAAAAA.toInt(), true
        )
        graphics.fill(
            panelLeft + NAME_LEFT_INSET, listTop - 6,
            panelRight - NAME_LEFT_INSET, listTop - 5, 0xFF606060.toInt()
        )

        val start = page * perPage
        islands.drop(start).take(perPage).forEachIndexed { i, island ->
            val rowY = listTop + i * ROW_HEIGHT
            graphics.text(font, island.displayName, panelLeft + NAME_LEFT_INSET, rowY + 4, 0xFFFFFFFF.toInt(), true)
        }

        if (pageCount > 1) {
            graphics.text(
                font, "Page ${page + 1}/$pageCount",
                panelLeft + NAME_LEFT_INSET + 50, panelFooterTop + 6, 0xFFAAAAAA.toInt(), true
            )
        }
    }
}
