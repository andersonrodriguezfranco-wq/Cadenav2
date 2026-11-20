package com.example.cadenav2.viewmodel

// ============================================================================
// BLOQUE 1: IMPORTACIONES Y MODELO DE DATOS DEL PUESTO
// Importa las clases de ViewModel, corrutinas, Firestore y JSON, y define la
// estructura PuestoCadena con los campos de cuota, pago y acceso por celular.
// ============================================================================

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Representa un puesto registrado dentro de la cadena familiar.
 * Contiene los datos de turno, aporte, pago en la quincena y credenciales.
 */
data class PuestoCadena(
    val id: String = "",
    val numeroPuesto: Int = 1,
    val titular: String = "",
    val puestosComprados: Int = 1,
    val valorCuotaIndividual: Int = 50000,
    val aporteTotalQuincenal: Int = 50000,
    val pagadoQuincenaActual: Boolean = false,
    val celular: String = "",
    val claveActual: String = "",
    val debeCambiarClave: Boolean = true
)

// ============================================================================
// BLOQUE 2: CLASE CADENAVIEWMODEL Y FLUJOS DE ESTADO (STATEFLOW)
// Mantiene los estados reactivos que observa la interfaz de Compose:
// puestos, quincenaActual, valorCuotaBase, cargando y mensajeEstado.
// ============================================================================

class CadenaViewModel : ViewModel() {

    // Cliente principal de Cloud Firestore para sincronizar datos en la nube
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Referencias a los listeners activos para limpiarlos al destruir el ViewModel
    private var escuchadorPuestos: ListenerRegistration? = null
    private var escuchadorConfig: ListenerRegistration? = null

    // Flujo con la lista ordenada de puestos de la cadena
    private val _puestos = MutableStateFlow<List<PuestoCadena>>(emptyList())
    val puestos: StateFlow<List<PuestoCadena>> = _puestos.asStateFlow()

    // Flujo con el número de quincena activa
    private val _quincenaActual = MutableStateFlow(1)
    val quincenaActual: StateFlow<Int> = _quincenaActual.asStateFlow()

    // Flujo con el valor base de cuota por puesto en pesos
    private val _valorCuotaBase = MutableStateFlow(50000)
    val valorCuotaBase: StateFlow<Int> = _valorCuotaBase.asStateFlow()

    // Indica si hay alguna operación en curso
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    // Mensaje de estado o error de la última acción ejecutada
    private val _mensajeEstado = MutableStateFlow("")
    val mensajeEstado: StateFlow<String> = _mensajeEstado.asStateFlow()

    init {
        // Inicia la escucha en tiempo real de configuración y puestos
        observarConfiguracion()
        observarPuestos()
    }

    override fun onCleared() {
        // Detiene los listeners cuando el ViewModel se destruye
        escuchadorPuestos?.remove()
        escuchadorConfig?.remove()
        super.onCleared()
    }

    // ========================================================================
    // BLOQUE 3: SINCRONIZACIÓN EN TIEMPO REAL CON FIRESTORE
    // Escucha el documento configuracion/general y la colección puestos para
    // mantener la pantalla sincronizada con los cambios en Firestore.
    // ========================================================================

