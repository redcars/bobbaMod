package bobba.mod.client.notify

import bobba.mod.client.config.ConfigManager
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents

object Notifier {
    /**
     * Shows a watchlist warning. When [note] is non-blank it is appended as a distinct styled
     * segment so the reminder you saved for that player stands out from the warning text.
     */
    fun warn(text: String, note: String? = null) {
        val mc = Minecraft.getInstance()
        mc.execute {
            val message = Component.literal("[BobbaMod] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(text).withStyle(ChatFormatting.YELLOW))
            note?.trim()?.takeIf { it.isNotEmpty() }?.let { trimmed ->
                message
                    .append(Component.literal(" — note: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(trimmed).withStyle(ChatFormatting.WHITE))
            }
            mc.gui.chat.addClientSystemMessage(message)
            if (ConfigManager.instance.watchlist.playSound) {
                mc.soundManager.play(
                    SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.5f)
                )
            }
        }
    }
}
