package bobba.mod.client.hypixel

import com.google.gson.JsonObject
import net.minecraft.ChatFormatting

enum class HypixelRank(val prefix: String, val color: ChatFormatting) {
    NONE("", ChatFormatting.GRAY),
    VIP("[VIP]", ChatFormatting.GREEN),
    VIP_PLUS("[VIP+]", ChatFormatting.GREEN),
    MVP("[MVP]", ChatFormatting.AQUA),
    MVP_PLUS("[MVP+]", ChatFormatting.AQUA),
    MVP_PLUS_PLUS("[MVP++]", ChatFormatting.GOLD),
    YOUTUBE("[YOUTUBE]", ChatFormatting.RED),
    HELPER("[HELPER]", ChatFormatting.BLUE),
    MODERATOR("[MOD]", ChatFormatting.DARK_GREEN),
    GAME_MASTER("[GM]", ChatFormatting.DARK_GREEN),
    ADMIN("[ADMIN]", ChatFormatting.RED),
    OWNER("[OWNER]", ChatFormatting.RED);

    companion object {
        fun parseFromPlayer(player: JsonObject): HypixelRank {
            val rankStr = player.get("rank")?.takeIf { !it.isJsonNull }?.asString
            if (rankStr != null && rankStr != "NORMAL") {
                return when (rankStr) {
                    "ADMIN" -> ADMIN
                    "MODERATOR" -> MODERATOR
                    "GAME_MASTER" -> GAME_MASTER
                    "HELPER" -> HELPER
                    "YOUTUBER" -> YOUTUBE
                    "OWNER" -> OWNER
                    else -> NONE
                }
            }

            val monthly = player.get("monthlyPackageRank")?.takeIf { !it.isJsonNull }?.asString
            if (monthly == "SUPERSTAR") return MVP_PLUS_PLUS

            val pkg = player.get("newPackageRank")?.takeIf { !it.isJsonNull }?.asString
                ?: player.get("packageRank")?.takeIf { !it.isJsonNull }?.asString
            return when (pkg) {
                "VIP" -> VIP
                "VIP_PLUS" -> VIP_PLUS
                "MVP" -> MVP
                "MVP_PLUS" -> MVP_PLUS
                else -> NONE
            }
        }
    }
}
