package bobba.mod.client.config

import bobba.mod.client.update.ConfigEditorVersionStatus
import bobba.mod.client.update.GuiOptionEditorVersionStatus
import io.github.notenoughupdates.moulconfig.managed.ManagedConfig
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import java.nio.file.Files

object ConfigManager {
    private lateinit var managed: ManagedConfig<BobbaConfig>

    val instance: BobbaConfig
        get() = managed.instance

    fun init() {
        val dir = FabricLoader.getInstance().configDir.resolve("bobbamod")
        Files.createDirectories(dir)
        val file = dir.resolve("config.json").toFile()
        managed = ManagedConfig.create(file, BobbaConfig::class.java) {
            customProcessor<ConfigEditorVersionStatus> { option, _ ->
                GuiOptionEditorVersionStatus(option)
            }
        }
    }

    fun openScreen() {
        Minecraft.getInstance().execute { managed.openConfigGui() }
    }
}
