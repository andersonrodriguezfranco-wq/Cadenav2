package com.example.cadenav2.model

/**
 * =====================================================================
 * MODELO DE DATOS: PagoModel
 * =====================================================================
 * Representa un pago o aporte quincenal registrado en Cloud Firestore.
 * Incluye valores por defecto en todos los campos para que Firestore
 * pueda deserializar los documentos sin necesitar constructores extra.
 */
data class PagoModel(
    // Identificador único del documento de pago en Firestore (ej: "pago_p1_q5_0")
    val id: String = "",

    // ID del participante en el respaldo (ej: 1 al 11 según
    val puestoId: Int = 0,

    // ID del documento del puesto en Firestore (ej: "puesto_1")
    val puestoDocId: String = "",

    // Número del puesto (del 1 al 18) para referencia rápida
    val numeroPuesto: Int = 0,

    // Nombre del titular que realizó el pago (ej: "ANDERSON", "ERIKA")
    val titular: String = "",

    // Número de la quincena pagada (ej: 3, 4, 5...)
    val quincena: Int = 0,

    // Monto pagado en pesos (ej: 50000, 100000 o 150000)
    val monto: Int = 0,

    // Fecha y hora del registro del pago
    val fecha: String = "",

    // UID del usuario o administrador que registró el pago (opcional)
    val registradoPorUid: String = ""
)