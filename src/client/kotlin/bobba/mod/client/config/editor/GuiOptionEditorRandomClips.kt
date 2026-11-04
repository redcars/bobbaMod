package bobba.mod.client.config.editor

import bobba.mod.client.media.MediaDropTarget
import bobba.mod.client.media.MediaFilePicker
import bobba.mod.client.media.MediaKind
import bobba.mod.client.media.random.RandomClip
import bobba.mod.client.media.random.RandomClipRanges
import bobba.mod.client.media.random.RandomMedia
import bobba.mod.client.media.random.Trigger
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import java.nio.file.Path
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/** The random media clip list inside its tab: add, test, and per-clip trigger, volume and size. */
class GuiOptionEditorRandomClips(option: ProcessedOption) : GuiOptionEditor(option) {

    private companion object {
        const val PAD = 8
        const val LINE = 12
        const val BUTTON_WIDTH = 84
        const val BUTTON_HEIGHT = 18
        const val CONTROL_HEIGHT = 16
        const val GAP = 6
        const val HEADER = PAD + BUTTON_HEIGHT + 6 + 2 * LINE + 4
        const val ROW_HEIGHT = 3 * (CONTROL_HEIGHT + 4) + 8
        const val MODE_WIDTH = 64
        const val TOGGLE_WIDTH = 34
        const val TEST_WIDTH = 40
        const val REMOVE_WIDTH = 18

        val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mkv", "mov")
    }

    /** A 0..1 control over one clip setting, optionally on a log scale. */
    private class Slider(
        val box: Box,
        val range: ClosedFloatingPointRange<Double>,
        val log: Boolean,
        val get: () -> Double,
        val set: (Double) -> Unit,
        val label: (Double) -> String,
    ) {
        fun fraction(): Double {
            val v = get().coerceIn(range)
            return if (log) ln(v / range.start) / ln(range.endInclusive / range.start)
            else (v - range.start) / (range.endInclusive - range.start)
        }

        fun setFraction(t: Double) {
            val f = t.coerceIn(0.0, 1.0)
            set(if (log) range.start * (range.endInclusive / range.start).pow(f) else range.start + f * (range.endInclusive - range.start))
        }
    }

    private class Row(val clip: RandomClip, val top: Int, val toggle: Box, val test: Box, val remove: Box, val mode: Box, val sliders: List<Slider>)

    private var status: String? = null
    private var dragging: Pair<String, Int>? = null

    private fun addButton(x: Int, y: Int, width: Int) = Box(x + width - PAD - 2 * BUTTON_WIDTH - GAP, y + PAD, BUTTON_WIDTH, BUTTON_HEIGHT)
    private fun stopButton(x: Int, y: Int, width: Int) = Box(x + width - PAD - BUTTON_WIDTH, y + PAD, BUTTON_WIDTH, BUTTON_HEIGHT)

    private fun isVideo(clip: RandomClip) = clip.file.substringAfterLast('.').lowercase() in VIDEO_EXTENSIONS

    private fun rows(x: Int, y: Int, width: Int): List<Row> =
        RandomMedia.clips.mapIndexed { i, clip ->
            val top = y + HEADER + i * ROW_HEIGHT
            val left = x + PAD
            val right = x + width - PAD
            val line2 = top + CONTROL_HEIGHT + 4
            val line3 = line2 + CONTROL_HEIGHT + 4

            val remove = Box(right - REMOVE_WIDTH, top, REMOVE_WIDTH, CONTROL_HEIGHT)
            val test = Box(remove.x - GAP - TEST_WIDTH, top, TEST_WIDTH, CONTROL_HEIGHT)
            val toggle = Box(test.x - GAP - TOGGLE_WIDTH, top, TOGGLE_WIDTH, CONTROL_HEIGHT)

            val mode = Box(left, line2, MODE_WIDTH, CONTROL_HEIGHT)
            val half = (right - mode.right - 2 * GAP) / 2
            val a = Box(mode.right + GAP, line2, half, CONTROL_HEIGHT)
            val b = Box(a.right + GAP, line2, half, CONTROL_HEIGHT)
            val third = (right - left - 2 * GAP) / 3
            val volume = Box(left, line3, third, CONTROL_HEIGHT)
            val size = Box(volume.right + GAP, line3, third, CONTROL_HEIGHT)

            val sliders = mutableListOf<Slider>()
            when (clip.trigger) {
                Trigger.INTERVAL -> {
                    sliders += Slider(a, RandomClipRanges.MINUTES, true, { clip.minMinutes }, {
                        clip.minMinutes = roundMinutes(it)
                        if (clip.maxMinutes < clip.minMinutes) clip.maxMinutes = clip.minMinutes
                    }) { "Min ${formatMinutes(it)}" }
                    sliders += Slider(b, RandomClipRanges.MINUTES, true, { clip.maxMinutes }, {
                        clip.maxMinutes = roundMinutes(it)
                        if (clip.minMinutes > clip.maxMinutes) clip.minMinutes = clip.maxMinutes
                    }) { "Max ${formatMinutes(it)}" }
                }
                Trigger.CHANCE -> {
                    sliders += Slider(a, RandomClipRanges.CHANCE, true, { clip.chancePercent }, {
                        clip.chancePercent = roundSignificant(it)
                    }) { "Chance ${formatPercent(it)}" }
                    sliders += Slider(b, RandomClipRanges.PERIOD, true, { clip.periodSeconds }, {
                        clip.periodSeconds = it.roundToInt().toDouble()
                    }) { "Every ${formatDuration(it)}" }
                }
            }
            sliders += Slider(volume, 0.0..1.0, false, { clip.volume }, {
                clip.volume = (it * 100).roundToInt() / 100.0
            }) { "Volume ${(it * 100).roundToInt()}%" }
            if (isVideo(clip)) {
                sliders += Slider(size, RandomClipRanges.SIZE, false, { clip.size }, {
                    clip.size = (it * 100).roundToInt() / 100.0
                }) { if (it >= 1.0) "Size: full width" else "Size ${(it * 100).roundToInt()}%" }
            }
            Row(clip, top, toggle, test, remove, mode, sliders)
        }

