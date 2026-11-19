package com.example.cadenav2.pantallas

// ============================================================================
// BLOQUE 1: IMPORTACIONES Y ESTRUCTURAS AUXILIARES
// Aquí importamos las clases de Android, Compose y Material 3 que necesita
// la pantalla, y definimos tipos simples para ordenar datos y el menú lateral.
// ============================================================================

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cadenav2.ui.theme.PaletaRetro90s
import com.example.cadenav2.viewmodel.CadenaViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

// Indica qué pantalla está activa en este momento dentro del flujo de administración
private enum class VistaAdminActiva {
    PANEL_GENERAL,
    RESUMEN_CICLO,
    CRONOGRAMA_GLOBAL,
    GESTION_MIEMBROS_AGREGAR,
    GESTION_MIEMBROS_EDITAR,
    GESTION_MIEMBROS_TURNOS,
    GESTION_MIEMBROS_ELIMINAR,
    CORREGIR_PAGOS,
    AJUSTES_CADENA
}

// Consolida los datos de un titular en la quincena activa (evita sumar doble si tiene varios puestos)
private data class ResumenTitularQuincena(
    val titular: String,
    val cantidadPuestos: Int,
    val montoTotal: Int,
    val pagado: Boolean
)

// Representa una opción clickable dentro del menú lateral
private data class OpcionMenuAdmin(
    val id: String,
    val titulo: String,
    val subtitulo: String,
    val icono: ImageVector,
    val tonoIcono: Color,
    val resaltada: Boolean = false,
    val habilitada: Boolean = true,
    val alHacerClic: () -> Unit
)

// Agrupa varias opciones del menú bajo un título de sección
private data class SeccionMenuAdmin(
    val tituloSeccion: String,
    val opciones: List<OpcionMenuAdmin>
)

