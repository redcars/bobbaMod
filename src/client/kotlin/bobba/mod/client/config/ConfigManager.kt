package bobba.mod.client.config

import bobba.mod.client.config.editor.ConfigEditorKuudraFace
import bobba.mod.client.config.editor.ConfigEditorOverlayList
import bobba.mod.client.config.editor.ConfigEditorRandomClips
import bobba.mod.client.config.editor.ConfigEditorScoreboardBackground
import bobba.mod.client.config.editor.ConfigEditorScoreboardLines
import bobba.mod.client.config.editor.GuiOptionEditorKuudraFace
import bobba.mod.client.config.editor.GuiOptionEditorOverlayList
import bobba.mod.client.config.editor.GuiOptionEditorRandomClips
import bobba.mod.client.config.editor.GuiOptionEditorScoreboardBackground
import bobba.mod.client.config.editor.GuiOptionEditorScoreboardLines
import bobba.mod.client.update.ConfigEditorVersionStatus
import bobba.mod.client.update.GuiOptionEditorVersionStatus
import io.github.notenoughupdates.moulconfig.common.IMinecraft
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
            customProcessor<ConfigEditorOverlayList> { option, _ ->
                GuiOptionEditorOverlayList(option)
            }
            customProcessor<ConfigEditorKuudraFace> { option, _ ->
                GuiOptionEditorKuudraFace(option)
            }
            customProcessor<ConfigEditorRandomClips> { option, _ ->
                GuiOptionEditorRandomClips(option)
            }
            customProcessor<ConfigEditorScoreboardBackground> { option, _ ->
                GuiOptionEditorScoreboardBackground(option)
            }
            customProcessor<ConfigEditorScoreboardLines> { option, _ ->
                GuiOptionEditorScoreboardLines(option)
            }
        }
    }

    /** For settings changed outside the config GUI, which saves its own edits. */
    fun save() = managed.saveToFile()

    /** Opens the config GUI, on the tab whose field in [BobbaConfig] is [category] if given. */
    fun openScreen(category: String? = null) {
        Minecraft.getInstance().execute {
            // getEditor() builds a fresh editor each call, so select the tab on the one we open.
            val editor = managed.getEditor()
            if (category != null) editor.allCategories[category]?.let { editor.setSelectedCategory(it) }
            IMinecraft.INSTANCE.openWrappedScreen(editor)
        }
    }
}
