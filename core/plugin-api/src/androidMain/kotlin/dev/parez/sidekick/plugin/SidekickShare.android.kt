package dev.parez.sidekick.plugin

import android.content.Intent

actual object SidekickShare {
    actual fun share(text: String, subject: String) {
        if (!ApplicationContextHolder.isInitialized) return
        val context = ApplicationContextHolder.context
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
            }
        // We hold an application context, not an Activity, so the chooser needs its
        // own task to launch into.
        val chooser =
            Intent.createChooser(send, subject).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        runCatching { context.startActivity(chooser) }
    }
}
