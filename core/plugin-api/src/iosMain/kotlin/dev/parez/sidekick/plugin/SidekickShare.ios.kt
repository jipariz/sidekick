package dev.parez.sidekick.plugin

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class)
actual object SidekickShare {
    actual fun share(text: String, subject: String) {
        val presenter = topMostViewController() ?: return
        val controller =
            UIActivityViewController(activityItems = listOf(text), applicationActivities = null)

        // On iPad a share sheet is a popover, and presenting one without an anchor
        // raises NSInvalidArgumentException. Anchoring to the presenting view is
        // enough; on iPhone the popover controller is nil and this is skipped.
        controller.popoverPresentationController?.let { popover ->
            popover.sourceView = presenter.view
            popover.sourceRect = presenter.view.bounds
        }

        presenter.presentViewController(controller, animated = true, completion = null)
    }

    /**
     * Walks to the view controller actually on screen. Presenting on the root controller fails when
     * something else is already presented — the normal case here, since Sidekick is itself shown
     * over the host app.
     */
    private fun topMostViewController(): UIViewController? {
        var top = keyWindow()?.rootViewController ?: return null
        while (true) {
            top = top.presentedViewController ?: return top
        }
    }

    @Suppress("DEPRECATION")
    private fun keyWindow(): UIWindow? {
        val application = UIApplication.sharedApplication
        // `keyWindow` is deprecated since iOS 13 in favour of scene-based lookup, but
        // it resolves correctly for single-scene apps and keeps this free of
        // UIWindowScene plumbing. Fall back to the first window when it is nil.
        return application.keyWindow ?: application.windows.firstOrNull() as? UIWindow
    }
}
