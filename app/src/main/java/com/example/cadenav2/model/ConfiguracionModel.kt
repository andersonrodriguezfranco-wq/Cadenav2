package com.example.cadenav2.model

/**
 * =====================================================================
 * BLOQUE 1: DECLARACIÓN DE LA CLASE DE DATOS DE CONFIGURACIÓN
 * =====================================================================
 * Una "data class" en Kotlin sirve exclusivamente para almacenar datos
 * (como una ficha técnica). Firestore la usa para convertir información
 * de la nube a objetos de Kotlin.
 */
data class ConfiguracionModel(

    /**
     * =====================================================================
     * BLOQUE 2: PROPIEDADES O CAMPOS DE CONFIGURACIÓN
     * =====================================================================
     */
    val id: String = "general",
    val nombreCadena: String = "Cadena septiembre 2026 - mayo 2027",
    val fechaInicio: String = "15 de septiembre de 2026",
    val fechaFin: String = "30 de mayo de 2027",
    val valorCuota: Long = 50000L,
    val quincenaActiva: Int = 1
) {
    /**
     * =====================================================================
     * BLOQUE 3: CONSTRUCTOR VACÍO (Requerido por Firebase Firestore)
     * =====================================================================
     * Necesario para que Firestore pueda instanciar la clase al descargar
     * los datos desde la nube.
     */
    constructor() : this(
        id = "general",
        nombreCadena = "Cadena septiembre 2026 - mayo 2027",
        fechaInicio = "15 de septiembre de 2026",
        fechaFin = "30 de mayo de 2027",
        valorCuota = 50000L,
        quincenaActiva = 1
    )
}