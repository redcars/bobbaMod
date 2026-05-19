package bobba.mod.client.keybinds.gui

import bobba.mod.client.gui.BobbaScreen
import bobba.mod.client.keybinds.KeyName
import bobba.mod.client.keybinds.KeybindEntry
import bobba.mod.client.keybinds.Keybinds
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
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
        private const val COMMAND_FIELD_WIDTH = 220
        private const val REMOVE_BUTTON_WIDTH = 20
        private const val ROW_GAP = 5

        fun open() {
            val mc = Minecraft.getInstance()
            mc.execute { mc.setScreen(KeybindEditorScreen(mc.screen)) }
        }
    }

    override val panelWidth: Int = 380

    private var capturingForIndex: Int? = null

    override fun init() {
        val entries = Keybinds.all()
        val rowWidth = KEY_BUTTON_WIDTH + ROW_GAP + COMMAND_FIELD_WIDTH + ROW_GAP + REMOVE_BUTTON_WIDTH
        val rowX = width / 2 - rowWidth / 2

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

            val xX = cmdX + COMMAND_FIELD_WIDTH + ROW_GAP
            addRenderableWidget(
                Button.builder(Component.literal("X")) {
                    Keybinds.removeAt(i)
                    capturingForIndex = null
                    rebuildWidgets()
                }.bounds(xX, rowY, REMOVE_BUTTON_WIDTH, 20).build()
            )
        }

        val addButtonY = panelContentTop + (entries.size.coerceAtMost(MAX_VISIBLE)) * ROW_HEIGHT + 6
        addRenderableWidget(
            Button.builder(Component.literal("+ Add binding")) {
                Keybinds.add(KeybindEntry(0, ""))
                capturingForIndex = null
                rebuildWidgets()
            }.bounds(width / 2 - 75, addButtonY, 150, 20).build()
        )

        addDoneButton()
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

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(graphics, mouseX, mouseY, delta)

        val entries = Keybinds.all()
        if (entries.isEmpty()) {
            graphics.drawCenteredString(
                font,
                Component.literal("No keybinds yet. Click + Add binding to start."),
                width / 2, panelContentTop + 40, 0xFFAAAAAA.toInt()
            )
        }
        if (entries.size > MAX_VISIBLE) {
            graphics.drawCenteredString(
                font,
                Component.literal("Showing $MAX_VISIBLE of ${entries.size} — use /keybinds list to see all."),
                width / 2, panelFooterTop - 12, 0xFFAAAAAA.toInt()
            )
        }
    }
}
