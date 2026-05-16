package bobba.mod.client.notify

import bobba.mod.client.config.ConfigManager
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents

object Notifier {
    fun warn(text: String) {
        val mc = Minecraft.getInstance()
        mc.execute {
            mc.gui.chat.addMessage(
                Component.literal("[BobbaMod] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(text).withStyle(ChatFormatting.YELLOW))
            )
            if (ConfigManager.instance.watchlist.playSound) {
                mc.soundManager.play(
                    SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.5f)
                )
            }
        }
    }
}
