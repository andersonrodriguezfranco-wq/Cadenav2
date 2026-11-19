package com.example.cadenav2.viewmodel

// ============================================================================
// BLOQUE 1: IMPORTACIONES Y MODELO DE DATOS DEL PUESTO
// Aquí traemos las herramientas de ViewModel, corrutinas y Firestore, y
// definimos la estructura de un puesto dentro de la cadena.
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
 * Representa un puesto registrado en la cadena.
 * Cada propiedad guarda un dato importante para calcular cuotas, turnos y pagos.
 */
data class PuestoCadena(
    val id: String = "",
    val numeroPuesto: Int = 1,
    val titular: String = "",
    val puestosComprados: Int = 1,
    val valorCuotaIndividual: Int = 50000,
    val aporteTotalQuincenal: Int = 50000,
    val pagadoQuincenaActual: Boolean = false
)

// ============================================================================
// BLOQUE 2: CLASE CADENAVIEWMODEL Y FLUJOS DE ESTADO (STATEFLOW)
// El ViewModel guarda los datos de la pantalla y sobrevive a rotaciones.
// MutableStateFlow permite cambiar valores dentro del ViewModel;
// StateFlow expone esos valores como de solo lectura hacia las pantallas.
// ============================================================================

class CadenaViewModel : ViewModel() {

    // Instancia única de Cloud Firestore para leer y escribir en la nube
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Escuchadores activos en tiempo real para liberar recursos al cerrar el ViewModel
    private var escuchadorPuestos: ListenerRegistration? = null
    private var escuchadorConfig: ListenerRegistration? = null

    // Lista de todos los puestos cargados desde Firestore
    private val _puestos = MutableStateFlow<List<PuestoCadena>>(emptyList())
    val puestos: StateFlow<List<PuestoCadena>> = _puestos.asStateFlow()

    // Número de la quincena que se está jugando actualmente (inicia en 1)
    private val _quincenaActual = MutableStateFlow(1)
    val quincenaActual: StateFlow<Int> = _quincenaActual.asStateFlow()

    // Valor base por defecto de cada puesto en pesos (ej. 50000)
    private val _valorCuotaBase = MutableStateFlow(50000)
    val valorCuotaBase: StateFlow<Int> = _valorCuotaBase.asStateFlow()

    // Indica si hay una operación cargando o guardando en este momento
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    // Mensaje corto para mostrar cómo va la sincronización o la última acción
    private val _mensajeEstado = MutableStateFlow("")
    val mensajeEstado: StateFlow<String> = _mensajeEstado.asStateFlow()

    // Bloque "init": se ejecuta automáticamente apenas se crea el ViewModel
    init {
        observarConfiguracion()
        observarPuestos()
    }

    // Se ejecuta cuando la pantalla/ViewModel se destruye para evitar fugas de memoria
    override fun onCleared() {
        escuchadorPuestos?.remove()
        escuchadorConfig?.remove()
        super.onCleared()
    }

    // ========================================================================
    // BLOQUE 3: SINCRONIZACIÓN EN TIEMPO REAL CON FIRESTORE
    // Escucha cambios en la configuración general y en la colección de puestos
    // para que todas las pantallas muestren siempre información actualizada.
    // ========================================================================

    /**
     * Escucha el documento configuracion/general para obtener quincenaActual
     * y valorCuotaBase en tiempo real.
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
     * Escucha la colección "puestos" y convierte cada documento en un PuestoCadena.
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

                    PuestoCadena(
                        id = doc.id,
                        numeroPuesto = numero,
                        titular = titularTexto,
                        puestosComprados = comprados,
                        valorCuotaIndividual = cuotaIndividual,
                        aporteTotalQuincenal = aporteTotal,
                        pagadoQuincenaActual = pagado
                    )
                }?.sortedBy { it.numeroPuesto } ?: emptyList()

                _puestos.value = listaConvertida
            }
    }

    // ========================================================================
    // BLOQUE 4: PAGOS Y CIERRE DE QUINCENA
    // Permite marcar/desmarcar el pago de un titular en la quincena activa y
    // avanzar a la siguiente quincena cuando todos hayan completado su aporte.
    // ========================================================================

    /**
     * Marca o desmarca pagadoQuincenaActual para todos los puestos de un titular.
     */
    fun alternarPagoTitular(titular: String, marcadoComoPagado: Boolean) {
        if (titular.isBlank()) return

        // Actualización optimista local para respuesta visual inmediata
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
     * Cierra la quincena actual: limpia pagadoQuincenaActual en todos los puestos
     * y suma 1 a quincenaActual en Firestore.
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
    // Conecta con PantallaGestionMiembros para agregar, editar, intercambiar
    // turnos y eliminar familiares en Cloud Firestore.
    // ========================================================================

    /**
     * Agrega un nuevo familiar/puesto a la colección "puestos" de Firestore.
     */
    fun guardarFamiliar(
        titular: String,
        puestosComprados: Int,
        numeroPuesto: Int,
        valorCuota: Int
    ) {
        val cantidadSegura = puestosComprados.coerceAtLeast(1)
        val cuotaSegura = valorCuota.coerceAtLeast(0)
        val aporteTotal = cantidadSegura * cuotaSegura

        val nuevoDocumento: Map<String, Any> = mapOf(
            "titular" to titular.trim(),
            "puestosComprados" to cantidadSegura,
            "numeroPuesto" to numeroPuesto.coerceAtLeast(1),
            "valorCuotaIndividual" to cuotaSegura,
            "aporteTotalQuincenal" to aporteTotal,
            "pagadoQuincenaActual" to false
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
     * Edita el nombre y la cantidad de puestos de un familiar existente.
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
     * Intercambia los números de puesto (turnos de quincena) entre dos familiares.
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
     * Elimina todos los puestos asociados a un familiar en Firestore.
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
    // Conecta con PantallaAjustesCadena y las opciones de backup del menú.
    // ========================================================================

    /**
     * Guarda el nuevo valor de cuota por puesto y la quincena activa.
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
     * Borra todos los puestos y reinicia quincenaActual a 1 en Firestore.
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
     * Exporta la configuración y todos los puestos actuales a un archivo .json.
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
     * Lee un archivo .json de respaldo y restaura puestos y configuración en Firestore.
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

                // Primero borramos los puestos actuales para evitar duplicados al restaurar
                for (doc in snapshotActual.documents) {
                    lote.delete(doc.reference)
                }

                // Luego agregamos los puestos del archivo de respaldo
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
                        "pagadoQuincenaActual" to obj.optBoolean("pagadoQuincenaActual", false)
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
}