package com.example.cadenav2.model

/**
 * =====================================================================
 * BLOQUE 1: DECLARACIÓN DE LA CLASE DE DATOS DE PUESTO
 * =====================================================================
 * Representa cada uno de los 18 puestos de la cadena familiar en
 * Cloud Firestore, alineado con el respaldo (participantes, puestos
 * comprados/asignados, cuotas y estado en la quincena activa).
 * Todos los campos tienen valor por defecto para que Firestore pueda
 * deserializar los documentos sin necesitar un constructor secundario.
 */
data class PuestoModel(
    // ID del documento en Firestore (ej. "puesto_1")
    val id: String = "",

    // Número de turno o puesto en la cadena (del 1 al 18)
    val numeroPuesto: Int = 0,

    // ID del participante en el respaldo (del 1 al 11)
    val participanteId: Int = 0,

    // Nombre del titular dueño del puesto (ej. "ANDERSON", "ERIKA")
    val titular: String = "",

    // Cantidad total de puestos comprados por este participante
    val puestosComprados: Int = 1,

    // Texto con los números de puesto asignados al titular (ej. "3,11")
    val puestosAsignadosTexto: String = "",

    // Valor individual de cada puesto en pesos (ej. 50000)
    val valorCuotaIndividual: Int = 50000,

    // Aporte total quincenal del titular (puestosComprados * valorCuotaIndividual)
    val aporteTotalQuincenal: Int = 50000,

    // Fecha de cobro asignada para este número de puesto
    val fechaCobro: String = "",

    // Indica si el titular ya pagó la quincena activa
    val pagadoQuincenaActual: Boolean = false
)