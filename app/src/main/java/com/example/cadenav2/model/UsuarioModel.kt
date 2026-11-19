package com.example.cadenav2.model

/**
 * =====================================================================
 * BLOQUE 1: DECLARACIÓN DE LA CLASE DE DATOS DE USUARIO
 * =====================================================================
 * Esta clase representa a cada miembro de la familia que interactúa
 * con la aplicación. Almacena su identidad única (UID), número de
 * teléfono, nombre y el rol que desempeña dentro de la cadena.
 */
data class UsuarioModels(

    /**
     * =====================================================================
     * BLOQUE 2: PROPIEDADES O CAMPOS DEL USUARIO
     * =====================================================================
     */

    // Identificador único del usuario (coincide con el UID generado por Firebase Auth al iniciar sesión).
    val uid: String = "",

    // Número de teléfono verificado mediante SMS (ej: "+573001234567").
    val telefono: String = "",

    // Nombre completo del familiar (ej: "Anderson", "Jeidy", "Tatis").
    val nombreCompleto: String = "",

    // Rol dentro de la aplicación.
    // Puede ser "ADMIN" (administrador general que controla todo) o "MIEMBRO" (usuario estándar).
    val rol: String = "MIEMBRO"
) {
    /**
     * =====================================================================
     * BLOQUE 3: CONSTRUCTOR VACÍO (Requerido por Firebase Firestore)
     * =====================================================================
     * Al igual que en la configuración, Firebase necesita este constructor
     * sin parámetros para poder instanciar el objeto al descargar los
     * datos de los usuarios desde la nube.
     */
    constructor() : this(
        uid = "",
        telefono = "",
        nombreCompleto = "",
        rol = "MIEMBRO"
    )
}