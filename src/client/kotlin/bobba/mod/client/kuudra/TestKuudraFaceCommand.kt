package bobba.mod.client.kuudra

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.minecraft.network.chat.Component

/**
 * Dev helper: lets the Kuudra face show outside Kuudra, so it can be checked in singleplayer on a
 * summoned size-30 magma cube. Registered only in the development environment.
 */
object TestKuudraFaceCommand {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("testkuudraface").executes { ctx ->
                    KuudraFace.ignoreIsland = !KuudraFace.ignoreIsland
                    val text = if (KuudraFace.ignoreIsland) {
                        "Kuudra face now shows anywhere. Test boss: /summon magma_cube ~ ~ ~ {Size:29,NoAI:1b}"
                    } else {
                        "Kuudra face only shows in Kuudra again."
                    }
                    ctx.source.sendFeedback(Component.literal(text))
                    1
                }
            )
        }
    }
}
