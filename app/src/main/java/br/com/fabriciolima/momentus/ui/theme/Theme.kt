package br.com.fabriciolima.momentus.ui.theme

import android.app.Activity
import android.app.Application
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import br.com.fabriciolima.momentus.data.repository.AppThemeMode
import br.com.fabriciolima.momentus.data.repository.UserPreferencesRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.flowOf

@Composable
fun MomentusTheme(
    overrideThemeMode: AppThemeMode? = null,
    overridePrimaryColorHex: String? = null,
    overrideCornerRadiusDp: Int? = null,
    overrideFontSizeMultiplier: Float? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    
    val repository = remember(context, isInPreview) {
        if (isInPreview) null else {
            try {
                // Tenta encontrar o Application context de forma segura para o Hilt
                var currentContext = context
                while (currentContext is android.content.ContextWrapper && currentContext !is Application) {
                    currentContext = currentContext.baseContext
                }
                
                if (currentContext is Application) {
                    EntryPointAccessors.fromApplication(currentContext, ThemeEntryPoint::class.java).userPreferencesRepository()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    val preferences by (repository?.userPreferencesFlow ?: flowOf(null)).collectAsState(initial = null)
    
    // Prioriza o override (Preview) ou usa o salvo, senão usa padrão de sistema
    val themeMode = overrideThemeMode ?: preferences?.themeMode ?: AppThemeMode.SYSTEM
    val darkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val primaryColorHex = overridePrimaryColorHex ?: preferences?.primaryColorHex
    val customPrimaryColor = primaryColorHex?.let { 
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
    }

    val cornerRadius = (overrideCornerRadiusDp ?: preferences?.cornerRadiusDp ?: 12).dp
    val fontSizeMultiplier = overrideFontSizeMultiplier ?: preferences?.fontSizeMultiplier ?: 1.0f

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = customPrimaryColor ?: md_theme_dark_primary,
            onPrimary = md_theme_dark_onPrimary,
            primaryContainer = (customPrimaryColor ?: md_theme_dark_primary).copy(alpha = 0.2f),
            onPrimaryContainer = md_theme_dark_onPrimaryContainer,
            secondary = md_theme_dark_secondary,
            onSecondary = md_theme_dark_onSecondary,
            background = md_theme_dark_background,
            onBackground = md_theme_dark_onBackground,
            surface = md_theme_dark_surface,
            onSurface = md_theme_dark_onSurface,
            surfaceVariant = md_theme_dark_surfaceVariant,
            onSurfaceVariant = md_theme_dark_onSurfaceVariant,
            outline = md_theme_dark_outline,
            error = md_theme_dark_error
        )
    } else {
        lightColorScheme(
            primary = customPrimaryColor ?: md_theme_light_primary,
            onPrimary = md_theme_light_onPrimary,
            primaryContainer = (customPrimaryColor ?: md_theme_light_primary).copy(alpha = 0.1f),
            onPrimaryContainer = md_theme_light_onPrimaryContainer,
            secondary = md_theme_light_secondary,
            onSecondary = md_theme_light_onSecondary,
            background = md_theme_light_background,
            onBackground = md_theme_light_onBackground,
            surface = md_theme_light_surface,
            onSurface = md_theme_light_onSurface,
            surfaceVariant = md_theme_light_surfaceVariant,
            onSurfaceVariant = md_theme_light_onSurfaceVariant,
            outline = md_theme_light_outline,
            error = md_theme_light_error
        )
    }

    // Shapes dinâmicos baseados no arredondamento escolhido
    val shapes = Shapes(
        small = RoundedCornerShape(cornerRadius / 2),
        medium = RoundedCornerShape(cornerRadius),
        large = RoundedCornerShape(cornerRadius * 1.5f)
    )

    // Tipografia dinâmica baseada no multiplicador
    val dynamicTypography = Typography.copy(
        displayLarge = Typography.displayLarge.copy(fontSize = Typography.displayLarge.fontSize * fontSizeMultiplier),
        displayMedium = Typography.displayMedium.copy(fontSize = Typography.displayMedium.fontSize * fontSizeMultiplier),
        displaySmall = Typography.displaySmall.copy(fontSize = Typography.displaySmall.fontSize * fontSizeMultiplier),
        headlineLarge = Typography.headlineLarge.copy(fontSize = Typography.headlineLarge.fontSize * fontSizeMultiplier),
        headlineMedium = Typography.headlineMedium.copy(fontSize = Typography.headlineMedium.fontSize * fontSizeMultiplier),
        headlineSmall = Typography.headlineSmall.copy(fontSize = Typography.headlineSmall.fontSize * fontSizeMultiplier),
        titleLarge = Typography.titleLarge.copy(fontSize = Typography.titleLarge.fontSize * fontSizeMultiplier),
        titleMedium = Typography.titleMedium.copy(fontSize = Typography.titleMedium.fontSize * fontSizeMultiplier),
        titleSmall = Typography.titleSmall.copy(fontSize = Typography.titleSmall.fontSize * fontSizeMultiplier),
        bodyLarge = Typography.bodyLarge.copy(fontSize = Typography.bodyLarge.fontSize * fontSizeMultiplier),
        bodyMedium = Typography.bodyMedium.copy(fontSize = Typography.bodyMedium.fontSize * fontSizeMultiplier),
        bodySmall = Typography.bodySmall.copy(fontSize = Typography.bodySmall.fontSize * fontSizeMultiplier),
        labelLarge = Typography.labelLarge.copy(fontSize = Typography.labelLarge.fontSize * fontSizeMultiplier),
        labelMedium = Typography.labelMedium.copy(fontSize = Typography.labelMedium.fontSize * fontSizeMultiplier),
        labelSmall = Typography.labelSmall.copy(fontSize = Typography.labelSmall.fontSize * fontSizeMultiplier)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        typography = dynamicTypography,
        content = content
    )
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface ThemeEntryPoint {
    fun userPreferencesRepository(): UserPreferencesRepository
}
