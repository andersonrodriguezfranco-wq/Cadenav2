package com.example.cadenav2.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Familia base limpia y moderna para toda la interfaz
private val FuenteModerna = FontFamily.SansSerif

// Escala tipográfica moderna pensada para lectura rápida en listas, tarjetas y montos
val Typography = Typography(
    // Montos principales destacados (ej. total recaudado) con cifras alineadas
    headlineLarge = TextStyle(
        fontFamily = FuenteModerna,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum"
    ),
    // Nombre de quien recibe la quincena en la tarjeta superior
    headlineMedium = TextStyle(
        fontFamily = FuenteModerna,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    // Títulos de pantalla, diálogos y encabezados importantes
    titleLarge = TextStyle(
        fontFamily = FuenteModerna,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.2).sp
    ),
    // Nombres de miembros en filas y subtítulos de tarjetas
    titleMedium = TextStyle(
        fontFamily = FuenteModerna,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    // Texto principal de tarjetas, switches y diálogos
    bodyLarge = TextStyle(
        fontFamily = FuenteModerna,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.1.sp
    ),
    // Detalles secundarios, montos por fila y contadores
    bodyMedium = TextStyle(
        fontFamily = FuenteModerna,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp,
        fontFeatureSettings = "tnum"
    ),
    // Mensajes de estado y notas cortas
    bodySmall = TextStyle(
        fontFamily = FuenteModerna,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    // Encabezados de sección en mayúsculas (ej. PENDIENTES)
    labelLarge = TextStyle(
        fontFamily = FuenteModerna,
        fontWeight = FontWeight.Bold,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp
    ),
    // Etiquetas superiores de tarjeta (ej. RECIBE ESTA QUINCENA, RECAUDADO)
    labelMedium = TextStyle(
        fontFamily = FuenteModerna,
        fontWeight = FontWeight.Bold,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.9.sp
    ),
    // Títulos de sección en menú lateral y etiquetas compactas
    labelSmall = TextStyle(
        fontFamily = FuenteModerna,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.7.sp
    )
)