    override fun getHeight(): Int = HEADER + RandomMedia.clips.size.coerceAtLeast(1) * ROW_HEIGHT + (if (status != null) LINE else 0) + PAD

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        MediaDropTarget.offer(::import)
        val fr = context.minecraft.defaultFontRenderer
        context.drawDarkRect(x, y, width, height, true)
        val textWidth = width - 2 * PAD

        EditorDraw.text(context, fr, "Clips", x + PAD, y + PAD + 5, EditorDraw.WHITE)
        EditorDraw.button(context, fr, addButton(x, y, width), "Add file...")
        val player = RandomMedia.player
        EditorDraw.button(context, fr, stopButton(x, y, width), "Stop playing", enabled = player?.busy == true)

        var lineY = y + PAD + BUTTON_HEIGHT + 6
        EditorDraw.text(context, fr, "Or drag ${MediaKind.CLIP.label} files onto this window.", x + PAD, lineY, EditorDraw.GRAY, textWidth)
        lineY += LINE
        val paused = RandomMedia.pausedReason()
        val cooldown = RandomMedia.cooldownSecondsLeft
        val (playerText, playerColor) = when {
            player == null -> "WATERMeDIA isn't installed: add WATERMeDIA and WATERMeDIA: Binaries from Modrinth." to EditorDraw.RED
            player.lastError != null -> "Couldn't play: ${player.lastError}" to EditorDraw.RED
            player.busy -> "Playing..." to EditorDraw.GRAY
            paused != null -> "Timers paused: $paused." to EditorDraw.GRAY
            cooldown > 0 -> "Cooldown: next clip can start in ${formatDuration(cooldown.toDouble())}." to EditorDraw.GRAY
            else -> "Ready." to EditorDraw.GRAY
        }
        EditorDraw.text(context, fr, playerText, x + PAD, lineY, playerColor, textWidth)

        val rows = rows(x, y, width)
        if (rows.isEmpty()) {
            EditorDraw.text(context, fr, "No clips yet.", x + PAD, y + HEADER + 4, EditorDraw.GRAY)
        }
        for (row in rows) {
            val clip = row.clip
            val icon = if (isVideo(clip)) "▶" else "♪"
            val nameColor = if (clip.enabled) EditorDraw.WHITE else EditorDraw.GRAY
            EditorDraw.text(context, fr, "$icon ${clip.file}", x + PAD, row.top + 4, nameColor, row.toggle.x - x - 2 * PAD)
            EditorDraw.button(context, fr, row.toggle, if (clip.enabled) "On" else "Off")
            EditorDraw.button(context, fr, row.test, "Test", enabled = player != null)
            EditorDraw.button(context, fr, row.remove, "X")
            EditorDraw.button(context, fr, row.mode, if (clip.trigger == Trigger.INTERVAL) "Interval" else "Chance")
            row.sliders.forEach { EditorDraw.slider(context, fr, it.box, it.fraction(), it.label(it.get())) }

            val last = row.sliders.last().box
            val waitX = last.right + GAP
            val next = RandomMedia.secondsUntilNext(clip)?.takeIf { clip.enabled }?.let {
                val what = if (clip.trigger == Trigger.INTERVAL) "next" else "next roll"
                " · $what in ${formatDuration(it.toDouble())}"
            } ?: ""
            EditorDraw.text(
                context, fr, "≈ once every ${formatDuration(clip.averageWaitSeconds)}$next",
                waitX, last.y + 4, EditorDraw.GRAY, x + width - PAD - waitX,
            )
        }

