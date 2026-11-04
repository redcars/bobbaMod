package bobba.mod.client.media.random

import java.util.UUID

enum class Trigger {
    /** Plays after a random wait between [RandomClip.minMinutes] and [RandomClip.maxMinutes]. */
    INTERVAL,

    /** Every [RandomClip.periodSeconds], plays with [RandomClip.chancePercent] percent chance. */
    CHANCE,
}

/** An audio or video file that plays on its own at random times while in a world. */
data class RandomClip(
    val id: String = UUID.randomUUID().toString(),
    /** Stored name in the media library. */
    val file: String,
    var enabled: Boolean = true,
    var trigger: Trigger = Trigger.INTERVAL,
    var minMinutes: Double = 10.0,
    var maxMinutes: Double = 30.0,
    var chancePercent: Double = 1.0,
    var periodSeconds: Double = 60.0,
    var volume: Double = 1.0,
    /** Video width as a fraction of the screen width; ignored for audio. */
    var size: Double = 0.5,
) {
    /** Average wait between plays, in seconds, ignoring cooldown and clips blocking each other. */
    val averageWaitSeconds: Double
        get() = when (trigger) {
            Trigger.INTERVAL -> (minMinutes + maxMinutes) / 2 * 60
            Trigger.CHANCE -> periodSeconds / (chancePercent / 100)
        }
}
