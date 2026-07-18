package bobba.mod.client.presence

import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback

object TestPresenceCommand {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("testpresence").then(
                    argument("ign", StringArgumentType.word()).executes { ctx ->
                        ServerPresenceDetection.simulateSeen(StringArgumentType.getString(ctx, "ign"))
                        1
                    }
                )
            )
        }
    }
}
