package com.example.ainotes.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary        = Blue, // цвет фона сообщений пользователя
    onPrimary      = UltraLightGray, // цвет фона сообщений ассистента
    secondary      = UltraLightGray, // цвет фона полей ввода
    onSecondary    = PrimaryText, // цвет текста полей ввода
    tertiary       = DarkGray, //цвет значков
    onTertiary     = Blue, //цвет значков активного экрана в TopBar
    background     = White, //цвет фона
    onBackground   = LightGray, //цвет BorderStroke и drawLine
    surface        = White, //цвет DropdownMenu
    onSurface      = AccentOrange, //цвет текста очистки и удаления
    error = HoloRedDark,
    primaryContainer = White
)

private val DarkColorScheme = darkColorScheme(
    primary        = HoloBlueDark, // цвет фона сообщений пользователя
    onPrimary      = DarkGray, // цвет фона сообщений ассистента
    secondary      = DarkGray, // цвет фона полей ввода
    onSecondary    = White, // цвет текста полей ввода
    tertiary       = White, //цвет значков неактивного экрана в TopBar
    onTertiary     = Blue, //цвет значков активного экрана в TopBar
    background     = VeryDarkGray, //цвет фона
    onBackground   = PrimaryText, //цвет BorderStroke и drawLine
    surface        = DarkGray, //цвет DropdownMenu
    onSurface      = AccentOrange, //цвет текста очистки и удаления
    error = HoloRedDark,
    primaryContainer = Black
)

@Composable
fun AiNotesTheme(
    darkTheme: Boolean, // Убрано значение по умолчанию
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(),
        content = content
    )
}