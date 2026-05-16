package br.com.fabriciolima.momentus.ui.util

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.OrientationEventListener
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.launch

/**
 * Utilitário para padronizar o comportamento de rotação e adaptabilidade.
 * Em celulares: Trava em modo retrato e mostra aviso se inclinado.
 * Em tablets: Permite rotação livre.
 */
@Composable
fun AdaptiveOrientationWrapper(
    windowSizeClass: WindowSizeClass,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val isMedium = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium
    val isTablet = isExpanded || isMedium

    DisposableEffect(isTablet) {
        val activity = context as? Activity
        var listener: OrientationEventListener? = null

        if (!isTablet && !view.isInEditMode) {
            // Trava em retrato para celulares
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            listener = object : OrientationEventListener(context) {
                override fun onOrientationChanged(orientation: Int) {
                    if (orientation == ORIENTATION_UNKNOWN) return
                    // Detecta se o usuário está tentando usar o celular na horizontal
                    val isTiltedHorizontal = (orientation in 70..110) || (orientation in 250..290)
                    if (isTiltedHorizontal && snackbarHostState != null) {
                        scope.launch {
                            if (snackbarHostState.currentSnackbarData == null) {
                                snackbarHostState.showSnackbar(
                                    message = "Momentus: Otimizado para o modo retrato no seu celular! ✨",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                }
            }
            listener.enable()
        }

        onDispose {
            listener?.disable()
            // Libera a orientação ao sair da tela (opcional, dependendo da navegação)
            if (!isTablet && !view.isInEditMode) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    content()
}
