package bobba.mod.client.config

import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback

object ConfigCommand {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("bobba")
                    .executes {
                        ConfigManager.openScreen()
                        1
                    }
                    .then(literal("overlays").executes {
                        ConfigManager.openScreen("overlays")
                        1
                    })
                    .then(literal("kuudra").executes {
                        ConfigManager.openScreen("kuudra")
                        1
                    })
                    .then(literal("media").executes {
                        ConfigManager.openScreen("randomMedia")
                        1
                    })
            )
        }
    }
}