// ============================================================================
// BLOQUE 2: PANTALLA PRINCIPAL, ESTADOS Y CÁLCULOS DE LA QUINCENA
// Obtenemos los datos del ViewModel y calculamos quién recibe, cuánto se ha
// recaudado y qué miembros siguen pendientes en la quincena actual.
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAdministracion(
    viewModel: CadenaViewModel,
    modifier: Modifier = Modifier
) {
    // Contexto de Android, estado del menú lateral y alcance para animaciones
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Guarda qué vista se muestra actualmente (inicia en PANEL_GENERAL)
    var vistaActiva by remember { mutableStateOf(VistaAdminActiva.PANEL_GENERAL) }

    // Guarda el archivo .json elegido antes de confirmar su importación
    var uriRespaldoSeleccionado by remember { mutableStateOf<Uri?>(null) }

    // Controla si se muestra el cuadro de confirmación para cerrar la quincena
    var mostrarDialogoCerrarQuincena by remember { mutableStateOf(false) }

    // Selector para abrir un archivo de respaldo (.json) desde el dispositivo
    val selectorArchivoSubir = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uriElegida: Uri? ->
        if (uriElegida != null) {
            uriRespaldoSeleccionado = uriElegida
        }
    }

    // Selector para guardar un nuevo archivo de respaldo (.json)
    val selectorArchivoGuardar = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uriDestino: Uri? ->
        if (uriDestino != null) {
            viewModel.exportarRespaldoHaciaUri(context, uriDestino)
        }
    }

    // Flujos de datos del ViewModel: cambian automáticamente cuando se actualiza Firestore
    val puestos by viewModel.puestos.collectAsState()
    val quincenaActual by viewModel.quincenaActual.collectAsState()
    val valorCuotaBase by viewModel.valorCuotaBase.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val mensajeEstado by viewModel.mensajeEstado.collectAsState()

    // Comparte por mensajería o apps externas el estado actual de la quincena
    val compartirEstadoQuincena = { nombreQuienRecibe: String, montoRecaudado: Int, pagaronCount: Int, totalCount: Int ->
        val resumenTexto = buildString {
            append("Quincena $quincenaActual · Cadena Familiar\n")
            append("Recibe esta quincena: $nombreQuienRecibe\n")
            append("Recaudado: ${formatearPesos(montoRecaudado)} ($pagaronCount de $totalCount ya pagaron)")
        }
        val intentCompartir = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, resumenTexto)
        }
        context.startActivity(
            Intent.createChooser(intentCompartir, "Compartir estado de quincena")
        )
    }

    // Puesto asignado a la quincena activa y titular que cobra el pozo
    val puestoQueRecibe = remember(puestos, quincenaActual) {
        puestos.find { it.numeroPuesto == quincenaActual } ?: puestos.firstOrNull()
    }
    val nombreQuienRecibe = puestoQueRecibe?.titular?.trim()?.ifBlank { "Sin asignar" } ?: "Sin asignar"

    // Agrupamos por titular para consolidar puestos, montos y estado de pago
    val todosLosMiembros = remember(puestos, valorCuotaBase) {
        puestos
            .filter { it.titular.isNotBlank() }
            .groupBy { it.titular.trim().uppercase(localeColombia()) }
            .map { (_, registrosDelTitular) ->
                val principal = registrosDelTitular.first()
                val nombreMostrado = principal.titular.trim()
                val cantidad = principal.puestosComprados.coerceAtLeast(1)
                val cuotaUnitaria = principal.valorCuotaIndividual.takeIf { it > 0 } ?: valorCuotaBase
                val monto = if (principal.aporteTotalQuincenal > 0) {
                    principal.aporteTotalQuincenal
                } else {
                    cantidad * cuotaUnitaria
                }
                val todosPagaron = registrosDelTitular.all { it.pagadoQuincenaActual }

                ResumenTitularQuincena(
                    titular = nombreMostrado,
                    cantidadPuestos = cantidad,
                    montoTotal = monto,
                    pagado = todosPagaron
                )
            }
    }

    // Datos de quien recibe en la quincena activa (para el interruptor de su propio aporte)
    val resumenQuienRecibe = remember(todosLosMiembros, nombreQuienRecibe, valorCuotaBase) {
        todosLosMiembros.find { it.titular.equals(nombreQuienRecibe, ignoreCase = true) }
            ?: ResumenTitularQuincena(
                titular = nombreQuienRecibe,
                cantidadPuestos = puestoQueRecibe?.puestosComprados?.coerceAtLeast(1) ?: 1,
                montoTotal = puestoQueRecibe?.aporteTotalQuincenal?.takeIf { it > 0 } ?: valorCuotaBase,
                pagado = puestoQueRecibe?.pagadoQuincenaActual ?: false
            )
    }

    // Lista de pendientes: excluye a quienes ya pagaron y a quien recibe el pozo esta quincena
    val miembrosPendientes = remember(todosLosMiembros, nombreQuienRecibe) {
        todosLosMiembros.filterNot { miembro ->
            miembro.pagado || miembro.titular.equals(nombreQuienRecibe, ignoreCase = true)
        }
    }

    // Solo se habilita cerrar quincena cuando no quedan pendientes y quien recibe ya aportó
    val puedeCerrarQuincena = remember(todosLosMiembros, miembrosPendientes, resumenQuienRecibe) {
        todosLosMiembros.isNotEmpty() &&
                miembrosPendientes.isEmpty() &&
                resumenQuienRecibe.pagado
    }

    // Totales y barra de avance de lo recaudado en la quincena activa
    val totalEsperadoQuincena = remember(todosLosMiembros) {
        todosLosMiembros.sumOf { it.montoTotal }
    }
    val montoRecaudado = remember(todosLosMiembros) {
        todosLosMiembros.filter { it.pagado }.sumOf { it.montoTotal }
    }
    val miembrosPagaronCount = remember(todosLosMiembros) {
        todosLosMiembros.count { it.pagado }
    }
    val progresoRecaudado = if (totalEsperadoQuincena > 0) {
        (montoRecaudado.toFloat() / totalEsperadoQuincena.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    // ========================================================================
    // BLOQUE 3: NAVEGACIÓN A LAS PANTALLAS SECUNDARIAS
    // Si el usuario eligió una opción del menú lateral, mostramos la pantalla
    // correspondiente y le pasamos la función "onVolver" para regresar aquí.
    // ========================================================================
    val volverAlPanel: () -> Unit = { vistaActiva = VistaAdminActiva.PANEL_GENERAL }

    when (vistaActiva) {
        VistaAdminActiva.RESUMEN_CICLO -> {
            PantallaResumenCiclo(viewModel = viewModel, onVolver = volverAlPanel, modifier = modifier)
            return
        }
        VistaAdminActiva.CRONOGRAMA_GLOBAL -> {
            PantallaCronogramaGlobal(viewModel = viewModel, onVolver = volverAlPanel, modifier = modifier)
            return
        }
        VistaAdminActiva.GESTION_MIEMBROS_AGREGAR -> {
            PantallaGestionMiembros(
                viewModel = viewModel,
                onVolver = volverAlPanel,
                modoInicial = "ANADIR",
                modifier = modifier
            )
            return
        }
        VistaAdminActiva.GESTION_MIEMBROS_EDITAR -> {
            PantallaGestionMiembros(
                viewModel = viewModel,
                onVolver = volverAlPanel,
                modoInicial = "EDITAR",
                modifier = modifier
            )
            return
        }
        VistaAdminActiva.GESTION_MIEMBROS_TURNOS -> {
            PantallaGestionMiembros(
                viewModel = viewModel,
                onVolver = volverAlPanel,
                modoInicial = "INTERCAMBIAR",
                modifier = modifier
            )
            return
        }
        VistaAdminActiva.GESTION_MIEMBROS_ELIMINAR -> {
            PantallaGestionMiembros(
                viewModel = viewModel,
                onVolver = volverAlPanel,
                modoInicial = "ELIMINAR",
                modifier = modifier
            )
            return
        }
        VistaAdminActiva.CORREGIR_PAGOS -> {
            PantallaCorregirPagos(viewModel = viewModel, onVolver = volverAlPanel, modifier = modifier)
            return
        }
        VistaAdminActiva.AJUSTES_CADENA -> {
            PantallaAjustesCadena(viewModel = viewModel, onVolver = volverAlPanel, modifier = modifier)
            return
        }
        VistaAdminActiva.PANEL_GENERAL -> {
            // Continúa hacia el panel principal de la quincena
        }
    }

    // ========================================================================
    // BLOQUE 4: DIÁLOGOS DE CONFIRMACIÓN Y MENÚ LATERAL
    // Aquí se manejan los cuadros emergentes (subir respaldo / cerrar quincena)
    // y se arma la lista de secciones y opciones del menú lateral.
    // ========================================================================
    uriRespaldoSeleccionado?.let { uriPendiente ->
        val nombreArchivo = remember(uriPendiente) {
            obtenerNombreArchivoDesdeUri(context, uriPendiente)
        }

        AlertDialog(
            onDismissRequest = { uriRespaldoSeleccionado = null },
            containerColor = PaletaRetro90s.fondoCrema,
            title = {
                Text(
                    text = "Confirmar subida de respaldo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PaletaRetro90s.textoOscuro
                )
            },
            text = {
                Text(
                    text = "Se subirá el archivo:\n\"$nombreArchivo\"\n\n¿Deseas actualizar los datos en la nube?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PaletaRetro90s.textoOscuro
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        uriRespaldoSeleccionado = null
                        viewModel.importarRespaldoDesdeUri(context, uriPendiente)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaletaRetro90s.vino,
                        contentColor = PaletaRetro90s.textoClaro
                    )
                ) {
                    Text(text = "Subir archivo")
                }
            },
            dismissButton = {
                TextButton(onClick = { uriRespaldoSeleccionado = null }) {
                    Text(text = "Cancelar", color = PaletaRetro90s.textoOscuro)
                }
            }
        )
    }

    if (mostrarDialogoCerrarQuincena) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCerrarQuincena = false },
            containerColor = PaletaRetro90s.fondoCrema,
            title = {
                Text(
                    text = "Cerrar Quincena $quincenaActual",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PaletaRetro90s.textoOscuro
                )
            },
            text = {
                Text(
                    text = "Todos los aportes de la Quincena $quincenaActual están completos. Se avanzará a la Quincena ${quincenaActual + 1} y todos los pagos volverán a quedar pendientes.\n\n¿Deseas cerrar esta quincena?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PaletaRetro90s.textoOscuro
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoCerrarQuincena = false
                        viewModel.cerrarQuincenaActual()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaletaRetro90s.ciruela,
                        contentColor = PaletaRetro90s.textoClaro
                    )
                ) {
                    Text(text = "Cerrar quincena", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoCerrarQuincena = false }) {
                    Text(text = "Cancelar", color = PaletaRetro90s.textoOscuro)
                }
            }
        )
    }

    // Cierra el menú lateral y cambia a la pantalla solicitada
    val navegarA: (VistaAdminActiva) -> Unit = { destino ->
        scope.launch { drawerState.close() }
        vistaActiva = destino
    }

    // Secciones del menú lateral replicando las opciones de la V1
    val seccionesMenu = remember(
        cargando,
        quincenaActual,
        nombreQuienRecibe,
        montoRecaudado,
        miembrosPagaronCount,
        todosLosMiembros.size,
        vistaActiva
    ) {
        listOf(
            SeccionMenuAdmin(
                tituloSeccion = "VISTAS",
                opciones = listOf(
                    OpcionMenuAdmin(
                        id = "panel_general",
                        titulo = "Panel general",
                        subtitulo = "Cobros y estado de la Quincena $quincenaActual",
                        icono = Icons.Default.Home,
                        tonoIcono = PaletaRetro90s.vino,
                        resaltada = vistaActiva == VistaAdminActiva.PANEL_GENERAL,
                        alHacerClic = { navegarA(VistaAdminActiva.PANEL_GENERAL) }
                    ),
                    OpcionMenuAdmin(
                        id = "resumen_ciclo",
                        titulo = "Resumen del ciclo",
                        subtitulo = "Métricas generales y avance de la cadena",
                        icono = Icons.AutoMirrored.Filled.List,
                        tonoIcono = PaletaRetro90s.naranja,
                        resaltada = vistaActiva == VistaAdminActiva.RESUMEN_CICLO,
                        alHacerClic = { navegarA(VistaAdminActiva.RESUMEN_CICLO) }
                    ),
                    OpcionMenuAdmin(
                        id = "cronograma_global",
                        titulo = "Cronograma global",
                        subtitulo = "Orden de quincenas y quién recibe en cada turno",
                        icono = Icons.Default.DateRange,
                        tonoIcono = PaletaRetro90s.rojo,
                        resaltada = vistaActiva == VistaAdminActiva.CRONOGRAMA_GLOBAL,
                        alHacerClic = { navegarA(VistaAdminActiva.CRONOGRAMA_GLOBAL) }
                    )
                )
            ),
            SeccionMenuAdmin(
                tituloSeccion = "MIEMBROS",
                opciones = listOf(
                    OpcionMenuAdmin(
                        id = "anadir_familiar",
                        titulo = "Añadir familiar",
                        subtitulo = "Registrar un nuevo titular o puesto",
                        icono = Icons.Default.Add,
                        tonoIcono = PaletaRetro90s.vino,
                        alHacerClic = { navegarA(VistaAdminActiva.GESTION_MIEMBROS_AGREGAR) }
                    ),
                    OpcionMenuAdmin(
                        id = "editar_familiar",
                        titulo = "Editar familiar",
                        subtitulo = "Modificar nombre o cantidad de puestos",
                        icono = Icons.Default.Create,
                        tonoIcono = PaletaRetro90s.ciruela,
                        alHacerClic = { navegarA(VistaAdminActiva.GESTION_MIEMBROS_EDITAR) }
                    ),
                    OpcionMenuAdmin(
                        id = "intercambiar_turnos",
                        titulo = "Intercambiar turnos",
                        subtitulo = "Cambiar la quincena de cobro entre dos familiares",
                        icono = Icons.Default.Refresh,
                        tonoIcono = PaletaRetro90s.naranja,
                        alHacerClic = { navegarA(VistaAdminActiva.GESTION_MIEMBROS_TURNOS) }
                    ),
                    OpcionMenuAdmin(
                        id = "eliminar_familiar",
                        titulo = "Eliminar familiar",
                        subtitulo = "Quitar un familiar de la cadena",
                        icono = Icons.Default.Delete,
                        tonoIcono = PaletaRetro90s.rojo,
                        alHacerClic = { navegarA(VistaAdminActiva.GESTION_MIEMBROS_ELIMINAR) }
                    )
                )
            ),
            SeccionMenuAdmin(
                tituloSeccion = "CONTROL",
                opciones = listOf(
                    OpcionMenuAdmin(
                        id = "corregir_pagos",
                        titulo = "Corregir pagos Q#$quincenaActual",
                        subtitulo = "Activar o desactivar aportes ya registrados",
                        icono = Icons.Default.AccountCircle,
                        tonoIcono = PaletaRetro90s.vino,
                        alHacerClic = { navegarA(VistaAdminActiva.CORREGIR_PAGOS) }
                    ),
                    OpcionMenuAdmin(
                        id = "compartir_estado",
                        titulo = "Compartir estado Q#$quincenaActual",
                        subtitulo = "Enviar resumen rápido por mensajería",
                        icono = Icons.Default.Share,
                        tonoIcono = PaletaRetro90s.naranja,
                        alHacerClic = {
                            scope.launch { drawerState.close() }
                            compartirEstadoQuincena(
                                nombreQuienRecibe,
                                montoRecaudado,
                                miembrosPagaronCount,
                                todosLosMiembros.size
                            )
                        }
                    ),
                    OpcionMenuAdmin(
                        id = "ajustes_cadena",
                        titulo = "Ajustes de la cadena",
                        subtitulo = "Cuota base, quincena activa y cierre total",
                        icono = Icons.Default.Settings,
                        tonoIcono = PaletaRetro90s.ciruela,
                        alHacerClic = { navegarA(VistaAdminActiva.AJUSTES_CADENA) }
                    )
                )
            ),
            SeccionMenuAdmin(
                tituloSeccion = "RESPALDO",
                opciones = listOf(
                    OpcionMenuAdmin(
                        id = "cargar_backup",
                        titulo = "Cargar backup",
                        subtitulo = "Importar respaldo (.json) desde el dispositivo",
                        icono = Icons.Default.Refresh,
                        tonoIcono = PaletaRetro90s.rojo,
                        habilitada = !cargando,
                        alHacerClic = {
                            scope.launch { drawerState.close() }
                            selectorArchivoSubir.launch(arrayOf("application/json", "*/*"))
                        }
                    ),
                    OpcionMenuAdmin(
                        id = "exportar_backup",
                        titulo = "Exportar backup",
                        subtitulo = "Guardar copia en formato .json",
                        icono = Icons.Default.Add,
                        tonoIcono = PaletaRetro90s.naranja,
                        habilitada = !cargando,
                        alHacerClic = {
                            scope.launch { drawerState.close() }
                            val fechaHoy = SimpleDateFormat("yyyy-MM-dd", localeColombia()).format(Date())
                            selectorArchivoGuardar.launch("respaldo_cadena_$fechaHoy.json")
                        }
                    )
                )
            ),
            SeccionMenuAdmin(
                tituloSeccion = "CIERRE DE CICLO",
                opciones = listOf(
                    OpcionMenuAdmin(
                        id = "finalizar_limpiar_cadena",
                        titulo = "Finalizar y limpiar cadena",
                        subtitulo = "Abrir sección de cierre con doble confirmación",
                        icono = Icons.AutoMirrored.Filled.ExitToApp,
                        tonoIcono = PaletaRetro90s.rojo,
                        alHacerClic = { navegarA(VistaAdminActiva.AJUSTES_CADENA) }
                    )
                )
            )
        )
    }

    // ========================================================================
    // BLOQUE 5: ESTRUCTURA VISUAL PRINCIPAL (SCAFFOLD Y LISTA DE PENDIENTES)
    // Dibuja la barra superior, la tarjeta de quien recibe, el botón de cerrar
    // quincena (cuando todo está al día) y la lista de miembros pendientes.
    // ========================================================================
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            MenuLateralAdministrador(secciones = seccionesMenu)
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = PaletaRetro90s.fondoCrema,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Abrir menú",
                                tint = PaletaRetro90s.textoOscuro
                            )
                        }
                    },
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Quincena $quincenaActual",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = PaletaRetro90s.textoOscuro
                            )
                            Text(
                                text = "${puestos.size.coerceAtLeast(18)} puestos en total",
                                style = MaterialTheme.typography.labelMedium,
                                color = PaletaRetro90s.textoOscuro
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                compartirEstadoQuincena(
                                    nombreQuienRecibe,
                                    montoRecaudado,
                                    miembrosPagaronCount,
                                    todosLosMiembros.size
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir resumen",
                                tint = PaletaRetro90s.textoOscuro
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PaletaRetro90s.fondoCrema,
                        titleContentColor = PaletaRetro90s.textoOscuro
                    )
                )
            }
        ) { paddingValues ->
            if (cargando && puestos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PaletaRetro90s.vino)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "tarjeta_quincena") {
                        TarjetaRecibeQuincenaRetro90s(
                            quienRecibe = nombreQuienRecibe,
                            montoAporteRecibe = resumenQuienRecibe.montoTotal,
                            aporteRegistrado = resumenQuienRecibe.pagado,
                            onCambiarAporteRecibe = { marcado ->
                                viewModel.alternarPagoTitular(nombreQuienRecibe, marcado)
                            },
                            montoRecaudado = montoRecaudado,
                            progreso = progresoRecaudado,
                            pagaronCount = miembrosPagaronCount,
                            totalMiembrosCount = todosLosMiembros.size
                        )
                    }

                    if (mensajeEstado.isNotBlank()) {
                        item(key = "estado_nube") {
                            Text(
                                text = mensajeEstado,
                                style = MaterialTheme.typography.bodySmall,
                                color = PaletaRetro90s.textoOscuro,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    if (puedeCerrarQuincena) {
                        item(key = "boton_cerrar_quincena") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.ciruela),
                                border = BorderStroke(1.5.dp, PaletaRetro90s.naranja)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "¡Todos los aportes de la Quincena $quincenaActual están completos!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PaletaRetro90s.textoClaro
                                    )
                                    Button(
                                        onClick = { mostrarDialogoCerrarQuincena = true },
                                        enabled = !cargando,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PaletaRetro90s.amarillo,
                                            contentColor = PaletaRetro90s.textoOscuro
                                        )
                                    ) {
                                        Text(
                                            text = "Cerrar quincena y pasar a Quincena ${quincenaActual + 1}",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "encabezado_pendientes") {
                        Text(
                            text = "PENDIENTES · ${formatearPesos(valorCuotaBase)} POR PUESTO",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = PaletaRetro90s.textoOscuro,
                            modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                        )
                    }

                    items(
                        items = miembrosPendientes,
                        key = { miembro -> miembro.titular.uppercase(localeColombia()) }
                    ) { miembro ->
                        FilaMiembroPagoRetro90s(
                            miembro = miembro,
                            onAlternarPago = {
                                viewModel.alternarPagoTitular(miembro.titular, true)
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ============================================================================
// BLOQUE 6: COMPONENTES REUTILIZABLES Y FUNCIONES DE APOYO
// Piezas visuales reutilizables (tarjeta superior, fila de miembro, menú lateral)
// y funciones auxiliares para formato de moneda, idioma y nombres de archivos.
// ============================================================================

@Composable
private fun TarjetaRecibeQuincenaRetro90s(
    quienRecibe: String,
    montoAporteRecibe: Int,
    aporteRegistrado: Boolean,
    onCambiarAporteRecibe: (Boolean) -> Unit,
    montoRecaudado: Int,
    progreso: Float,
    pagaronCount: Int,
    totalMiembrosCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.vino),
        border = BorderStroke(1.5.dp, PaletaRetro90s.naranja)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "RECIBE ESTA QUINCENA",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = PaletaRetro90s.amarillo
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = quienRecibe.uppercase(localeColombia()),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = PaletaRetro90s.textoClaro,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = PaletaRetro90s.ciruela,
                border = BorderStroke(1.dp, PaletaRetro90s.naranja)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Registrar su aporte (${formatearPesos(montoAporteRecibe)})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = PaletaRetro90s.textoClaro,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = aporteRegistrado,
                        onCheckedChange = onCambiarAporteRecibe,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PaletaRetro90s.textoOscuro,
                            checkedTrackColor = PaletaRetro90s.amarillo,
                            uncheckedThumbColor = PaletaRetro90s.textoClaro,
                            uncheckedTrackColor = PaletaRetro90s.vino
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "RECAUDADO",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = PaletaRetro90s.amarillo
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = formatearPesos(montoRecaudado),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = PaletaRetro90s.textoClaro
            )

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape),
                color = PaletaRetro90s.amarillo,
                trackColor = PaletaRetro90s.ciruela
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$pagaronCount de $totalMiembrosCount ya pagaron",
                style = MaterialTheme.typography.bodyMedium,
                color = PaletaRetro90s.textoClaro
            )
        }
    }
}

