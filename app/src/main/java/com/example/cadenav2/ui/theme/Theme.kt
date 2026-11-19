package com.example.cadenav2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Agrupación ordenada de los colores noventeros y sus colores dedicados
 * exclusivamente a textos para garantizar buena lectura en toda la app.
 */
object PaletaRetro90s {
    val fondoCrema: Color = FondoCremaNoventas
    val amarillo: Color = AmarilloRetro90s
    val naranja: Color = NaranjaVibrante90s
    val rojo: Color = RojoNoventero90s
    val vino: Color = VinoBorgona90s
    val ciruela: Color = CiruelaOscuro90s

    val textoOscuro: Color = TextoOscuroLegible
    val textoClaro: Color = TextoClaroLegible
}

private val EsquemaNoventasClaro = lightColorScheme(
    primary = VinoBorgona90s,
    onPrimary = TextoClaroLegible,
    primaryContainer = CiruelaOscuro90s,
    onPrimaryContainer = TextoClaroLegible,

    secondary = NaranjaVibrante90s,
    onSecondary = TextoOscuroLegible,
    secondaryContainer = AmarilloRetro90s,
    onSecondaryContainer = TextoOscuroLegible,

    tertiary = RojoNoventero90s,
    onTertiary = TextoClaroLegible,
    tertiaryContainer = AmarilloRetro90s,
    onTertiaryContainer = TextoOscuroLegible,

    background = FondoCremaNoventas,
    onBackground = TextoOscuroLegible,

    surface = FondoCremaNoventas,
    onSurface = TextoOscuroLegible,
    surfaceVariant = NaranjaVibrante90s,
    onSurfaceVariant = TextoOscuroLegible,

    outline = VinoBorgona90s,
    outlineVariant = CiruelaOscuro90s
)

@Composable
fun CadenaFamiliarTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EsquemaNoventasClaro,
        typography = Typography,
        content = content
    )
}