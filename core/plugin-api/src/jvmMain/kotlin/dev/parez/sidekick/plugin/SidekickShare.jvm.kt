package dev.parez.sidekick.plugin

import java.awt.FileDialog
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
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
            //
            // createTempFile rather than a predictable path in the shared temp
            // directory: a fixed name is guessable, and writing to it would follow a
            // pre-existing symlink and clobber its target (CWE-377). The randomised
            // file is created exclusively, and owner-only where the filesystem
            // supports POSIX permissions.
            runCatching {
                val file =
                    File.createTempFile("${subject.toSafeFileName()}-", ".txt").apply {
                        deleteOnExit()
                    }
                runCatching {
                    Files.setPosixFilePermissions(
                        file.toPath(),
                        PosixFilePermissions.fromString("rw-------"),
                    )
                }
                file.writeText(text)
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
                        ownerFrame(),
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
            FileDialog(ownerFrame(), "Export $subject", FileDialog.SAVE).apply {
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

    /**
     * The app's own window.
     *
     * Passing `null` here lets AWT own the dialog with a hidden shared frame, which on macOS opens
     * it *behind* whatever else is on screen — the dialog exists, blocks the app, and the user
     * never sees it. Owning it from the real window keeps it on top of the app and moves with it.
     */
    private fun ownerFrame(): Frame? =
        Frame.getFrames().firstOrNull { it.isVisible && it.isShowing }

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
