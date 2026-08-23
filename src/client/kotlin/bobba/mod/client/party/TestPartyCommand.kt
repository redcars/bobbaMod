package bobba.mod.client.party

import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object TestPartyCommand {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("testparty")
                    .then(
                        literal("join").then(
                            argument("ign", StringArgumentType.word()).executes { ctx ->
                                simulateJoin(StringArgumentType.getString(ctx, "ign"))
                                1
                            }
                        )
                    )
                    .then(
                        literal("finder").then(
                            argument("ign", StringArgumentType.word()).executes { ctx ->
                                simulateFinderJoin(StringArgumentType.getString(ctx, "ign"))
                                1
                            }
                        )
                    )
                    .then(
                        literal("kick").then(
                            argument("ign", StringArgumentType.word()).executes { ctx ->
                                PartyDetection.simulateKick(StringArgumentType.getString(ctx, "ign"))
                                1
                            }
                        )
                    )
            )
        }
    }

    private fun simulateJoin(ign: String) {
        val fake = "[MVP+] $ign joined the party."
        Minecraft.getInstance().gui.chat.addServerSystemMessage(Component.literal(fake))
        PartyDetection.handleMessage(fake)
    }

    // Party Finder joins carry no [RANK] text, matching how Hypixel formats the real message.
    private fun simulateFinderJoin(ign: String) {
        val fake = Component.literal(
            "§dParty Finder §f> §b$ign §ejoined the dungeon group! (§bArcher Level 9§e)"
        )
        Minecraft.getInstance().gui.chat.addServerSystemMessage(fake)
        PartyDetection.handleMessage(fake)
    }
}
