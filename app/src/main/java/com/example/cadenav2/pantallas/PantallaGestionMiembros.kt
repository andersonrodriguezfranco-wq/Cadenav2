package com.example.cadenav2.pantallas

// ============================================================================
// BLOQUE 1: IMPORTACIONES Y TIPOS INTERNOS
// Importamos solo los componentes de Compose y Material 3 que usamos aquí,
// y definimos las pestañas disponibles para gestionar familiares.
// ============================================================================

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cadenav2.ui.theme.PaletaRetro90s
import com.example.cadenav2.viewmodel.CadenaViewModel
import java.text.NumberFormat
import java.util.Locale

// Pestañas internas para alternar entre las 4 acciones sobre familiares
private enum class ModoGestionMiembro(val tituloPestana: String) {
    AGREGAR("Agregar"),
    EDITAR("Editar"),
    INTERCAMBIAR("Turnos"),
    ELIMINAR("Eliminar")
}

// ============================================================================
// BLOQUE 2: PANTALLA PRINCIPAL Y ESTADOS DE FORMULARIOS
// Leemos los datos del ViewModel y guardamos en variables de estado lo que
// el usuario escribe o selecciona en cada pestaña.
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaGestionMiembros(
    viewModel: CadenaViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    modoInicial: String = "AGREGAR"
) {
    // Escuchamos los puestos actuales y el valor base de la cuota desde el ViewModel
    val puestos by viewModel.puestos.collectAsState()
    val valorCuotaBase by viewModel.valorCuotaBase.collectAsState()

    // Determinamos qué pestaña abrir al entrar según el parámetro recibido
    val pestanaInicial = remember(modoInicial) {
        when (modoInicial.uppercase(localeColombia())) {
            "EDITAR" -> ModoGestionMiembro.EDITAR
            "INTERCAMBIAR", "TURNOS" -> ModoGestionMiembro.INTERCAMBIAR
            "ELIMINAR" -> ModoGestionMiembro.ELIMINAR
            else -> ModoGestionMiembro.AGREGAR
        }
    }

    // Pestaña seleccionada en este momento
    var modoActual by remember(pestanaInicial) { mutableStateOf(pestanaInicial) }

    // Lista de titulares únicos sin duplicar si alguien compró más de un puesto
    val titularesUnicos = remember(puestos) {
        puestos
            .filter { it.titular.isNotBlank() }
            .groupBy { it.titular.trim().uppercase(localeColombia()) }
            .map { (_, registros) -> registros.first().titular.trim() }
    }

    // Estados para la pestaña "Agregar"
    var nombreNuevo by remember { mutableStateOf("") }
    var puestosCompradosNuevo by remember { mutableStateOf("1") }
    var numeroPuestoTurnoNuevo by remember { mutableStateOf("1") }

    // Estados para la pestaña "Editar"
    var titularSeleccionadoEditar by remember { mutableStateOf("") }
    var nombreEditado by remember { mutableStateOf("") }
    var puestosEditados by remember { mutableStateOf("1") }

    // Estados para la pestaña "Turnos" (intercambiar quincenas entre dos familiares)
    var titularOrigenTurno by remember { mutableStateOf("") }
    var titularDestinoTurno by remember { mutableStateOf("") }

    // Estado para confirmar eliminación de un familiar
    var titularAEliminar by remember { mutableStateOf<String?>(null) }

    // Mensaje de ayuda o confirmación para mostrar al usuario
    var avisoLocal by remember { mutableStateOf("") }

    // ========================================================================
    // BLOQUE 3: DIÁLOGO DE CONFIRMACIÓN PARA ELIMINAR
    // Evita borrar a un familiar por accidente pidiendo confirmación explícita.
    // ========================================================================
    titularAEliminar?.let { personaBorrar ->
        AlertDialog(
            onDismissRequest = { titularAEliminar = null },
            containerColor = PaletaRetro90s.fondoCrema,
            title = {
                Text(
                    text = "Eliminar familiar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PaletaRetro90s.textoOscuro
                )
            },
            text = {
                Text(
                    text = "¿Seguro que deseas eliminar a \"$personaBorrar\" de los puestos de la cadena?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PaletaRetro90s.textoOscuro
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarFamiliar(personaBorrar)
                        avisoLocal = "Se solicitó eliminar a $personaBorrar."
                        titularAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaletaRetro90s.rojo,
                        contentColor = PaletaRetro90s.textoClaro
                    )
                ) {
                    Text(text = "Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { titularAEliminar = null }) {
                    Text(text = "Cancelar", color = PaletaRetro90s.textoOscuro)
                }
            }
        )
    }

    // ========================================================================
    // BLOQUE 4: ESTRUCTURA VISUAL Y PESTAÑAS DE GESTIÓN
    // Dibuja la barra superior, las pestañas (Agregar, Editar, Turnos, Eliminar)
    // y el formulario o lista correspondiente a cada acción.
    // ========================================================================
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PaletaRetro90s.fondoCrema,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = PaletaRetro90s.textoOscuro
                        )
                    }
                },
                title = {
                    Text(
                        text = "Gestión de miembros",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PaletaRetro90s.textoOscuro
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaletaRetro90s.fondoCrema,
                    titleContentColor = PaletaRetro90s.textoOscuro
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Fila con las pestañas de acción
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ModoGestionMiembro.entries) { modo ->
                        val seleccionada = modo == modoActual
                        FilterChip(
                            selected = seleccionada,
                            onClick = {
                                modoActual = modo
                                avisoLocal = ""
                            },
                            label = {
                                Text(
                                    text = modo.tituloPestana,
                                    fontWeight = FontWeight.Bold,
                                    color = if (seleccionada) {
                                        PaletaRetro90s.textoClaro
                                    } else {
                                        PaletaRetro90s.textoOscuro
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = PaletaRetro90s.fondoCrema,
                                selectedContainerColor = PaletaRetro90s.vino,
                                labelColor = PaletaRetro90s.textoOscuro,
                                selectedLabelColor = PaletaRetro90s.textoClaro
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = seleccionada,
                                borderColor = PaletaRetro90s.naranja,
                                selectedBorderColor = PaletaRetro90s.ciruela
                            )
                        )
                    }
                }
            }

            // Tarjeta de aviso si hay algún mensaje o validación
            if (avisoLocal.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.amarillo),
                        border = BorderStroke(1.dp, PaletaRetro90s.naranja)
                    ) {
                        Text(
                            text = avisoLocal,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PaletaRetro90s.textoOscuro,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Contenido según la pestaña activa
            when (modoActual) {
                ModoGestionMiembro.AGREGAR -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.fondoCrema),
                            border = BorderStroke(1.2.dp, PaletaRetro90s.naranja)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "AGREGAR NUEVO FAMILIAR",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PaletaRetro90s.vino
                                )

                                CampoTextoRetro90s(
                                    valor = nombreNuevo,
                                    onValorCambio = { nombreNuevo = it },
                                    etiqueta = "Nombre del familiar",
                                    tecladoNumerico = false
                                )

                                CampoTextoRetro90s(
                                    valor = puestosCompradosNuevo,
                                    onValorCambio = { puestosCompradosNuevo = it },
                                    etiqueta = "Cantidad de puestos comprados",
                                    tecladoNumerico = true
                                )

                                CampoTextoRetro90s(
                                    valor = numeroPuestoTurnoNuevo,
                                    onValorCambio = { numeroPuestoTurnoNuevo = it },
                                    etiqueta = "Número de quincena / turno asignado",
                                    tecladoNumerico = true
                                )

                                val cantidadEntera = puestosCompradosNuevo.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                val aporteEstimado = cantidadEntera * valorCuotaBase

                                Text(
                                    text = "Aporte quincenal estimado: ${formatearPesos(aporteEstimado)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PaletaRetro90s.textoOscuro
                                )

                                Button(
                                    onClick = {
                                        val nombreLimpio = nombreNuevo.trim()
                                        val turnoEntero = numeroPuestoTurnoNuevo.toIntOrNull() ?: 1
                                        if (nombreLimpio.isBlank()) {
                                            avisoLocal = "Escribe el nombre del familiar antes de guardar."
                                        } else {
                                            viewModel.guardarFamiliar(
                                                titular = nombreLimpio,
                                                puestosComprados = cantidadEntera,
                                                numeroPuesto = turnoEntero.coerceAtLeast(1),
                                                valorCuota = valorCuotaBase
                                            )
                                            avisoLocal = "Guardado: $nombreLimpio ($cantidadEntera puesto(s), Q#$turnoEntero)."
                                            nombreNuevo = ""
                                            puestosCompradosNuevo = "1"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PaletaRetro90s.vino,
                                        contentColor = PaletaRetro90s.textoClaro
                                    )
                                ) {
                                    Text(text = "Guardar familiar", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                ModoGestionMiembro.EDITAR -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.fondoCrema),
                            border = BorderStroke(1.2.dp, PaletaRetro90s.naranja)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "EDITAR FAMILIAR EXISTENTE",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PaletaRetro90s.vino
                                )

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(titularesUnicos) { persona ->
                                        val activa = persona.equals(titularSeleccionadoEditar, ignoreCase = true)
                                        FilterChip(
                                            selected = activa,
                                            onClick = {
                                                titularSeleccionadoEditar = persona
                                                nombreEditado = persona
                                                val puestoRef = puestos.find {
                                                    it.titular.equals(persona, ignoreCase = true)
                                                }
                                                puestosEditados = (puestoRef?.puestosComprados ?: 1).toString()
                                            },
                                            label = {
                                                Text(
                                                    text = persona,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = if (activa) {
                                                        PaletaRetro90s.textoClaro
                                                    } else {
                                                        PaletaRetro90s.textoOscuro
                                                    }
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = PaletaRetro90s.fondoCrema,
                                                selectedContainerColor = PaletaRetro90s.ciruela
                                            )
                                        )
                                    }
                                }

                                CampoTextoRetro90s(
                                    valor = nombreEditado,
                                    onValorCambio = { nombreEditado = it },
                                    etiqueta = "Nuevo nombre del titular",
                                    tecladoNumerico = false
                                )

                                CampoTextoRetro90s(
                                    valor = puestosEditados,
                                    onValorCambio = { puestosEditados = it },
                                    etiqueta = "Cantidad de puestos",
                                    tecladoNumerico = true
                                )

                                Button(
                                    onClick = {
                                        val anterior = titularSeleccionadoEditar.trim()
                                        val nuevoNombre = nombreEditado.trim()
                                        val nuevaCantidad = puestosEditados.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                        if (anterior.isBlank() || nuevoNombre.isBlank()) {
                                            avisoLocal = "Selecciona un familiar y escribe un nombre válido."
                                        } else {
                                            viewModel.editarFamiliar(
                                                titularAnterior = anterior,
                                                nuevoTitular = nuevoNombre,
                                                puestosComprados = nuevaCantidad,
                                                valorCuota = valorCuotaBase
                                            )
                                            titularSeleccionadoEditar = nuevoNombre
                                            avisoLocal = "Actualizado: $nuevoNombre ($nuevaCantidad puesto(s))."
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PaletaRetro90s.ciruela,
                                        contentColor = PaletaRetro90s.textoClaro
                                    )
                                ) {
                                    Text(text = "Guardar cambios", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                ModoGestionMiembro.INTERCAMBIAR -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.fondoCrema),
                            border = BorderStroke(1.2.dp, PaletaRetro90s.naranja)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "INTERCAMBIAR TURNOS DE QUINCENA",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PaletaRetro90s.vino
                                )

                                Text(
                                    text = "Elige dos familiares para intercambiar la quincena en la que reciben el pozo:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PaletaRetro90s.textoOscuro
                                )

                                Text(
                                    text = "Primer familiar: ${titularOrigenTurno.ifBlank { "Ninguno seleccionado" }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PaletaRetro90s.textoOscuro
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(titularesUnicos) { persona ->
                                        val sel = persona.equals(titularOrigenTurno, ignoreCase = true)
                                        FilterChip(
                                            selected = sel,
                                            onClick = { titularOrigenTurno = persona },
                                            label = {
                                                Text(
                                                    text = persona,
                                                    color = if (sel) PaletaRetro90s.textoClaro else PaletaRetro90s.textoOscuro
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = PaletaRetro90s.fondoCrema,
                                                selectedContainerColor = PaletaRetro90s.vino
                                            )
                                        )
                                    }
                                }

                                Text(
                                    text = "Segundo familiar: ${titularDestinoTurno.ifBlank { "Ninguno seleccionado" }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PaletaRetro90s.textoOscuro
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(titularesUnicos) { persona ->
                                        val sel = persona.equals(titularDestinoTurno, ignoreCase = true)
                                        FilterChip(
                                            selected = sel,
                                            onClick = { titularDestinoTurno = persona },
                                            label = {
                                                Text(
                                                    text = persona,
                                                    color = if (sel) PaletaRetro90s.textoClaro else PaletaRetro90s.textoOscuro
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = PaletaRetro90s.fondoCrema,
                                                selectedContainerColor = PaletaRetro90s.rojo
                                            )
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (titularOrigenTurno.isBlank() || titularDestinoTurno.isBlank()) {
                                            avisoLocal = "Selecciona ambos familiares para intercambiar turnos."
                                        } else if (titularOrigenTurno.equals(titularDestinoTurno, ignoreCase = true)) {
                                            avisoLocal = "Debes elegir dos familiares distintos."
                                        } else {
                                            viewModel.intercambiarTurnos(
                                                primerTitular = titularOrigenTurno,
                                                segundoTitular = titularDestinoTurno
                                            )
                                            avisoLocal = "Turnos intercambiados entre $titularOrigenTurno y $titularDestinoTurno."
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PaletaRetro90s.vino,
                                        contentColor = PaletaRetro90s.textoClaro
                                    )
                                ) {
                                    Text(text = "Intercambiar turnos", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                ModoGestionMiembro.ELIMINAR -> {
                    items(titularesUnicos, key = { it }) { persona ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.fondoCrema),
                            border = BorderStroke(1.2.dp, PaletaRetro90s.rojo)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = persona,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PaletaRetro90s.textoOscuro,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedButton(
                                    onClick = { titularAEliminar = persona },
                                    border = BorderStroke(1.2.dp, PaletaRetro90s.rojo),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = PaletaRetro90s.textoOscuro
                                    )
                                ) {
                                    Text(text = "Eliminar", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// BLOQUE 5: COMPONENTES REUTILIZABLES Y FUNCIONES DE APOYO
// Campo de texto con el estilo de la paleta de los 90s y funciones para
// formatear moneda y construir Locale sin advertencias de versión de Android.
// ============================================================================

@Composable
private fun CampoTextoRetro90s(
    valor: String,
    onValorCambio: (String) -> Unit,
    etiqueta: String,
    tecladoNumerico: Boolean
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorCambio,
        label = {
            Text(
                text = etiqueta,
                color = PaletaRetro90s.textoOscuro
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (tecladoNumerico) KeyboardType.Number else KeyboardType.Text
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PaletaRetro90s.textoOscuro,
            unfocusedTextColor = PaletaRetro90s.textoOscuro,
            focusedBorderColor = PaletaRetro90s.vino,
            unfocusedBorderColor = PaletaRetro90s.naranja,
            cursorColor = PaletaRetro90s.vino
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// Crea Locale("es", "CO") compatible con API 26 y versiones superiores
private fun localeColombia(): Locale {
    return Locale.Builder().setLanguage("es").setRegion("CO").build()
}

// Convierte un número entero (ej. 50000) a formato de pesos ("$50.000")
private fun formatearPesos(valor: Int): String {
    val formato = NumberFormat.getNumberInstance(localeColombia())
    return "$${formato.format(valor)}"
}