        status?.let { EditorDraw.text(context, fr, it, x + PAD, y + height - PAD - 10, EditorDraw.GRAY, textWidth) }
    }

    override fun mouseInput(x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int, mouseEvent: MouseEvent): Boolean {
        dragging?.let { (clipId, index) ->
            when (mouseEvent) {
                is MouseEvent.Move -> {
                    val slider = rows(x, y, width).firstOrNull { it.clip.id == clipId }?.sliders?.getOrNull(index)
                    slider?.setFraction((mouseX - slider.box.x).toDouble() / slider.box.width)
                    return true
                }
                is MouseEvent.Click -> if (!mouseEvent.mouseState) {
                    dragging = null
                    RandomMedia.clips.firstOrNull { it.id == clipId }?.let { RandomMedia.resetTimer(it) }
                    RandomMedia.save()
                    return true
                }
                else -> {}
            }
        }

        if (mouseEvent !is MouseEvent.Click || !mouseEvent.mouseState || mouseEvent.mouseButton != 0) return false

        if (addButton(x, y, width).contains(mouseX, mouseY)) {
            MediaFilePicker.pick(MediaKind.CLIP, "Choose audio or video files", "Audio and video", ::import)
            return true
        }
        if (stopButton(x, y, width).contains(mouseX, mouseY)) {
            RandomMedia.player?.stop()
            return true
        }
        for (row in rows(x, y, width)) {
            val clip = row.clip
            when {
                row.toggle.contains(mouseX, mouseY) -> {
                    clip.enabled = !clip.enabled
                    RandomMedia.resetTimer(clip)
                    RandomMedia.save()
                }
                row.test.contains(mouseX, mouseY) -> RandomMedia.play(clip)
                row.remove.contains(mouseX, mouseY) -> RandomMedia.remove(clip)
                row.mode.contains(mouseX, mouseY) -> {
                    clip.trigger = if (clip.trigger == Trigger.INTERVAL) Trigger.CHANCE else Trigger.INTERVAL
                    RandomMedia.resetTimer(clip)
                    RandomMedia.save()
                }
                else -> {
                    val index = row.sliders.indexOfFirst { it.box.contains(mouseX, mouseY) }
                    if (index < 0) continue
                    val slider = row.sliders[index]
                    dragging = clip.id to index
                    slider.setFraction((mouseX - slider.box.x).toDouble() / slider.box.width)
                }
            }
            return true
        }
        return false
    }

    private fun import(paths: List<Path>) {
        status = "Importing ${paths.size} file(s)..."
        RandomMedia.importClips(paths) { added, errors ->
            status = when {
                errors.isEmpty() -> "Added $added clip(s)."
                else -> "Added $added. Skipped: ${errors.joinToString("; ")}"
            }
        }
    }

    private fun roundMinutes(m: Double) = if (m < 10) (m * 2).roundToInt() / 2.0 else m.roundToInt().toDouble()

    // Two significant digits, so a log slider lands on 0.012% or 35% rather than 0.0123456%.
    private fun roundSignificant(v: Double): Double {
        if (v <= 0) return v
        val scale = 10.0.pow(kotlin.math.floor(kotlin.math.log10(v)) - 1)
        return (v / scale).roundToInt() * scale
    }

    private fun formatPercent(p: Double): String =
        if (p >= 10) "${p.roundToInt()}%" else "${String.format(Locale.ROOT, "%.2g", p).trimEnd('0').trimEnd('.')}%"

    private fun formatMinutes(m: Double): String =
        if (m >= 60) formatDuration(m * 60) else if (m < 10) "${String.format(Locale.ROOT, "%.1f", m).removeSuffix(".0")} min" else "${m.roundToInt()} min"

    private fun formatDuration(seconds: Double): String {
        val s = seconds.roundToInt()
        return when {
            s < 60 -> "$s s"
            s < 3600 -> "${s / 60} min" + if (s % 60 != 0 && s < 600) " ${s % 60} s" else ""
            s < 86400 -> "${s / 3600} h" + if ((s % 3600) / 60 != 0) " ${(s % 3600) / 60} min" else ""
            else -> String.format(Locale.ROOT, "%.1f days", seconds / 86400).replace(".0 ", " ")
        }
    }

    override fun fulfillsSearch(word: String): Boolean =
        super.fulfillsSearch(word) || word in "random" || word in "audio" || word in "video" || word in "mp3" || word in "mp4"
}
