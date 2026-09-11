package dev.parez.sidekick.plugin

import java.awt.Desktop
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

public actual object SidekickShare {
    /**
     * Desktop has no share sheet. Writes the payload to a temp file and opens it in the default
     * text handler; if that isn't available (headless CI, a stripped desktop environment), falls
     * back to putting the text on the clipboard so the data is still recoverable.
     */
    public actual fun share(text: String, subject: String) {
        val file = File(System.getProperty("java.io.tmpdir"), "${subject.toSafeFileName()}.txt")
        val opened =
            runCatching {
                    file.writeText(text)
                    val desktop =
                        if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()) {
                            Desktop.getDesktop()
                        } else {
                            null
                        }
                    if (desktop?.isSupported(Desktop.Action.OPEN) == true) {
                        desktop.open(file)
                        true
                    } else {
                        false
                    }
                }
                .getOrDefault(false)

        if (!opened && !GraphicsEnvironment.isHeadless()) {
            runCatching {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
            }
        }
    }
}