@Composable
private fun FilaMiembroPagoRetro90s(
    miembro: ResumenTitularQuincena,
    onAlternarPago: () -> Unit
) {
    val inicial = miembro.titular.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val textoPuestos = if (miembro.cantidadPuestos == 1) "1 puesto" else "${miembro.cantidadPuestos} puestos"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onAlternarPago() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.fondoCrema),
        border = BorderStroke(1.2.dp, PaletaRetro90s.naranja)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PaletaRetro90s.amarillo),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = inicial,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PaletaRetro90s.textoOscuro
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = miembro.titular.uppercase(localeColombia()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PaletaRetro90s.textoOscuro,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${formatearPesos(miembro.montoTotal)} · $textoPuestos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaletaRetro90s.textoOscuro
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (miembro.pagado) PaletaRetro90s.rojo else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (miembro.pagado) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Pagado",
                        tint = PaletaRetro90s.textoClaro,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.5.dp, PaletaRetro90s.rojo)
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun MenuLateralAdministrador(
    secciones: List<SeccionMenuAdmin>
) {
    ModalDrawerSheet(
        modifier = Modifier.width(305.dp),
        drawerContainerColor = PaletaRetro90s.fondoCrema,
        drawerContentColor = PaletaRetro90s.textoOscuro
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Panel del Administrador",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PaletaRetro90s.vino
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Vistas, miembros, control y respaldo",
                style = MaterialTheme.typography.bodySmall,
                color = PaletaRetro90s.textoOscuro
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = PaletaRetro90s.naranja
            )

            secciones.forEach { seccion ->
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = seccion.tituloSeccion,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = PaletaRetro90s.textoOscuro,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))

                seccion.opciones.forEach { opcion ->
                    val fondoItem = when {
                        opcion.resaltada -> PaletaRetro90s.amarillo
                        opcion.habilitada -> PaletaRetro90s.amarillo.copy(alpha = 0.28f)
                        else -> Color.Transparent
                    }

                    NavigationDrawerItem(
                        label = {
                            Column {
                                Text(
                                    text = opcion.titulo,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (opcion.resaltada) FontWeight.Bold else FontWeight.SemiBold,
                                    color = PaletaRetro90s.textoOscuro
                                )
                                Text(
                                    text = opcion.subtitulo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PaletaRetro90s.textoOscuro
                                )
                            }
                        },
                        selected = opcion.resaltada,
                        onClick = { if (opcion.habilitada) opcion.alHacerClic() },
                        icon = {
                            Icon(
                                imageVector = opcion.icono,
                                contentDescription = null,
                                tint = if (opcion.habilitada) {
                                    opcion.tonoIcono
                                } else {
                                    PaletaRetro90s.ciruela
                                }
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = fondoItem,
                            selectedContainerColor = fondoItem
                        ),
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }
    }
}

// Crea una configuración regional para español de Colombia compatible con API 26+
private fun localeColombia(): Locale {
    return Locale.Builder().setLanguage("es").setRegion("CO").build()
}

// Convierte un número entero (ej. 50000) a formato de moneda ("$50.000")
private fun formatearPesos(valor: Int): String {
    val formato = NumberFormat.getNumberInstance(localeColombia())
    return "$${formato.format(valor)}"
}

// Busca el nombre visible de un archivo a partir de su Uri en Android
private fun obtenerNombreArchivoDesdeUri(context: Context, uri: Uri): String {
    var nombre = "archivo_respaldo.json"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val indiceNombre = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (indiceNombre != -1 && cursor.moveToFirst()) {
            nombre = cursor.getString(indiceNombre) ?: nombre
        }
    }
    return nombre
}