package dev.parez.sidekick.demo

import androidx.compose.ui.window.ComposeUIViewController

/**
 * UIViewController entry point consumed by `iosApp/iosApp/ContentView.swift`:
 * ```swift
 * struct ComposeView: UIViewControllerRepresentable {
 *     func makeUIViewController(context: Context) -> UIViewController {
 *         MainViewControllerKt.MainViewController()
 *     }
 * }
 * ```
 */
@Suppress("unused") fun MainViewController() = ComposeUIViewController { DemoApp() }
