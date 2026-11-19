package com.example.cadenav2.repository

import com.example.cadenav2.model.PuestoModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

/**
 * =====================================================================
 * REPOSITORIO DE LA CADENA: Habla directamente con Firebase Firestore
 * =====================================================================
 * Aquí se guardan y se leen los datos de la nube para que la pantalla
 * no tenga que saber cómo funciona Firebase por dentro.
 */
class CadenaRepository {

    // Obtenemos la conexión principal a la base de datos Cloud Firestore
    private val db = FirebaseFirestore.getInstance()

    /**
     * Descarga todos los puestos desde la colección "puestos" de Firestore
     * y los devuelve ordenados del puesto 1 al 18.
     */
    suspend fun obtenerPuestos(): Result<List<PuestoModel>> {
        return try {
            // Pedimos todos los documentos de la colección "puestos"
            val snapshot = db.collection("puestos").get().await()

            // Convertimos cada documento de Firestore en un objeto PuestoModel
            val listaPuestos = snapshot.documents.mapNotNull { doc ->
                doc.toObject(PuestoModel::class.java)?.copy(id = doc.id)
            }.sortedBy { it.numeroPuesto } // Ordenamos por número de puesto (1, 2, 3...)

            Result.success(listaPuestos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lee un texto en formato JSON (elegido por el administrador desde su teléfono)
     * y sube su información a Cloud Firestore.
     */
    suspend fun sincronizarDesdeBackupJson(jsonTexto: String): Result<String> {
        return try {
            // Convertimos el texto plano en un objeto JSON que Kotlin puede leer por llaves
            val raiz = JSONObject(jsonTexto)

            // Creamos un lote (batch) para escribir varios documentos en Firestore al mismo tiempo
            val batch = db.batch()

            // 1. Si el respaldo trae configuración general, la guardamos en "configuracion/general"
            if (raiz.has("configuracion")) {
                val configObj = raiz.getJSONObject("configuracion")
                val mapaConfig = hashMapOf<String, Any>(
                    "nombreCadena" to configObj.optString("nombreCadena", "Cadena Familiar"),
                    "quincenaActual" to configObj.optInt("quincenaActual", 1),
                    "valorCuota" to configObj.optInt("valorCuota", 50000),
                    "totalPuestos" to configObj.optInt("totalPuestos", 18)
                )
                val refConfig = db.collection("configuracion").document("general")
                batch.set(refConfig, mapaConfig)
            }

            // 2. Si el respaldo trae la lista de "puestos", los recorremos uno por uno
            if (raiz.has("puestos")) {
                val arrayPuestos = raiz.getJSONArray("puestos")
                for (i in 0 until arrayPuestos.length()) {
                    val item = arrayPuestos.getJSONObject(i)
                    val numero = item.optInt("numeroPuesto", i + 1)
                    val docId = item.optString("id").ifBlank { "puesto_$numero" }

                    val puesto = PuestoModel(
                        id = docId,
                        numeroPuesto = numero,
                        participanteId = item.optInt("participanteId", 0),
                        titular = item.optString("titular", ""),
                        puestosComprados = item.optInt("puestosComprados", 1),
                        puestosAsignadosTexto = item.optString("puestosAsignadosTexto", "$numero"),
                        valorCuotaIndividual = item.optInt("valorCuotaIndividual", 50000),
                        aporteTotalQuincenal = item.optInt("aporteTotalQuincenal", 50000),
                        fechaCobro = item.optString("fechaCobro", ""),
                        pagadoQuincenaActual = item.optBoolean("pagadoQuincenaActual", false)
                    )

                    val refPuesto = db.collection("puestos").document(docId)
                    batch.set(refPuesto, puesto)
                }
            }

            // 3. Si el respaldo trae historial de "pagos", también los subimos
            if (raiz.has("pagos")) {
                val arrayPagos = raiz.getJSONArray("pagos")
                for (i in 0 until arrayPagos.length()) {
                    val pagoObj = arrayPagos.getJSONObject(i)
                    val pagoId = pagoObj.optString("id").ifBlank { "pago_${i + 1}" }
                    val mapaPago = mutableMapOf<String, Any>()
                    val llaves = pagoObj.keys()
                    while (llaves.hasNext()) {
                        val llave = llaves.next()
                        mapaPago[llave] = pagoObj.get(llave)
                    }
                    val refPago = db.collection("pagos").document(pagoId)
                    batch.set(refPago, mapaPago)
                }
            }

            // Ejecutamos todo el paquete de cambios en la nube de una sola vez
            batch.commit().await()
            Result.success("Respaldo subido y sincronizado en Firestore correctamente.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Descarga el estado actual de Firestore (configuración, puestos y pagos)
     * y construye un texto JSON bonito y ordenado para guardarlo en el celular.
     */
    suspend fun generarBackupJson(): Result<String> {
        return try {
            // Objeto JSON principal que contendrá toda la copia de seguridad
            val raizJson = JSONObject()
            raizJson.put("versionRespaldo", "2.0")
            raizJson.put("fechaExportacion", System.currentTimeMillis())

            // 1. Leemos la configuración general si existe en la nube
            val docConfig = db.collection("configuracion").document("general").get().await()
            if (docConfig.exists() && docConfig.data != null) {
                raizJson.put("configuracion", JSONObject(docConfig.data!!))
            }

            // 2. Leemos todos los puestos ordenados del 1 al 18
            val snapPuestos = db.collection("puestos").get().await()
            val listaPuestos = snapPuestos.documents.mapNotNull { doc ->
                doc.toObject(PuestoModel::class.java)?.copy(id = doc.id)
            }.sortedBy { it.numeroPuesto }

            // Convertimos cada PuestoModel a un objeto JSON dentro de un arreglo
            val arrayPuestos = JSONArray()
            for (puesto in listaPuestos) {
                val obj = JSONObject().apply {
                    put("id", puesto.id)
                    put("numeroPuesto", puesto.numeroPuesto)
                    put("participanteId", puesto.participanteId)
                    put("titular", puesto.titular)
                    put("puestosComprados", puesto.puestosComprados)
                    put("puestosAsignadosTexto", puesto.puestosAsignadosTexto)
                    put("valorCuotaIndividual", puesto.valorCuotaIndividual)
                    put("aporteTotalQuincenal", puesto.aporteTotalQuincenal)
                    put("fechaCobro", puesto.fechaCobro)
                    put("pagadoQuincenaActual", puesto.pagadoQuincenaActual)
                }
                arrayPuestos.put(obj)
            }
            raizJson.put("puestos", arrayPuestos)

            // 3. Leemos los pagos registrados para incluirlos también en el respaldo
            val snapPagos = db.collection("pagos").get().await()
            val arrayPagos = JSONArray()
            for (docPago in snapPagos.documents) {
                val datos = docPago.data ?: continue
                val objPago = JSONObject(datos)
                objPago.put("id", docPago.id)
                arrayPagos.put(objPago)
            }
            raizJson.put("pagos", arrayPagos)

            // toString(2) formatea el JSON con sangría de 2 espacios para que sea legible
            Result.success(raizJson.toString(2))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}