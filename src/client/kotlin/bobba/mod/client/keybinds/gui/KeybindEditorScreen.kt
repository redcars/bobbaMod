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
        private const val PRESETS_SECTION_WIDTH = 140
        private const val PRESETS_ADD_BUTTON_WIDTH = 20

        fun open() {
            val mc = Minecraft.getInstance()
            mc.execute { mc.setScreen(KeybindEditorScreen(mc.screen)) }
        }
    }

    override val panelWidth: Int =
        LEFT_SECTION_WIDTH + DIVIDER_GAP + PRESETS_SECTION_WIDTH + 10

    private var capturingForIndex: Int? = null
    private lateinit var presetNameInput: EditBox

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
        val presetButtonWidth = PRESETS_SECTION_WIDTH - PRESETS_ADD_BUTTON_WIDTH - 3
        val canDelete = presets.size > 1

        presets.take(MAX_VISIBLE).forEachIndexed { i, preset ->
            val rowY = panelContentTop + i * ROW_HEIGHT
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

            if (canDelete) {
                addRenderableWidget(
                    Button.builder(Component.literal("X")) {
                        Keybinds.removePreset(preset.name)
                        capturingForIndex = null
                        rebuildWidgets()
                    }.bounds(presetsLeft + presetButtonWidth + 3, rowY, PRESETS_ADD_BUTTON_WIDTH, 20).build()
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
            }.bounds(presetsLeft + presetButtonWidth + 3, inputY, PRESETS_ADD_BUTTON_WIDTH, 20).build()
        )

        val autoSwapLabel = if (Keybinds.isAutoSwapEnabled()) "Auto-swap: ON" else "Auto-swap…"
        addRenderableWidget(
            Button.builder(Component.literal(autoSwapLabel)) { AutoSwapScreen.open() }
                .bounds(presetsLeft, panelFooterTop, PRESETS_SECTION_WIDTH, 20).build()
        )
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
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
