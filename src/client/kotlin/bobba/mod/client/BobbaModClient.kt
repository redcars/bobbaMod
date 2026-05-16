package bobba.mod.client

import bobba.mod.client.config.ConfigCommand
import bobba.mod.client.config.ConfigManager
import bobba.mod.client.watchlist.Watchlist
import bobba.mod.client.watchlist.WatchlistCommands
import net.fabricmc.api.ClientModInitializer

object BobbaModClient : ClientModInitializer {
	override fun onInitializeClient() {
		ConfigManager.init()
		Watchlist.load()
		WatchlistCommands.register()
		ConfigCommand.register()
	}
}