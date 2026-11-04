package bobba.mod.client.scoreboard

import bobba.mod.client.config.ConfigManager
import bobba.mod.client.config.ScoreboardCategory
import bobba.mod.client.media.LoadedImage
import bobba.mod.client.media.MediaLibrary
import bobba.mod.client.media.MediaTextures
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.numbers.StyledFormat
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.PlayerScoreEntry
import net.minecraft.world.scores.PlayerTeam
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Draws the sidebar scoreboard in place of vanilla's, with lines hidden, scaled, faded and an
 * optional image behind it. Layout copies `Hud.displayScoreboardSidebar` so default settings look
 * the same as vanilla. Render thread only.
 */
object CustomScoreboard {
    /** Key of the title row, which has no line text of its own. */
    const val TITLE_KEY = "<title>"

    private const val MAX_LINES = 15
    private const val LINE_HEIGHT = 9

    // Vanilla's sidebar order.
    private val ORDER = compareByDescending<PlayerScoreEntry> { it.value }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.owner }

    private val formatting = Regex("§.")
    private val whitespace = Regex("\\s+")

    /** One sidebar row: what is drawn and the [key] it is hidden by. */
    class Line(val name: Component, val score: Component, val key: String)

    private val config: ScoreboardCategory get() = ConfigManager.instance.scoreboard

    fun init() {
        MediaLibrary.addReferences { listOfNotNull(config.backgroundImage) }
    }

    @JvmStatic
    val active: Boolean get() = config.enabled

    /** The objective vanilla would show in the sidebar right now, as in `Hud.extractScoreboardSidebar`. */
    fun currentObjective(): Objective? {
        val mc = Minecraft.getInstance()
        val scoreboard = mc.level?.scoreboard ?: return null
        val player = mc.player ?: return null
        val teamSlot = scoreboard.getPlayersTeam(player.scoreboardName)?.color?.orElse(null)?.displaySlot()
        return teamSlot?.let { scoreboard.getDisplayObjective(it) } ?: scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR)
    }

    fun lines(objective: Objective): List<Line> {
        val scoreboard = objective.scoreboard
        val format = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT)
        return scoreboard.listPlayerScores(objective)
            .asSequence()
            .filter { !it.isHidden }
            .sortedWith(ORDER)
            .take(MAX_LINES)
            .map { entry ->
                val name = PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner), entry.ownerName())
                Line(name, entry.formatValue(format), key(name.string))
            }
            .toList()
    }

    /** [text] without formatting codes or the emoji Hypixel sprinkles in to keep lines unique. */
    fun plain(text: String): String {
        val stripped = formatting.replace(text, "")
        val out = StringBuilder(stripped.length)
        stripped.codePoints().forEach { cp ->
            if (cp < 0x1F000 && cp != 0x26BD) out.appendCodePoint(cp)
        }
        return out.toString().trim()
    }

    /**
     * What a hide rule stores for a line. Words with digits become `#` and only the label before a
     * colon is kept, so "Purse: 1,234" keeps matching as the number changes.
     */
    fun key(text: String): String {
        val words = plain(text).split(whitespace).filter { it.isNotEmpty() }
        val normal = words.joinToString(" ") { word -> if (word.any(Char::isDigit)) "#" else word }
        val colon = normal.indexOf(':')
        return if (colon >= 0) normal.substring(0, colon + 1) else normal
    }

    @JvmStatic
    fun render(graphics: GuiGraphicsExtractor, objective: Objective) {
        val c = config
        val opacity = c.opacity.coerceIn(0, 100) / 100f
        if (opacity <= 0f) return
        val font = Minecraft.getInstance().font

        val hidden = c.hiddenLines.toHashSet()
        val all = lines(objective)
        val rows = all.filter { it.key !in hidden }.let { if (it.size < all.size) tidyBlanks(it) else it }
        val title = objective.displayName.takeIf { TITLE_KEY !in hidden }
        if (title == null && rows.isEmpty()) return

        val scoreWidths = rows.map { if (c.hideScoreNumbers) 0 else font.width(it.score) }
        val spacer = font.width(": ")
        var width = title?.let { font.width(it) } ?: 0
        rows.forEachIndexed { i, row ->
            width = max(width, font.width(row.name) + if (scoreWidths[i] > 0) spacer + scoreWidths[i] else 0)
        }

        val scale = c.scale.coerceIn(0.5f, 2f)
        val pose = graphics.pose()
        pose.pushMatrix()
        // Lay out in scaled space so the board keeps vanilla's right-edge, mid-height anchor.
        pose.scale(scale, scale)
        val screenWidth = (graphics.guiWidth() / scale).toInt()
        val screenHeight = (graphics.guiHeight() / scale).toInt()

        val height = rows.size * LINE_HEIGHT
        val bottom = screenHeight / 2 + height / 3
        val headerY = bottom - height
        val titleTop = headerY - LINE_HEIGHT - 1
        // The background box; vanilla pads the text by 2 on the left.
        var boxLeft = screenWidth - width - 5
        val boxRight = screenWidth - 1
        var boxTop = if (title != null) titleTop else headerY - 1
        var boxBottom = bottom

        val image = c.backgroundImage?.let { MediaTextures.get(it) }
        if (image != null && c.fitToImage) {
            val aspect = image.width.toFloat() / image.height
            val boxWidth = boxRight - boxLeft
            val boxHeight = boxBottom - boxTop
            if (boxWidth.toFloat() / boxHeight < aspect) {
                // Grow left only, so the board stays against the screen edge.
                boxLeft = boxRight - (boxHeight * aspect).roundToInt()
            } else {
                val extra = (boxWidth / aspect).roundToInt() - boxHeight
                boxTop -= extra / 2
                boxBottom += extra - extra / 2
            }
        }
        val left = boxLeft + 2
        val right = boxRight

        if (image != null) {
            val alpha = c.backgroundImageOpacity.coerceIn(0, 100) / 100f * opacity
            drawCover(graphics, image, boxLeft, boxTop, boxRight - boxLeft, boxBottom - boxTop, alpha)
        }

        val bodyColor = black(c.backgroundOpacity.coerceIn(0, 100) / 100f * opacity)
        if (title != null) {
            val bodyAlpha = c.backgroundOpacity.coerceIn(0, 100) / 100f
            // Vanilla's header is 10 points darker than the body.
            val headerAlpha = if (bodyAlpha > 0f) minOf(1f, bodyAlpha + 0.1f) else 0f
            graphics.fill(boxLeft, boxTop, boxRight, titleTop, bodyColor)
            graphics.fill(boxLeft, titleTop, boxRight, headerY - 1, black(headerAlpha * opacity))
        }
        graphics.fill(boxLeft, if (title != null) headerY - 1 else boxTop, boxRight, boxBottom, bodyColor)

        val textColor = (alphaByte(opacity) shl 24) or 0xFFFFFF
        if (title != null) {
            val textWidth = boxRight - boxLeft - 4
            graphics.text(font, title, left + textWidth / 2 - font.width(title) / 2, headerY - LINE_HEIGHT, textColor, false)
        }
        rows.forEachIndexed { i, row ->
            val y = bottom - (rows.size - i) * LINE_HEIGHT
            graphics.text(font, row.name, left, y, textColor, false)
            if (scoreWidths[i] > 0) graphics.text(font, row.score, right - scoreWidths[i], y, textColor, false)
        }
        pose.popMatrix()
    }

    /** After hiding lines, drops blank rows at the ends and merges runs of them, so no gaps are left. */
    private fun tidyBlanks(rows: List<Line>): List<Line> {
        val out = ArrayList<Line>(rows.size)
        for (row in rows) {
            val blank = row.key.isEmpty()
            if (blank && (out.isEmpty() || out.last().key.isEmpty())) continue
            out += row
        }
        while (out.isNotEmpty() && out.last().key.isEmpty()) out.removeAt(out.lastIndex)
        return out
    }

    /** Fills the box with [image], cropped to its shape instead of stretched. */
    private fun drawCover(graphics: GuiGraphicsExtractor, image: LoadedImage, x: Int, y: Int, width: Int, height: Int, alpha: Float) {
        if (width <= 0 || height <= 0 || alpha <= 0f) return
        val boxAspect = width.toFloat() / height
        val imageAspect = image.width.toFloat() / image.height
        var u = 0f
        var v = 0f
        var uWidth = image.width
        var vHeight = image.height
        if (imageAspect > boxAspect) {
            uWidth = (image.height * boxAspect).roundToInt().coerceIn(1, image.width)
            u = (image.width - uWidth) / 2f
        } else {
            vHeight = (image.width / boxAspect).roundToInt().coerceIn(1, image.height)
            v = (image.height - vHeight) / 2f
        }
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            image.frameAt(System.currentTimeMillis()),
            x, y, u, v,
            width, height,
            uWidth, vHeight,
            image.width, image.height,
            (alphaByte(alpha) shl 24) or 0xFFFFFF,
        )
    }

    private fun alphaByte(alpha: Float) = (alpha * 255).roundToInt().coerceIn(0, 255)

    private fun black(alpha: Float) = alphaByte(alpha) shl 24

    fun setBackground(name: String?) {
        val old = config.backgroundImage
        config.backgroundImage = name
        ConfigManager.save()
        if (old != null && old != name) MediaLibrary.deleteIfUnused(old)
    }

    fun setHidden(key: String, hide: Boolean) {
        val list = config.hiddenLines
        if (hide) {
            if (key !in list) list += key
        } else {
            list.remove(key)
        }
        ConfigManager.save()
    }
}
