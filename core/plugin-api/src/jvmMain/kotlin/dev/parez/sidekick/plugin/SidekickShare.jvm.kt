package dev.parez.sidekick.plugin

import java.awt.FileDialog
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

public actual object SidekickShare {

    /**
     * Desktop has no share sheet, so it asks instead: save the payload as a `.txt` somewhere of the
     * user's choosing, or put it on the clipboard.
     *
     * The earlier behaviour — write to the temp directory and hope `Desktop.open` picked a text
     * editor — gave the user no say in either the destination or the handler, and silently produced
     * nothing visible when no editor was registered.
     *
     * Uses AWT/Swing rather than a Compose dialog because this is a plain object with no
     * composition to host one, and because the save panel should be the platform's own — on macOS
     * [FileDialog] is the native one.
     */
    public actual fun share(text: String, subject: String) {
        if (GraphicsEnvironment.isHeadless()) {
            // Nothing to ask with. Still write the file so headless runs (CI, a
            // stripped desktop environment) produce something recoverable.
            runCatching {
                File(System.getProperty("java.io.tmpdir"), "${subject.toSafeFileName()}.txt")
                    .writeText(text)
            }
            return
        }
        // Modal dialogs must run on the EDT. Compose Desktop already dispatches there,
        // but share() can also be reached from a coroutine on another dispatcher.
        onSwingThread { promptAndExport(text, subject) }
    }

    private fun promptAndExport(text: String, subject: String) {
        val saveOption = "Save as .txt…"
        val clipboardOption = "Copy to clipboard"
        val choice =
            runCatching {
                    JOptionPane.showOptionDialog(
                        null,
                        "$subject — ${text.summary()}",
                        "Export",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        arrayOf(saveOption, clipboardOption, "Cancel"),
                        saveOption,
                    )
                }
                .getOrDefault(JOptionPane.CLOSED_OPTION)

        when (choice) {
            0 -> saveAsTxt(text, subject)
            1 -> copyToClipboard(text)
            else -> Unit
        }
    }

    private fun saveAsTxt(text: String, subject: String) {
        val dialog =
            FileDialog(null as Frame?, "Export $subject", FileDialog.SAVE).apply {
                file = "${subject.toSafeFileName()}.txt"
                isVisible = true
            }
        // Both are null when the user cancels the panel.
        val directory = dialog.directory ?: return
        val name = dialog.file ?: return
        runCatching { File(directory, name).writeText(text) }
    }

    private fun copyToClipboard(text: String) {
        runCatching {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        }
    }

    private fun onSwingThread(block: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeLater(block)
    }

    /** "128 lines · 6KB" — enough for the user to tell which export they just triggered. */
    private fun String.summary(): String {
        val lines = count { it == '\n' } + 1
        val bytes = encodeToByteArray().size
        val size =
            when {
                bytes < 1_024 -> "${bytes}B"
                bytes < 1_048_576 -> "${bytes / 1_024}KB"
                else -> "${bytes / 1_048_576}MB"
            }
        return "$lines ${if (lines == 1) "line" else "lines"} · $size"
    }
}
