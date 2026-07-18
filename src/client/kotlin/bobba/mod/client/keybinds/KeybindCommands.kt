package bobba.mod.client.keybinds

import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object KeybindCommands {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("keybinds")
                    .then(
                        literal("add").then(
                            argument("key", StringArgumentType.word()).then(
                                argument("command", StringArgumentType.greedyString()).executes { ctx ->
                                    handleAdd(
                                        ctx.source,
                                        StringArgumentType.getString(ctx, "key"),
                                        StringArgumentType.getString(ctx, "command")
                                    )
                                    1
                                }
                            )
                        )
                    )
                    .then(
                        literal("remove").then(
                            argument("key", StringArgumentType.word()).executes { ctx ->
                                handleRemove(ctx.source, StringArgumentType.getString(ctx, "key"))
                                1
                            }
                        )
                    )
                    .then(
                        literal("toggle").then(
                            argument("key", StringArgumentType.word()).executes { ctx ->
                                handleToggle(ctx.source, StringArgumentType.getString(ctx, "key"))
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

    private fun handleToggle(source: FabricClientCommandSource, key: String) {
        val code = KeyName.parse(key)
        if (code == null) {
            source.sendError(Component.literal("Unknown key '$key'.").withStyle(ChatFormatting.RED))
            return
        }
        val entries = Keybinds.all()
        var toggled = 0
        entries.forEachIndexed { i, entry ->
            if (entry.keyCode == code) {
                Keybinds.updateAt(i, entry.copy(enabled = !entry.isEnabled))
                toggled++
            }
        }
        if (toggled == 0) {
            source.sendError(
                Component.literal("No keybind found for ${KeyName.displayName(code)}.")
                    .withStyle(ChatFormatting.RED)
            )
        } else {
            source.sendFeedback(
                Component.literal("Toggled $toggled keybind(s) for ${KeyName.displayName(code)}.")
                    .withStyle(ChatFormatting.GREEN)
            )
        }
    }

    private fun handleAdd(source: FabricClientCommandSource, key: String, command: String) {
        val code = KeyName.parse(key)
        if (code == null) {
            source.sendError(
                Component.literal("Unknown key '$key'. Examples: h, f5, space, shift.")
                    .withStyle(ChatFormatting.RED)
            )
            return
        }
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            source.sendError(Component.literal("Command cannot be empty.").withStyle(ChatFormatting.RED))
            return
        }
        Keybinds.add(KeybindEntry(code, trimmed))
        source.sendFeedback(
            Component.literal("Bound ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(KeyName.displayName(code)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" → ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(trimmed).withStyle(ChatFormatting.WHITE))
        )
    }

    private fun handleRemove(source: FabricClientCommandSource, key: String) {
        val code = KeyName.parse(key)
        if (code == null) {
            source.sendError(
                Component.literal("Unknown key '$key'.").withStyle(ChatFormatting.RED)
            )
            return
        }
        val removed = Keybinds.removeByKeyCode(code)
        if (removed == 0) {
            source.sendError(
                Component.literal("No keybind found for ${KeyName.displayName(code)}.")
                    .withStyle(ChatFormatting.RED)
            )
        } else {
            source.sendFeedback(
                Component.literal("Removed $removed keybind(s) for ${KeyName.displayName(code)}.")
                    .withStyle(ChatFormatting.GREEN)
            )
        }
    }

    private fun handleList(source: FabricClientCommandSource) {
        val all = Keybinds.all()
        if (all.isEmpty()) {
            source.sendFeedback(Component.literal("No keybinds set.").withStyle(ChatFormatting.GRAY))
            return
        }
        source.sendFeedback(Component.literal("Keybinds (${all.size}):").withStyle(ChatFormatting.GOLD))
        all.forEach { entry ->
            val statusTag = if (entry.isEnabled) {
                Component.literal("[ON] ").withStyle(ChatFormatting.GREEN)
            } else {
                Component.literal("[OFF] ").withStyle(ChatFormatting.RED)
            }
            source.sendFeedback(
                Component.literal(" - ").withStyle(ChatFormatting.WHITE)
                    .append(statusTag)
                    .append(Component.literal(KeyName.displayName(entry.keyCode)).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" → ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(entry.command).withStyle(ChatFormatting.WHITE))
            )
        }
    }
}
