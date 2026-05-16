package bobba.mod.client.watchlist

import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object WatchlistCommands {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("watchlist")
                    .then(
                        literal("add").then(
                            argument("ign", StringArgumentType.word()).executes { ctx ->
                                handleAdd(ctx.source, StringArgumentType.getString(ctx, "ign"))
                                1
                            }
                        )
                    )
                    .then(
                        literal("remove").then(
                            argument("ign", StringArgumentType.word()).executes { ctx ->
                                handleRemove(ctx.source, StringArgumentType.getString(ctx, "ign"))
                                1
                            }
                        )
                    )
                    .then(
                        literal("list").executes { ctx ->
                            handleList(ctx.source)
                            1
                        }
                    )
            )
        }
    }

    private fun handleAdd(source: FabricClientCommandSource, ign: String) {
        if (!Watchlist.add(WatchlistEntry(ign = ign))) {
            source.sendError(Component.literal("$ign is already on your watchlist.").withStyle(ChatFormatting.RED))
            return
        }
        source.sendFeedback(Component.literal("Added $ign to your watchlist.").withStyle(ChatFormatting.GREEN))

        MojangApi.resolveUuid(ign).thenAccept { uuid ->
            if (uuid != null) Watchlist.attachUuid(ign, uuid)
        }
    }

    private fun handleRemove(source: FabricClientCommandSource, ign: String) {
        val removed = Watchlist.remove(ign)
        if (removed == null) {
            source.sendError(Component.literal("$ign is not on your watchlist.").withStyle(ChatFormatting.RED))
        } else {
            source.sendFeedback(Component.literal("Removed ${removed.ign} from your watchlist.").withStyle(ChatFormatting.GREEN))
        }
    }

    private fun handleList(source: FabricClientCommandSource) {
        val entries = Watchlist.entries
        if (entries.isEmpty()) {
            source.sendFeedback(Component.literal("Your watchlist is empty.").withStyle(ChatFormatting.GRAY))
            return
        }
        source.sendFeedback(Component.literal("Watchlist (${entries.size}):").withStyle(ChatFormatting.GOLD))
        entries.sortedBy { it.ign.lowercase() }.forEach { entry ->
            val suffix = if (entry.uuid == null) " §7(pending UUID)" else ""
            source.sendFeedback(Component.literal("§f - ${entry.ign}$suffix"))
        }
    }
}
