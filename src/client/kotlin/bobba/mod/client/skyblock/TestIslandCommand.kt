package bobba.mod.client.skyblock

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

/**
 * Dev helper: prints what [SkyblockLocation] currently sees so scoreboard/tab parsing can be
 * verified against a live Hypixel server. Registered only in the development environment.
 */
object TestIslandCommand {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("testisland").executes { ctx ->
                    val mc = Minecraft.getInstance()
                    val tabArea = SkyblockLocation.readAreaFromTabList(mc)
                    val scoreboard = SkyblockLocation.readScoreboardText(mc)?.replace("\n", " | ")
                    ctx.source.sendFeedback(
                        Component.literal("Island: ").withStyle(ChatFormatting.GOLD)
                            .append(
                                Component.literal(SkyblockLocation.current?.displayName ?: "none")
                                    .withStyle(ChatFormatting.AQUA)
                            )
                    )
                    ctx.source.sendFeedback(
                        Component.literal("Tab area: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(tabArea ?: "—").withStyle(ChatFormatting.WHITE))
                    )
                    ctx.source.sendFeedback(
                        Component.literal("Scoreboard: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(scoreboard ?: "—").withStyle(ChatFormatting.WHITE))
                    )
                    1
                }
            )
        }
    }
}
