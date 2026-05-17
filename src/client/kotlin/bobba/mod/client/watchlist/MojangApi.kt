package bobba.mod.client.watchlist

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture

object MojangApi {
    private val logger = LoggerFactory.getLogger("bobbamod/mojang")
    private val gson = Gson()

    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    private data class ProfileResponse(val id: String?, val name: String?)

    data class MojangProfile(val uuid: UUID, val name: String)

    fun resolveProfile(ign: String): CompletableFuture<MojangProfile?> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/$ign"))
            .header("User-Agent", "BobbaMod (+https://github.com/redcars/bobbaMod)")
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build()
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply { resp ->
                if (resp.statusCode() != 200) return@thenApply null
                val parsed = gson.fromJson(resp.body(), ProfileResponse::class.java) ?: return@thenApply null
                val uuid = parsed.id?.let(::dashUuid) ?: return@thenApply null
                val name = parsed.name ?: return@thenApply null
                MojangProfile(uuid, name)
            }
            .exceptionally { e ->
                logger.warn("Failed to resolve profile for {}: {}", ign, e.message)
                null
            }
    }

    fun resolveCurrentName(uuid: UUID): CompletableFuture<String?> {
        val undashed = uuid.toString().replace("-", "")
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/$undashed"))
            .header("User-Agent", "BobbaMod (+https://github.com/redcars/bobbaMod)")
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build()
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply { resp ->
                if (resp.statusCode() != 200) return@thenApply null
                gson.fromJson(resp.body(), ProfileResponse::class.java)?.name
            }
            .exceptionally { e ->
                logger.warn("Failed to resolve name for {}: {}", uuid, e.message)
                null
            }
    }

    private fun dashUuid(undashed: String): UUID? {
        val s = undashed.replace("-", "")
        if (s.length != 32) return null
        return try {
            UUID.fromString(
                "${s.substring(0, 8)}-${s.substring(8, 12)}-${s.substring(12, 16)}-" +
                    "${s.substring(16, 20)}-${s.substring(20)}"
            )
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
