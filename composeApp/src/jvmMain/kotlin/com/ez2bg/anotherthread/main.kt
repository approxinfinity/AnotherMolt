package com.ez2bg.anotherthread

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AnotherThread",
    ) {
        App()
    }
}