package io.github.torrenkt.tslmwebui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.bindToBrowserNavigation
import io.github.torrenkt.tslmwebui.view.App
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

@OptIn(ExperimentalBrowserHistoryApi::class, ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    ComposeViewport {
        val fsc by preloadFont(Res.font.NotoSerifSC_Regular)
        val ftc by preloadFont(Res.font.NotoSerifJP_Regular)
        val fontFamilyResolver = LocalFontFamilyResolver.current
        var fontLoaded by remember { mutableStateOf(false) }

        MaterialTheme {
            if (fontLoaded) {
                App(
                    onNavHostReady = {
                        it.bindToBrowserNavigation()
                    },
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }

            LaunchedEffect(fsc, ftc) {
                val fonts = listOfNotNull(fsc, ftc)
                if (fonts.size >= 2) {
                    val fontFamily = FontFamily(fonts)
                    fontFamilyResolver.preload(fontFamily)
                    fontLoaded = true
                }
            }
        }
    }
}
