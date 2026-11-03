package bobba.mod.client.config

import bobba.mod.client.overlay.gui.OverlayManagerScreen
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
                        OverlayManagerScreen.open()
                        1
                    })
            )
        }
    }
}
