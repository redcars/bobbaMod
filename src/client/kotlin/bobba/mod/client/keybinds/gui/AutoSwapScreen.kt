package bobba.mod.client.keybinds.gui

import bobba.mod.client.gui.BobbaScreen
import bobba.mod.client.gui.Dropdown
import bobba.mod.client.gui.DropdownOption
import bobba.mod.client.keybinds.Keybinds
import bobba.mod.client.skyblock.AutoPresetSwitcher
import bobba.mod.client.skyblock.SkyblockIsland
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

/**
 * Configures automatic keybind-preset switching per SkyBlock island.
 *
 * Each island (and the default-preset fallback) opens a dropdown listing "None" plus every preset;
 * the master toggle sits at the top. Islands are paged since the full list is longer than the panel.
 */
class AutoSwapScreen(parent: Screen?) :
    BobbaScreen(Component.literal("Auto-swap Presets"), parent) {

    companion object {
        private const val ROW_HEIGHT = 22
        private const val SELECT_WIDTH = 150
        private const val NAME_LEFT_INSET = 15
        private const val NONE_LABEL = "— None —"

        fun open() {
            val mc = Minecraft.getInstance()
            mc.execute { mc.setScreen(AutoSwapScreen(mc.screen)) }
        }
    }

    override val panelWidth: Int = 340

    private val islands = SkyblockIsland.entries
    private var page = 0

    /** The open preset dropdown, if any; at most one is open at a time. */
    private var dropdown: Dropdown<String?>? = null

    private val listTop get() = panelContentTop + 52
    private val perPage: Int get() = ((panelFooterTop - 6 - listTop) / ROW_HEIGHT).coerceAtLeast(1)
    private val pageCount: Int get() = ((islands.size + perPage - 1) / perPage).coerceAtLeast(1)

    private val selectLeft get() = panelRight - NAME_LEFT_INSET - SELECT_WIDTH

    override fun init() {
        dropdown = null
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
        addRenderableWidget(
            Button.builder(selectLabel("Default: ", default)) {
                openDropdown(panelContentTop, default) { picked ->
                    Keybinds.setDefaultPreset(picked)
                    rebuildWidgets()
                }
            }.bounds(selectLeft, panelContentTop, SELECT_WIDTH, 20).build()
        )
    }

    private fun addIslandRows() {
        val start = page * perPage
        val pageItems = islands.drop(start).take(perPage)
        pageItems.forEachIndexed { i, island ->
            val rowY = listTop + i * ROW_HEIGHT - 2
            val assigned = Keybinds.presetForIsland(island.id)
            addRenderableWidget(
                Button.builder(selectLabel("", assigned)) {
                    openDropdown(rowY, assigned) { picked ->
                        if (picked == null) Keybinds.unassignIsland(island.id)
                        else Keybinds.assignIsland(island.id, picked)
                        rebuildWidgets()
                    }
                }.bounds(selectLeft, rowY, SELECT_WIDTH, 20).build()
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

    /** Label for a select button: the assignment (or "None") plus a chevron hinting at the dropdown. */
    private fun selectLabel(prefix: String, preset: String?): Component =
        Component.literal(prefix)
            .append(
                Component.literal(preset ?: NONE_LABEL).withStyle(
                    if (preset != null) ChatFormatting.AQUA else ChatFormatting.DARK_GRAY
                )
            )
            .append(Component.literal(" ▼").withStyle(ChatFormatting.GRAY))

    private fun openDropdown(anchorY: Int, selected: String?, onPick: (String?) -> Unit) {
        val options = buildList {
            add(DropdownOption<String?>(null, Component.literal(NONE_LABEL).withStyle(ChatFormatting.GRAY)))
            Keybinds.presets().forEach {
                add(DropdownOption<String?>(it.name, Component.literal(it.name).withStyle(ChatFormatting.AQUA)))
            }
        }
        dropdown = Dropdown(selectLeft, anchorY, SELECT_WIDTH, 20, options, selected, height, onPick)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val open = dropdown ?: return super.mouseClicked(mouseButtonEvent, doubleClick)
        // A click outside the list just dismisses it; either way it never reaches the widgets below.
        if (!open.mouseClicked(mouseButtonEvent.x(), mouseButtonEvent.y())) dropdown = null
        return true
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        dropdown?.let { return it.mouseScrolled(scrollY) }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (dropdown != null && keyEvent.key() == InputConstants.KEY_ESCAPE) {
            dropdown = null
            return true
        }
        return super.keyPressed(keyEvent)
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

        // Rendered last so the option list paints over the rows it covers.
        dropdown?.render(graphics, font, mouseX, mouseY)
    }
}
