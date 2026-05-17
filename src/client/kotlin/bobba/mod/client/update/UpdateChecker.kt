package bobba.mod.client.update

import bobba.mod.client.config.ConfigManager
import com.google.gson.JsonParser
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

object UpdateChecker {
    private val logger = LoggerFactory.getLogger("bobbamod/update")

    private const val REPO_OWNER = "redcars"
    private const val REPO_NAME = "bobbaMod"

    enum class State { IDLE, CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, ERROR }

    @Volatile
    var state: State = State.IDLE
        private set

    @Volatile
    var latestVersion: String? = null
        private set

    @Volatile
    var latestUrl: String? = null
        private set

    @Volatile
    var lastError: String? = null
        private set

    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    private val hasChecked = AtomicBoolean(false)

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            if (hasChecked.getAndSet(true)) return@register
            if (!ConfigManager.instance.about.checkForUpdates) return@register
            checkAsync(silentIfNoUpdate = true)
        }
    }

    fun forceCheck() {
        hasChecked.set(true)
        if (state == State.CHECKING) return
        checkAsync(silentIfNoUpdate = false)
    }

    fun currentVersion(): String = FabricLoader.getInstance()
        .getModContainer("bobbamod")
        .map { it.metadata.version.friendlyString }
        .orElse("0.0.0")

    fun openLatest() {
        val latest = latestVersion ?: return
        val url = latestUrl ?: return
        notifyUpdateAvailable(currentVersion(), latest, url)
    }

    private fun checkAsync(silentIfNoUpdate: Boolean) {
        state = State.CHECKING
        lastError = null
        val current = currentVersion()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "BobbaMod (+https://github.com/redcars/bobbaMod)")
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build()

        http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept { resp ->
                when (resp.statusCode()) {
                    200 -> handleResponse(resp.body(), current, silentIfNoUpdate)
                    404 -> {
                        state = State.ERROR
                        lastError = "No releases yet"
                        if (!silentIfNoUpdate) notify("No releases published yet.", ChatFormatting.YELLOW)
                    }
                    else -> {
                        state = State.ERROR
                        lastError = "HTTP ${resp.statusCode()}"
                        logger.warn("GitHub releases returned {}", resp.statusCode())
                        if (!silentIfNoUpdate) notify("GitHub API returned ${resp.statusCode()}.", ChatFormatting.RED)
                    }
                }
            }
            .exceptionally { e ->
                state = State.ERROR
                lastError = e.message ?: "unknown error"
                logger.warn("Update check failed: {}", e.message)
                if (!silentIfNoUpdate) notify("Update check failed: ${e.message}", ChatFormatting.RED)
                null
            }
    }

    private fun handleResponse(body: String, current: String, silentIfNoUpdate: Boolean) {
        val json = JsonParser.parseString(body).asJsonObject
        val tag = json.get("tag_name")?.asString
        val htmlUrl = json.get("html_url")?.asString
        if (tag == null || htmlUrl == null) {
            state = State.ERROR
            lastError = "Malformed response"
            if (!silentIfNoUpdate) notify("Malformed GitHub response.", ChatFormatting.RED)
            return
        }
        val latest = tag.removePrefix("v")
        latestVersion = latest
        latestUrl = htmlUrl

        if (isNewer(latest, current)) {
            state = State.UPDATE_AVAILABLE
            notifyUpdateAvailable(current, latest, htmlUrl)
        } else {
            state = State.UP_TO_DATE
            if (!silentIfNoUpdate) notify("Up to date (v$current).", ChatFormatting.GREEN)
        }
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val l = parseVersion(latest)
        val c = parseVersion(current)
        for (i in 0 until maxOf(l.size, c.size)) {
            val li = l.getOrElse(i) { 0 }
            val ci = c.getOrElse(i) { 0 }
            if (li > ci) return true
            if (li < ci) return false
        }
        return false
    }

    private fun parseVersion(v: String): List<Int> {
        val core = v.substringBefore('-').substringBefore('+')
        return core.split('.').mapNotNull { it.toIntOrNull() }
    }

    private fun notifyUpdateAvailable(current: String, latest: String, url: String) {
        Minecraft.getInstance().execute {
            val chat = Minecraft.getInstance().gui.chat
            chat.addMessage(
                Component.literal("[BobbaMod] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("Update available: ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("v$current → v$latest").withStyle(ChatFormatting.AQUA))
            )
            chat.addMessage(
                Component.literal("[BobbaMod] ").withStyle(ChatFormatting.GOLD)
                    .append(
                        Component.literal("[Click here to open the release page]").withStyle(
                            Style.EMPTY
                                .withColor(ChatFormatting.GREEN)
                                .withUnderlined(true)
                                .withClickEvent(ClickEvent.OpenUrl(URI.create(url)))
                        )
                    )
            )
        }
    }

    private fun notify(text: String, color: ChatFormatting) {
        Minecraft.getInstance().execute {
            Minecraft.getInstance().gui.chat.addMessage(
                Component.literal("[BobbaMod] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(text).withStyle(color))
            )
        }
    }
}
