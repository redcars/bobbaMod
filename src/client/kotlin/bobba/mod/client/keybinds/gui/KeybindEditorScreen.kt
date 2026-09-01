package bobba.mod.client.keybinds.gui

import bobba.mod.client.gui.BobbaScreen
import bobba.mod.client.keybinds.KeyName
import bobba.mod.client.keybinds.KeybindEntry
import bobba.mod.client.keybinds.Keybinds
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component

class KeybindEditorScreen(parent: Screen?) :
    BobbaScreen(Component.literal("Keybind Editor"), parent) {

    companion object {
        private const val MAX_VISIBLE = 10
        private const val ROW_HEIGHT = 24

        private const val KEY_BUTTON_WIDTH = 80
        private const val COMMAND_FIELD_WIDTH = 195
        private const val TOGGLE_BUTTON_WIDTH = 40
        private const val REMOVE_BUTTON_WIDTH = 20
        private const val ROW_GAP = 5

        private const val LEFT_SECTION_WIDTH = 375
        private const val DIVIDER_GAP = 10
        private const val PRESETS_SECTION_WIDTH = 165
        private const val PRESETS_ADD_BUTTON_WIDTH = 20
        private const val PRESETS_BUTTON_GAP = 3

        private val NAME_ERROR_COLOR = 0xFFFF5555.toInt()

        fun open() {
            val mc = Minecraft.getInstance()
            mc.execute { mc.setScreen(KeybindEditorScreen(mc.screen)) }
        }
    }

    override val panelWidth: Int =
        LEFT_SECTION_WIDTH + DIVIDER_GAP + PRESETS_SECTION_WIDTH + 10

    private var capturingForIndex: Int? = null
    private lateinit var presetNameInput: EditBox

    /** Name of the preset currently being renamed in place, if any. */
    private var renamingPreset: String? = null
    private var renameInput: EditBox? = null

    private val leftSectionLeft get() = panelLeft + 5
    private val leftSectionCenter get() = leftSectionLeft + LEFT_SECTION_WIDTH / 2
    private val dividerX get() = leftSectionLeft + LEFT_SECTION_WIDTH + DIVIDER_GAP / 2
    private val presetsLeft get() = dividerX + DIVIDER_GAP / 2

    override fun init() {
        renderKeybindRows()
        renderPresetsColumn()
        addDoneButton()
    }

    private fun renderKeybindRows() {
        val entries = Keybinds.all()
        val rowWidth = KEY_BUTTON_WIDTH + ROW_GAP + COMMAND_FIELD_WIDTH + ROW_GAP +
            TOGGLE_BUTTON_WIDTH + ROW_GAP + REMOVE_BUTTON_WIDTH
        val rowX = leftSectionCenter - rowWidth / 2

        entries.take(MAX_VISIBLE).forEachIndexed { i, entry ->
            val rowY = panelContentTop + i * ROW_HEIGHT
            val keyLabel = if (capturingForIndex == i) "Press a key..." else KeyName.displayName(entry.keyCode)

            addRenderableWidget(
                Button.builder(Component.literal(keyLabel)) {
                    capturingForIndex = i
                    rebuildWidgets()
                }.bounds(rowX, rowY, KEY_BUTTON_WIDTH, 20).build()
            )

            val cmdX = rowX + KEY_BUTTON_WIDTH + ROW_GAP
            val cmdField = EditBox(font, cmdX, rowY, COMMAND_FIELD_WIDTH, 20, Component.empty())
            cmdField.setMaxLength(256)
            cmdField.value = entry.command
            cmdField.setHint(Component.literal("/hub"))
            cmdField.setResponder { newVal ->
                val current = Keybinds.all()
                if (i < current.size) {
                    Keybinds.updateAt(i, current[i].copy(command = newVal))
                }
            }
            addRenderableWidget(cmdField)

            val toggleX = cmdX + COMMAND_FIELD_WIDTH + ROW_GAP
            val toggleLabel = if (entry.isEnabled) {
                Component.literal("ON").withStyle(ChatFormatting.GREEN)
            } else {
                Component.literal("OFF").withStyle(ChatFormatting.RED)
            }
            addRenderableWidget(
                Button.builder(toggleLabel) {
                    val current = Keybinds.all()
                    if (i < current.size) {
                        Keybinds.updateAt(i, current[i].copy(enabled = !current[i].isEnabled))
                    }
                    rebuildWidgets()
                }.bounds(toggleX, rowY, TOGGLE_BUTTON_WIDTH, 20).build()
            )

            val xX = toggleX + TOGGLE_BUTTON_WIDTH + ROW_GAP
            addRenderableWidget(
                Button.builder(Component.literal("X")) {
                    Keybinds.removeAt(i)
                    capturingForIndex = null
                    rebuildWidgets()
                }.bounds(xX, rowY, REMOVE_BUTTON_WIDTH, 20).build()
            )
        }

        val addBindingY = panelContentTop + entries.size.coerceAtMost(MAX_VISIBLE) * ROW_HEIGHT + 6
        addRenderableWidget(
            Button.builder(Component.literal("+ Add binding")) {
                Keybinds.add(KeybindEntry(0, ""))
                capturingForIndex = null
                rebuildWidgets()
            }.bounds(leftSectionCenter - 75, addBindingY, 150, 20).build()
        )
    }

    private fun renderPresetsColumn() {
        val presets = Keybinds.presets()
        val activeName = Keybinds.activeName()
        // Two trailing buttons per row (rename + delete); the add row reuses the same name width.
        val presetButtonWidth =
            PRESETS_SECTION_WIDTH - 2 * (PRESETS_ADD_BUTTON_WIDTH + PRESETS_BUTTON_GAP)
        val renameButtonX = presetsLeft + presetButtonWidth + PRESETS_BUTTON_GAP
        val deleteButtonX = renameButtonX + PRESETS_ADD_BUTTON_WIDTH + PRESETS_BUTTON_GAP
        val canDelete = presets.size > 1

        renameInput = null
        presets.take(MAX_VISIBLE).forEachIndexed { i, preset ->
            val rowY = panelContentTop + i * ROW_HEIGHT
            if (preset.name == renamingPreset) {
                addRenameRow(preset.name, rowY, presetButtonWidth, renameButtonX)
                return@forEachIndexed
            }

            val isActive = preset.name == activeName
            val labelText = if (isActive) "> ${preset.name}" else preset.name
            val labelComponent = Component.literal(labelText).withStyle(
                if (isActive) ChatFormatting.GREEN else ChatFormatting.WHITE
            )

            addRenderableWidget(
                Button.builder(labelComponent) {
                    Keybinds.setActive(preset.name)
                    capturingForIndex = null
                    rebuildWidgets()
                }.bounds(presetsLeft, rowY, presetButtonWidth, 20).build()
            )

            addRenderableWidget(
                Button.builder(Component.literal("✎")) {
                    renamingPreset = preset.name
                    capturingForIndex = null
                    rebuildWidgets()
                }.tooltip(Tooltip.create(Component.literal("Rename preset")))
                    .bounds(renameButtonX, rowY, PRESETS_ADD_BUTTON_WIDTH, 20).build()
            )

            if (canDelete) {
                addRenderableWidget(
                    Button.builder(Component.literal("X")) {
                        Keybinds.removePreset(preset.name)
                        capturingForIndex = null
                        rebuildWidgets()
                    }.bounds(deleteButtonX, rowY, PRESETS_ADD_BUTTON_WIDTH, 20).build()
                )
            }
        }

        val inputY = panelContentTop + presets.size.coerceAtMost(MAX_VISIBLE) * ROW_HEIGHT + 6
        presetNameInput = EditBox(
            font, presetsLeft, inputY,
            presetButtonWidth, 20,
            Component.empty()
        )
        presetNameInput.setHint(Component.literal("Name..."))
        presetNameInput.setMaxLength(32)
        addRenderableWidget(presetNameInput)

        addRenderableWidget(
            Button.builder(Component.literal("+")) {
                val name = presetNameInput.value.trim()
                if (name.isNotEmpty() && Keybinds.addPreset(name)) {
                    Keybinds.setActive(name)
                    presetNameInput.value = ""
                    capturingForIndex = null
                    rebuildWidgets()
                }
            }.bounds(renameButtonX, inputY, PRESETS_ADD_BUTTON_WIDTH, 20).build()
        )

        val autoSwapLabel = if (Keybinds.isAutoSwapEnabled()) "Auto-swap: ON" else "Auto-swap…"
        addRenderableWidget(
            Button.builder(Component.literal(autoSwapLabel)) { AutoSwapScreen.open() }
                .bounds(presetsLeft, panelFooterTop, PRESETS_SECTION_WIDTH, 20).build()
        )
    }

    /**
     * Focus the name field while renaming. Vanilla runs this right after [init], so doing it here
     * (rather than inside [addRenameRow]) keeps the tab-navigation default from stealing the focus.
     */
    override fun setInitialFocus() {
        val input = renameInput
        if (input != null) setInitialFocus(input) else super.setInitialFocus()
    }

    /** Replaces a preset row with an in-place name field plus a confirm button. */
    private fun addRenameRow(name: String, rowY: Int, nameWidth: Int, confirmX: Int) {
        val input = EditBox(font, presetsLeft, rowY, nameWidth, 20, Component.empty())
        input.setMaxLength(32)
        input.value = name
        input.moveCursorToEnd(false)
        // Clear the "name taken" tint as soon as the name is edited again.
        input.setResponder { input.setTextColor(EditBox.DEFAULT_TEXT_COLOR) }
        addRenderableWidget(input)
        renameInput = input

        addRenderableWidget(
            Button.builder(Component.literal("✔")) { commitRename() }
                .tooltip(Tooltip.create(Component.literal("Save name (Enter)")))
                .bounds(confirmX, rowY, PRESETS_ADD_BUTTON_WIDTH, 20).build()
        )
    }

    private fun commitRename() {
        val oldName = renamingPreset ?: return
        val input = renameInput
        val newName = input?.value?.trim().orEmpty()
        if (newName.isEmpty() || newName == oldName) {
            cancelRename()
            return
        }
        if (!Keybinds.renamePreset(oldName, newName)) {
            // Another preset already uses that name; stay in the field and flag it.
            input?.setTextColor(NAME_ERROR_COLOR)
            return
        }
        renamingPreset = null
        renameInput = null
        rebuildWidgets()
    }

    private fun cancelRename() {
        renamingPreset = null
        renameInput = null
        rebuildWidgets()
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        if (renamingPreset != null) {
            when (keyEvent.key()) {
                InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                    commitRename()
                    return true
                }
                InputConstants.KEY_ESCAPE -> {
                    cancelRename()
                    return true
                }
            }
        }

        val capturing = capturingForIndex
        if (capturing != null) {
            val keyCode = keyEvent.key()
            if (keyCode == InputConstants.KEY_ESCAPE) {
                capturingForIndex = null
                rebuildWidgets()
                return true
            }
            val current = Keybinds.all()
            if (capturing < current.size) {
                Keybinds.updateAt(capturing, current[capturing].copy(keyCode = keyCode))
            }
            capturingForIndex = null
            rebuildWidgets()
            return true
        }
        return super.keyPressed(keyEvent)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)

        // Divider line between keybind column and presets column
        graphics.fill(dividerX, panelContentTop - 4, dividerX + 1, panelFooterTop - 4, 0xFF606060.toInt())

        // Section header for presets
        graphics.text(font, "Presets", presetsLeft, panelTop + 14, 0xFFFFFFFF.toInt(), true)

        val entries = Keybinds.all()
        if (entries.isEmpty()) {
            graphics.centeredText(
                font,
                Component.literal("No keybinds in this preset."),
                leftSectionCenter, panelContentTop + 40, 0xFFAAAAAA.toInt()
            )
        }
        if (entries.size > MAX_VISIBLE) {
            graphics.centeredText(
                font,
                Component.literal("Showing $MAX_VISIBLE of ${entries.size}."),
                leftSectionCenter, panelFooterTop - 12, 0xFFAAAAAA.toInt()
            )
        }
    }
}
