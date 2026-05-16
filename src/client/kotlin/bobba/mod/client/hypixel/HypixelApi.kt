package bobba.mod.client.hypixel

import bobba.mod.client.config.ConfigManager
import com.google.gson.JsonParser
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture

object HypixelApi {
    private val logger = LoggerFactory.getLogger("bobbamod/hypixel")

    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    fun resolveRank(uuid: UUID): CompletableFuture<HypixelRank?> {
        val apiKey = ConfigManager.instance.api.hypixelApiKey.trim()
        if (apiKey.isEmpty()) return CompletableFuture.completedFuture(null)

        val undashed = uuid.toString().replace("-", "")
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.hypixel.net/v2/player?uuid=$undashed"))
            .header("API-Key", apiKey)
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build()

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply { resp ->
                if (resp.statusCode() != 200) {
                    logger.warn("Hypixel API returned {} for {}", resp.statusCode(), uuid)
                    return@thenApply null
                }
                val json = JsonParser.parseString(resp.body()).asJsonObject
                if (!json.get("success").asBoolean) return@thenApply null
                val player = json.get("player")
                if (player == null || player.isJsonNull) HypixelRank.NONE
                else HypixelRank.parseFromPlayer(player.asJsonObject)
            }
            .exceptionally { e ->
                logger.warn("Hypixel API call failed for {}: {}", uuid, e.message)
                null
            }
    }
}
