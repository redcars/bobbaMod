package bobba.mod.client.media.random

import bobba.mod.client.config.ConfigManager
import bobba.mod.client.media.MediaKind
import bobba.mod.client.media.MediaLibrary
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundSource
import java.nio.file.Path
import kotlin.random.Random

/**
 * Plays the user's audio and video clips at random times while in a world. Playback needs the
 * WATERMeDIA mod; without it clips can still be managed, they just never play. Render-thread only.
 */
object RandomMedia {
    private const val TICKS_PER_SECOND = 20

    // When a clip is due but another is playing (or cooling down), check again this soon.
    private const val RETRY_TICKS = 5 * TICKS_PER_SECOND

    private val list = mutableListOf<RandomClip>()

    val clips: List<RandomClip> get() = list

    /** Null when WATERMeDIA isn't installed. */
    val player: ClipPlayer? = if (FabricLoader.getInstance().isModLoaded("watermedia")) WaterMediaPlayer() else null

    // Ticks until each clip's next play (interval) or next roll (chance), keyed by clip id.
    private val timers = HashMap<String, Int>()
    private var cooldownTicks = 0
    private var wasBusy = false

    // The clip currently loading or playing, so removing it can stop it first.
    private var playingClipId: String? = null

    private val stopKey = KeyMapping(
        "key.bobbamod.stop_random_media",
        InputConstants.UNKNOWN.value,
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("bobbamod", "bobbamod")),
    )

    init {
        MediaLibrary.addReferences { list.map { it.file } }
    }

    fun init() {
        list += RandomClipsStorage.load()
        KeyMappingHelper.registerKeyMapping(stopKey)
        ClientTickEvents.END_CLIENT_TICK.register { tick(it) }
        // Minecraft tears down its OpenAL context on quit, under any clip still playing.
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> player?.stop() }
        ClientLifecycleEvents.CLIENT_STOPPING.register { player?.stop() }
        // On top of everything: a jumpscare under the hotbar wouldn't be much of one.
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("bobbamod", "random_media")) { graphics, _ ->
            player?.render(graphics)
        }
    }

    fun save() = RandomClipsStorage.save(list)

    fun importClips(paths: List<Path>, onDone: (added: Int, errors: List<String>) -> Unit) {
        MediaLibrary.importFiles(paths, MediaKind.CLIP) { names, errors ->
            names.forEach { list += RandomClip(file = it) }
            if (names.isNotEmpty()) save()
            onDone(names.size, errors)
        }
    }

    fun remove(clip: RandomClip) {
        if (!list.remove(clip)) return
        // A playing file is still open, so it couldn't be deleted.
        if (playingClipId == clip.id) player?.stop()
        timers.remove(clip.id)
        save()
        MediaLibrary.deleteIfUnused(clip.file)
    }

    /** Restarts [clip]'s timer, so a changed setting takes effect now rather than after the old wait. */
    fun resetTimer(clip: RandomClip) {
        timers.remove(clip.id)
    }

    fun play(clip: RandomClip) {
        val p = player ?: return
        val master = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER)
        p.play(MediaLibrary.resolve(clip.file), clip.volume * master, clip.size)
        playingClipId = clip.id
    }

    /** Seconds until [clip] next plays (interval) or next rolls (chance), or null before its timer starts. */
    fun secondsUntilNext(clip: RandomClip): Int? = timers[clip.id]?.let { (it + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND }

    val cooldownSecondsLeft: Int get() = (cooldownTicks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND

    /** Why timers aren't counting right now, or null when they are. */
    fun pausedReason(): String? {
        val mc = Minecraft.getInstance()
        return when {
            !ConfigManager.instance.randomMedia.enabled -> "Play random media is off"
            mc.level == null -> "you're not in a world"
            mc.isPaused -> "the game is paused"
            else -> null
        }
    }

    private fun tick(mc: Minecraft) {
        val p = player ?: return
        p.tick()
        while (stopKey.consumeClick()) p.stop()

        // The cooldown starts when a clip finishes, not when it starts.
        if (wasBusy && !p.busy) cooldownTicks = ConfigManager.instance.randomMedia.cooldownSeconds * TICKS_PER_SECOND
        wasBusy = p.busy
        if (!p.busy) playingClipId = null

        if (!ConfigManager.instance.randomMedia.enabled || mc.level == null || mc.isPaused) return
        if (cooldownTicks > 0) cooldownTicks--

        for (clip in list) {
            if (!clip.enabled) continue
            val left = (timers[clip.id] ?: nextWait(clip)) - 1
            if (left > 0) {
                timers[clip.id] = left
                continue
            }
            val free = !p.busy && cooldownTicks <= 0
            when (clip.trigger) {
                Trigger.INTERVAL -> if (free) {
                    play(clip)
                    timers[clip.id] = nextWait(clip)
                } else {
                    timers[clip.id] = RETRY_TICKS
                }
                Trigger.CHANCE -> {
                    if (free && Random.nextDouble() * 100 < clip.chancePercent) play(clip)
                    timers[clip.id] = nextWait(clip)
                }
            }
        }
    }

    private fun nextWait(clip: RandomClip): Int = when (clip.trigger) {
        Trigger.INTERVAL -> {
            val minutes = if (clip.maxMinutes > clip.minMinutes) Random.nextDouble(clip.minMinutes, clip.maxMinutes) else clip.minMinutes
            (minutes * 60 * TICKS_PER_SECOND).toInt().coerceAtLeast(1)
        }
        Trigger.CHANCE -> (clip.periodSeconds * TICKS_PER_SECOND).toInt().coerceAtLeast(1)
    }
}
