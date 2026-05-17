package bobba.mod.client

import bobba.mod.client.config.ConfigCommand
import bobba.mod.client.config.ConfigManager
import bobba.mod.client.party.PartyDetection
import bobba.mod.client.party.TestPartyCommand
import bobba.mod.client.presence.ServerPresenceDetection
import bobba.mod.client.presence.TestPresenceCommand
import bobba.mod.client.update.UpdateChecker
import bobba.mod.client.watchlist.Watchlist
import bobba.mod.client.watchlist.WatchlistCommands
import bobba.mod.client.watchlist.WatchlistRefresher
import net.fabricmc.api.ClientModInitializer

object BobbaModClient : ClientModInitializer {
	override fun onInitializeClient() {
		ConfigManager.init()
		Watchlist.load()
		WatchlistCommands.register()
		ConfigCommand.register()
		PartyDetection.init()
		TestPartyCommand.register()
		ServerPresenceDetection.init()
		TestPresenceCommand.register()
		WatchlistRefresher.init()
		UpdateChecker.init()
	}
}