package bobba.mod.client.config

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback

object ConfigCommand {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("bobba").executes {
                    ConfigManager.openScreen()
                    1
                }
            )
        }
    }
}
