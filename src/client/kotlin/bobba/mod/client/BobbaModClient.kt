package bobba.mod.client

import bobba.mod.client.config.ConfigCommand
import bobba.mod.client.config.ConfigManager
import bobba.mod.client.keybinds.KeybindCommands
import bobba.mod.client.keybinds.KeybindHandler
import bobba.mod.client.keybinds.Keybinds
import bobba.mod.client.party.PartyDetection
import bobba.mod.client.party.TestPartyCommand
import bobba.mod.client.presence.ServerPresenceDetection
import bobba.mod.client.presence.TestPresenceCommand
import bobba.mod.client.update.UpdateChecker
import bobba.mod.client.watchlist.Watchlist
import bobba.mod.client.watchlist.WatchlistCommands
import bobba.mod.client.watchlist.WatchlistRefresher
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader

object BobbaModClient : ClientModInitializer {
	override fun onInitializeClient() {
		ConfigManager.init()
		Watchlist.load()
		Keybinds.load()
		WatchlistCommands.register()
		ConfigCommand.register()
		KeybindCommands.register()
		PartyDetection.init()
		ServerPresenceDetection.init()
		WatchlistRefresher.init()
		UpdateChecker.init()
		KeybindHandler.init()

		if (FabricLoader.getInstance().isDevelopmentEnvironment) {
			TestPartyCommand.register()
			TestPresenceCommand.register()
		}
	}
}