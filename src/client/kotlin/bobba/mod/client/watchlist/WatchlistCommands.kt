package bobba.mod.client.watchlist

import bobba.mod.client.hypixel.HypixelApi
import bobba.mod.client.hypixel.HypixelRank
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
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
                        literal("note").then(
                            argument("ign", StringArgumentType.word()).then(
                                argument("text", StringArgumentType.greedyString()).executes { ctx ->
                                    handleNote(
                                        ctx.source,
                                        StringArgumentType.getString(ctx, "ign"),
                                        StringArgumentType.getString(ctx, "text")
                                    )
                                    1
                                }
                            )
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

    private fun handleNote(source: FabricClientCommandSource, ign: String, text: String) {
        val entry = Watchlist.getByIgn(ign)
        if (entry == null) {
            source.sendError(Component.literal("$ign is not on your watchlist.").withStyle(ChatFormatting.RED))
            return
        }
        Watchlist.setNote(ign, text)
        source.sendFeedback(
            Component.literal("Note set for ${entry.ign}: ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(text.trim()).withStyle(ChatFormatting.WHITE))
        )
    }

private fun handleAdd(source: FabricClientCommandSource, ign: String) {
        if (!Watchlist.add(WatchlistEntry(ign = ign))) {
            source.sendError(Component.literal("$ign is already on your watchlist.").withStyle(ChatFormatting.RED))
            return
        }
        source.sendFeedback(Component.literal("Added $ign to your watchlist.").withStyle(ChatFormatting.GREEN))

        MojangApi.resolveProfile(ign).thenAccept { profile ->
            if (profile == null) return@thenAccept
            Watchlist.attachProfile(ign, profile.uuid, profile.name)
            HypixelApi.resolveRank(profile.uuid).thenAccept { rank ->
                if (rank != null) Watchlist.attachRank(profile.uuid, rank)
            }
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
            val rank = entry.rank ?: HypixelRank.NONE
            val prefix = if (rank.prefix.isNotEmpty()) "${rank.prefix} " else ""
            val nameComponent = Component.literal("$prefix${entry.ign}").withStyle(rank.color)
            val line = Component.literal(" - ").withStyle(ChatFormatting.WHITE).append(nameComponent)
            if (entry.uuid == null) {
                line.append(Component.literal(" (pending)").withStyle(ChatFormatting.GRAY))
            }
            entry.note?.takeIf { it.isNotBlank() }?.let { note ->
                line.append(Component.literal(" — $note").withStyle(ChatFormatting.ITALIC))
            }
            source.sendFeedback(line)
        }
    }
}