    /**
     * Escucha cambios en configuracion/general para actualizar quincenaActual
     * y valorCuotaBase.
     */
    private fun observarConfiguracion() {
        escuchadorConfig = db.collection("configuracion")
            .document("general")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _mensajeEstado.value = "Error al leer configuracion: ${error.localizedMessage}"
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val quincenaLeida = snapshot.getLong("quincenaActual")?.toInt()
                    val cuotaLeida = snapshot.getLong("valorCuotaBase")?.toInt()

                    if (quincenaLeida != null && quincenaLeida > 0) {
                        _quincenaActual.value = quincenaLeida
                    }
                    if (cuotaLeida != null && cuotaLeida > 0) {
                        _valorCuotaBase.value = cuotaLeida
                    }
                }
            }
    }

    /**
     * Escucha cambios en la colección "puestos", convierte cada documento en
     * PuestoCadena y ordena el resultado por numeroPuesto.
     */
    private fun observarPuestos() {
        _cargando.value = true
        escuchadorPuestos = db.collection("puestos")
            .addSnapshotListener { snapshot, error ->
                _cargando.value = false

                if (error != null) {
                    _mensajeEstado.value = "Error al cargar puestos: ${error.localizedMessage}"
                    return@addSnapshotListener
                }

                val listaConvertida = snapshot?.documents?.mapNotNull { doc ->
                    val titularTexto = doc.getString("titular").orEmpty()
                    val numero = doc.getLong("numeroPuesto")?.toInt() ?: 1
                    val comprados = doc.getLong("puestosComprados")?.toInt() ?: 1
                    val cuotaIndividual = doc.getLong("valorCuotaIndividual")?.toInt()
                        ?: _valorCuotaBase.value
                    val aporteTotal = doc.getLong("aporteTotalQuincenal")?.toInt()
                        ?: (comprados * cuotaIndividual)
                    val pagado = doc.getBoolean("pagadoQuincenaActual") ?: false
                    val celularTexto = normalizarCelular(doc.getString("celular").orEmpty())
                    val claveTexto = doc.getString("claveActual").orEmpty()
                    val cambiarClave = doc.getBoolean("debeCambiarClave") ?: true

                    PuestoCadena(
                        id = doc.id,
                        numeroPuesto = numero,
                        titular = titularTexto,
                        puestosComprados = comprados,
                        valorCuotaIndividual = cuotaIndividual,
                        aporteTotalQuincenal = aporteTotal,
                        pagadoQuincenaActual = pagado,
                        celular = celularTexto,
                        claveActual = claveTexto,
                        debeCambiarClave = cambiarClave
                    )
                }?.sortedBy { it.numeroPuesto } ?: emptyList()

                _puestos.value = listaConvertida
            }
    }

    // ========================================================================
    // BLOQUE 4: PAGOS Y CIERRE DE QUINCENA
    // Marca o desmarca el pago de un titular en la quincena activa y permite
    // cerrar la quincena reiniciando pagos y avanzando la quincena.
    // ========================================================================

    /**
     * Cambia el estado pagadoQuincenaActual para todos los puestos de un titular,
     * primero en memoria y luego en lote dentro de Firestore.
     */
    fun alternarPagoTitular(titular: String, marcadoComoPagado: Boolean) {
        if (titular.isBlank()) return

        _puestos.value = _puestos.value.map { puesto ->
            if (puesto.titular.equals(titular.trim(), ignoreCase = true)) {
                puesto.copy(pagadoQuincenaActual = marcadoComoPagado)
            } else {
                puesto
            }
        }

        viewModelScope.launch {
            try {
                val snapshot = db.collection("puestos").get().await()
                val lote = db.batch()

                for (doc in snapshot.documents) {
                    val nombreDoc = doc.getString("titular")?.trim().orEmpty()
                    if (nombreDoc.equals(titular.trim(), ignoreCase = true)) {
                        val cambios: Map<String, Any> = mapOf("pagadoQuincenaActual" to marcadoComoPagado)
                        lote.update(doc.reference, cambios)
                    }
                }

                lote.commit().await()
                _mensajeEstado.value = if (marcadoComoPagado) {
                    "Pago registrado para $titular."
                } else {
                    "Pago marcado como pendiente para $titular."
                }
            } catch (e: Exception) {
                _mensajeEstado.value = "Error al actualizar pago: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Cierra la quincena activa: reinicia pagadoQuincenaActual a false en todos
     * los puestos e incrementa quincenaActual en configuracion/general.
     */
    fun cerrarQuincenaActual() {
        val siguienteQuincena = _quincenaActual.value + 1

        viewModelScope.launch {
            _cargando.value = true
            try {
                val snapshot = db.collection("puestos").get().await()
                val lote = db.batch()

                for (doc in snapshot.documents) {
                    val reiniciarPago: Map<String, Any> = mapOf("pagadoQuincenaActual" to false)
                    lote.update(doc.reference, reiniciarPago)
                }

                val refConfig = db.collection("configuracion").document("general")
                val nuevaQuincenaMap: Map<String, Any> = mapOf("quincenaActual" to siguienteQuincena)
                lote.set(refConfig, nuevaQuincenaMap, SetOptions.merge())

                lote.commit().await()
                _quincenaActual.value = siguienteQuincena
                _mensajeEstado.value = "Se cerró la quincena. Ahora estamos en la Quincena $siguienteQuincena."
            } catch (e: Exception) {
                _mensajeEstado.value = "Error al cerrar quincena: ${e.localizedMessage}"
            } finally {
                _cargando.value = false
            }
        }
    }

    // ========================================================================
    // BLOQUE 5: GESTIÓN DE FAMILIARES Y TURNOS
    // Crea, edita, intercambia turnos y elimina familiares o puestos en
    // Cloud Firestore.
    // ========================================================================

    /**
     * Crea un nuevo registro en "puestos" incluyendo celular normalizado,
     * clave inicial y debeCambiarClave en true.
     */
    fun guardarFamiliar(
        titular: String,
        puestosComprados: Int,
        numeroPuesto: Int,
        valorCuota: Int,
        celular: String = "",
        claveInicial: String = ""
    ) {
        val cantidadSegura = puestosComprados.coerceAtLeast(1)
        val cuotaSegura = valorCuota.coerceAtLeast(0)
        val aporteTotal = cantidadSegura * cuotaSegura
        val celularLimpio = normalizarCelular(celular)
        val claveSegura = claveInicial.trim().ifBlank { "1234" }

        val nuevoDocumento: Map<String, Any> = mapOf(
            "titular" to titular.trim(),
            "puestosComprados" to cantidadSegura,
            "numeroPuesto" to numeroPuesto.coerceAtLeast(1),
            "valorCuotaIndividual" to cuotaSegura,
            "aporteTotalQuincenal" to aporteTotal,
            "pagadoQuincenaActual" to false,
            "celular" to celularLimpio,
            "claveActual" to claveSegura,
            "debeCambiarClave" to true
        )

        viewModelScope.launch {
            try {
                db.collection("puestos").add(nuevoDocumento).await()
                _mensajeEstado.value = "Familiar '$titular' guardado."
            } catch (e: Exception) {
                _mensajeEstado.value = "Error al guardar familiar: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Actualiza el nombre del titular, la cantidad de puestos y la cuota
     * individual en Firestore.
     */
    fun editarFamiliar(
        titularAnterior: String,
        nuevoTitular: String,
        puestosComprados: Int,
        valorCuota: Int
    ) {
        val cantidadSegura = puestosComprados.coerceAtLeast(1)
        val cuotaSegura = valorCuota.coerceAtLeast(0)
        val aporteTotal = cantidadSegura * cuotaSegura

        viewModelScope.launch {
            try {
                val snapshot = db.collection("puestos").get().await()
                val lote = db.batch()
                val cambios: Map<String, Any> = mapOf(
                    "titular" to nuevoTitular.trim(),
                    "puestosComprados" to cantidadSegura,
                    "valorCuotaIndividual" to cuotaSegura,
                    "aporteTotalQuincenal" to aporteTotal
                )

                for (doc in snapshot.documents) {
                    val nombreDoc = doc.getString("titular")?.trim().orEmpty()
                    if (nombreDoc.equals(titularAnterior.trim(), ignoreCase = true)) {
                        lote.update(doc.reference, cambios)
                    }
                }

                lote.commit().await()
                _mensajeEstado.value = "Datos de '$nuevoTitular' actualizados."
            } catch (e: Exception) {
                _mensajeEstado.value = "Error al editar familiar: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Intercambia los números de puesto (turnos) entre dos titulares.
     */
    fun intercambiarTurnos(
        primerTitular: String,
        segundoTitular: String
    ) {
        val listaActual = _puestos.value
        val turnoPrimero = listaActual
            .firstOrNull { it.titular.equals(primerTitular.trim(), ignoreCase = true) }
            ?.numeroPuesto
        val turnoSegundo = listaActual
            .firstOrNull { it.titular.equals(segundoTitular.trim(), ignoreCase = true) }
            ?.numeroPuesto

        if (turnoPrimero == null || turnoSegundo == null) {
            _mensajeEstado.value = "No se encontraron los turnos de ambos familiares."
            return
        }

        viewModelScope.launch {
            try {
                val snapshot = db.collection("puestos").get().await()
                val lote = db.batch()

                for (doc in snapshot.documents) {
                    val nombreDoc = doc.getString("titular")?.trim().orEmpty()
                    when {
                        nombreDoc.equals(primerTitular.trim(), ignoreCase = true) -> {
                            val cambioTurno: Map<String, Any> = mapOf("numeroPuesto" to turnoSegundo)
                            lote.update(doc.reference, cambioTurno)
                        }
                        nombreDoc.equals(segundoTitular.trim(), ignoreCase = true) -> {
                            val cambioTurno: Map<String, Any> = mapOf("numeroPuesto" to turnoPrimero)
                            lote.update(doc.reference, cambioTurno)
                        }
                    }
                }

                lote.commit().await()
                _mensajeEstado.value = "Turnos intercambiados entre $primerTitular y $segundoTitular."
            } catch (e: Exception) {
                _mensajeEstado.value = "Error al intercambiar turnos: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Elimina todos los documentos de puestos asociados a un titular.
     */
    fun eliminarFamiliar(titular: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("puestos").get().await()
                val lote = db.batch()

                for (doc in snapshot.documents) {
                    val nombreDoc = doc.getString("titular")?.trim().orEmpty()
                    if (nombreDoc.equals(titular.trim(), ignoreCase = true)) {
                        lote.delete(doc.reference)
                    }
                }

                lote.commit().await()
                _mensajeEstado.value = "Familiar '$titular' eliminado."
            } catch (e: Exception) {
                _mensajeEstado.value = "Error al eliminar familiar: ${e.localizedMessage}"
            }
        }
    }

    // ========================================================================
    // BLOQUE 6: AJUSTES GENERALES, LIMPIEZA TOTAL Y RESPALDOS (.JSON)
    // Guarda ajustes generales, reinicia la cadena y exporta o importa
    // respaldos en formato JSON.
    // ========================================================================

    /**
     * Guarda el valor de cuota base y la quincena activa en estado y Firestore.
     */
    fun guardarAjustesCadena(
        nuevoValorCuota: Int,
        nuevaQuincenaActual: Int
    ) {
        _valorCuotaBase.value = nuevoValorCuota
        _quincenaActual.value = nuevaQuincenaActual

        viewModelScope.launch {
            try {
                val datosConfig: Map<String, Any> = mapOf(
                    "valorCuotaBase" to nuevoValorCuota,
                    "quincenaActual" to nuevaQuincenaActual
                )
                db.collection("configuracion")
                    .document("general")
                    .set(datosConfig, SetOptions.merge())
                    .await()
                _mensajeEstado.value = "Ajustes guardados en la nube."
            } catch (e: Exception) {
                _mensajeEstado.value = "Error al guardar ajustes: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Borra todos los puestos y restablece quincenaActual a 1.
     */
    fun finalizarYLimpiarCadena() {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val snapshot = db.collection("puestos").get().await()
                val lote = db.batch()

                for (doc in snapshot.documents) {
                    lote.delete(doc.reference)
                }

                val refConfig = db.collection("configuracion").document("general")
                val reinicioMap: Map<String, Any> = mapOf("quincenaActual" to 1)
                lote.set(refConfig, reinicioMap, SetOptions.merge())

                lote.commit().await()
                _puestos.value = emptyList()
                _quincenaActual.value = 1
                _mensajeEstado.value = "Cadena finalizada y limpiada."
            } catch (e: Exception) {
                _mensajeEstado.value = "Error al limpiar la cadena: ${e.localizedMessage}"
            } finally {
                _cargando.value = false
            }
        }
    }

    /**
     * Exporta la configuración y la lista de puestos a un archivo .json.
     */
    fun exportarRespaldoHaciaUri(context: Context, uriDestino: Uri) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val raizJson = JSONObject().apply {
                    put("quincenaActual", _quincenaActual.value)
                    put("valorCuotaBase", _valorCuotaBase.value)

                    val arregloPuestos = JSONArray()
                    for (puesto in _puestos.value) {
                        val itemJson = JSONObject().apply {
                            put("numeroPuesto", puesto.numeroPuesto)
                            put("titular", puesto.titular)
                            put("puestosComprados", puesto.puestosComprados)
                            put("valorCuotaIndividual", puesto.valorCuotaIndividual)
                            put("aporteTotalQuincenal", puesto.aporteTotalQuincenal)
                            put("pagadoQuincenaActual", puesto.pagadoQuincenaActual)
                            put("celular", puesto.celular)
                            put("claveActual", puesto.claveActual)
                            put("debeCambiarClave", puesto.debeCambiarClave)
                        }
                        arregloPuestos.put(itemJson)
                    }
                    put("puestos", arregloPuestos)
                }

                context.contentResolver.openOutputStream(uriDestino)?.use { flujoSalida ->
                    flujoSalida.write(raizJson.toString(2).toByteArray(Charsets.UTF_8))
                }
                _mensajeEstado.value = "Respaldo (.json) exportado correctamente."
            } catch (e: Exception) {
                _mensajeEstado.value = "Error al exportar respaldo: ${e.localizedMessage}"
            } finally {
                _cargando.value = false
            }
        }
    }

    /**
     * Importa un archivo .json de respaldo y reemplaza los puestos en Firestore.
     */
    fun importarRespaldoDesdeUri(context: Context, uriOrigen: Uri) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val contenidoJson = StringBuilder()
                context.contentResolver.openInputStream(uriOrigen)?.use { flujoEntrada ->
                    BufferedReader(InputStreamReader(flujoEntrada, Charsets.UTF_8)).use { lector ->
                        var linea = lector.readLine()
                        while (linea != null) {
                            contenidoJson.append(linea)
                            linea = lector.readLine()
                        }
                    }
                }

                val raiz = JSONObject(contenidoJson.toString())
                val nuevaQuincena = raiz.optInt("quincenaActual", _quincenaActual.value).coerceAtLeast(1)
                val nuevaCuota = raiz.optInt("valorCuotaBase", _valorCuotaBase.value).coerceAtLeast(0)
                val arregloPuestos = raiz.optJSONArray("puestos") ?: JSONArray()

                val snapshotActual = db.collection("puestos").get().await()
                val lote = db.batch()

                for (doc in snapshotActual.documents) {
                    lote.delete(doc.reference)
                }

                for (i in 0 until arregloPuestos.length()) {
                    val obj = arregloPuestos.optJSONObject(i) ?: continue
                    val comprados = obj.optInt("puestosComprados", 1).coerceAtLeast(1)
                    val cuotaInd = obj.optInt("valorCuotaIndividual", nuevaCuota).coerceAtLeast(0)
                    val aporte = obj.optInt("aporteTotalQuincenal", comprados * cuotaInd)

                    val docNuevo: Map<String, Any> = mapOf(
                        "numeroPuesto" to obj.optInt("numeroPuesto", i + 1).coerceAtLeast(1),
                        "titular" to obj.optString("titular", ""),
                        "puestosComprados" to comprados,
                        "valorCuotaIndividual" to cuotaInd,
                        "aporteTotalQuincenal" to aporte,
                        "pagadoQuincenaActual" to obj.optBoolean("pagadoQuincenaActual", false),
                        "celular" to normalizarCelular(obj.optString("celular", "")),
                        "claveActual" to obj.optString("claveActual", "1234"),
                        "debeCambiarClave" to obj.optBoolean("debeCambiarClave", true)
                    )
                    val nuevaRef = db.collection("puestos").document()
                    lote.set(nuevaRef, docNuevo)
                }

                val refConfig = db.collection("configuracion").document("general")
                val configMap: Map<String, Any> = mapOf(
                    "quincenaActual" to nuevaQuincena,
                    "valorCuotaBase" to nuevaCuota
                )
                lote.set(refConfig, configMap, SetOptions.merge())

                lote.commit().await()
                _quincenaActual.value = nuevaQuincena
                _valorCuotaBase.value = nuevaCuota
                _mensajeEstado.value = "Respaldo importado con éxito (${arregloPuestos.length()} puestos)."
            } catch (e: Exception) {
                _mensajeEstado.value = "Error al importar respaldo: ${e.localizedMessage}"
            } finally {
                _cargando.value = false
            }
        }
    }

    // ========================================================================
    // BLOQUE 7: LOGIN POR CELULAR, SESIÓN Y CAMBIO OBLIGATORIO DE CLAVE
    // Gestiona el acceso por teléfono celular (administrador o familiar), la
    // sesión activa y el cambio obligatorio de contraseña en el primer ingreso.
    // ========================================================================

    enum class RolUsuario {
        ADMIN,
        USUARIO
    }

    data class SesionActiva(
        val rol: RolUsuario,
        val nombreMostrado: String,
        val celular: String,
        val debeCambiarClave: Boolean = false
    )

    private val celularAdminConfigurado = normalizarCelular("3173699663")
    private val claveAdminConfigurada = "Andromeda26."

    private val _sesionActiva = MutableStateFlow<SesionActiva?>(null)
    val sesionActiva: StateFlow<SesionActiva?> = _sesionActiva.asStateFlow()

    /**
     * Limpia espacios, guiones o prefijo 57 para comparar celulares igual.
     */
    fun normalizarCelular(celularRaw: String): String {
        val soloDigitos = celularRaw.filter { it.isDigit() }
        return if (soloDigitos.length > 10 && soloDigitos.startsWith("57")) {
            soloDigitos.removePrefix("57")
        } else {
            soloDigitos
        }
    }

    /**
     * Valida celular y clave para abrir sesión de administrador o familiar.
     */
    fun iniciarSesionPorCelular(
        celularIngresado: String,
        claveIngresada: String,
        onResultado: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val celularLimpio = normalizarCelular(celularIngresado)
        val claveLimpia = claveIngresada.trim()

        if (celularLimpio.length < 7) {
            onResultado(false, "Ingresa un número de celular válido.")
            return
        }
        if (claveLimpia.isBlank()) {
            onResultado(false, "Ingresa tu contraseña.")
            return
        }

        if (celularLimpio == celularAdminConfigurado) {
            if (claveLimpia == claveAdminConfigurada) {
                _sesionActiva.value = SesionActiva(
                    rol = RolUsuario.ADMIN,
                    nombreMostrado = "Administrador",
                    celular = celularLimpio,
                    debeCambiarClave = false
                )
                onResultado(true, "Bienvenido, Administrador.")
            } else {
                onResultado(false, "Contraseña de administrador incorrecta.")
            }
            return
        }

        val registroFamiliar = _puestos.value.firstOrNull { puesto ->
            normalizarCelular(puesto.celular) == celularLimpio
        }

        if (registroFamiliar == null) {
            onResultado(false, "No hay ningún familiar registrado con ese celular.")
            return
        }

        val claveEsperada = registroFamiliar.claveActual.ifBlank { "1234" }
        if (claveLimpia != claveEsperada) {
            onResultado(false, "Contraseña incorrecta.")
            return
        }

        _sesionActiva.value = SesionActiva(
            rol = RolUsuario.USUARIO,
            nombreMostrado = registroFamiliar.titular,
            celular = celularLimpio,
            debeCambiarClave = registroFamiliar.debeCambiarClave
        )
        onResultado(true, "Bienvenido/a, ${registroFamiliar.titular}.")
    }

    /**
     * Devuelve los puestos pertenecientes a un titular específico.
     */
    fun obtenerPuestosDeTitular(nombreTitular: String): List<PuestoCadena> {
        val objetivo = nombreTitular.trim()
        if (objetivo.isBlank()) return emptyList()
        return _puestos.value.filter { it.titular.trim().equals(objetivo, ignoreCase = true) }
    }

    /**
     * Calcula el aporte total que debe pagar un titular en la quincena actual.
     */
    fun calcularAporteQuincenalUsuario(nombreTitular: String): Int {
        val registros = obtenerPuestosDeTitular(nombreTitular)
        if (registros.isEmpty()) return 0

        val totalPuestos = if (registros.size > 1 && registros.all { it.puestosComprados == 1 }) {
            registros.size
        } else {
            registros.sumOf { it.puestosComprados }.coerceAtLeast(registros.size)
        }

        val cuotaUnitaria = registros.first().valorCuotaIndividual.takeIf { it > 0 }
            ?: _valorCuotaBase.value

        return totalPuestos * cuotaUnitaria
    }

    /**
     * Indica si el titular tiene todos sus puestos marcados como pagados hoy.
     */
    fun usuarioEstaAlDiaEnQuincena(nombreTitular: String): Boolean {
        val registros = obtenerPuestosDeTitular(nombreTitular)
        return registros.isNotEmpty() && registros.all { it.pagadoQuincenaActual }
    }

    /**
     * Registra celular, clave inicial y debeCambiarClave = true en los puestos
     * del familiar en Firestore.
     */
    fun configurarAccesoFamiliar(
        titular: String,
        celular: String,
        claveInicial: String
    ) {
        val celularLimpio = normalizarCelular(celular)
        val claveLimpia = claveInicial.trim()
        if (titular.isBlank() || celularLimpio.isBlank() || claveLimpia.isBlank()) {
            _mensajeEstado.value = "Completa nombre, celular y clave inicial."
            return
        }

        viewModelScope.launch {
            try {
                val snapshot = db.collection("puestos").get().await()
                val lote = db.batch()
                val datosAcceso: Map<String, Any> = mapOf(
                    "celular" to celularLimpio,
                    "claveActual" to claveLimpia,
                    "debeCambiarClave" to true
                )

                for (doc in snapshot.documents) {
                    val nombreDoc = doc.getString("titular")?.trim().orEmpty()
                    if (nombreDoc.equals(titular.trim(), ignoreCase = true)) {
                        lote.update(doc.reference, datosAcceso)
                    }
                }

                lote.commit().await()
                _mensajeEstado.value = "Acceso configurado para $titular."
            } catch (e: Exception) {
                _mensajeEstado.value = "Error al configurar acceso: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Guarda la nueva contraseña en Firestore y marca debeCambiarClave = false.
     */
    fun completarCambioClaveObligatorio(
        nuevaClave: String,
        onResultado: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val actual = _sesionActiva.value
        val claveLimpia = nuevaClave.trim()

        if (actual == null || actual.rol != RolUsuario.USUARIO) {
            onResultado(false, "No hay sesión de usuario activa.")
            return
        }
        if (claveLimpia.length < 4) {
            onResultado(false, "La nueva clave debe tener al menos 4 caracteres.")
            return
        }

        viewModelScope.launch {
            try {
                val snapshot = db.collection("puestos").get().await()
                val lote = db.batch()
                val cambios: Map<String, Any> = mapOf(
                    "claveActual" to claveLimpia,
                    "debeCambiarClave" to false
                )

                for (doc in snapshot.documents) {
                    val nombreDoc = doc.getString("titular")?.trim().orEmpty()
                    val celDoc = normalizarCelular(doc.getString("celular").orEmpty())
                    if (nombreDoc.equals(actual.nombreMostrado.trim(), ignoreCase = true) ||
                        (actual.celular.isNotBlank() && celDoc == actual.celular)
                    ) {
                        lote.update(doc.reference, cambios)
                    }
                }

                lote.commit().await()
                _sesionActiva.value = actual.copy(debeCambiarClave = false)
                onResultado(true, "Contraseña actualizada.")
            } catch (e: Exception) {
                onResultado(false, "Error al actualizar clave: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Cierra la sesión activa para volver a la pantalla de login.
     */
    fun cerrarSesion() {
        _sesionActiva.value = null
    }
}