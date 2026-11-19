package com.example.cadenav2.pantallas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.cadenav2.ui.theme.PaletaRetro90s
import com.example.cadenav2.viewmodel.CadenaViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Pantalla de configuración general y cierre de ciclo de la cadena.
 * Permite editar parámetros (cuota y quincena activa) y ejecutar el cierre
 * total con doble confirmación por seguridad.
 */
@OptIn(ExperimentalMaterial3Api::class) // Habilita componentes Material 3 como TopAppBar
@Composable
fun PantallaAjustesCadena(
    viewModel: CadenaViewModel, // ViewModel con los datos de la cadena y acciones hacia Firestore
    onVolver: () -> Unit, // Función que se ejecuta al tocar la flecha de regresar
    modifier: Modifier = Modifier // Modificador opcional desde el contenedor padre
) {
    // Leemos los valores actuales desde el ViewModel usando collectAsState()
    val quincenaActual by viewModel.quincenaActual.collectAsState()
    val valorCuotaBase by viewModel.valorCuotaBase.collectAsState()

    // Estados locales para editar el valor de la cuota y el número de quincena.
    // "remember(valorCuotaBase)" reinicia el texto si cambia el valor en el ViewModel.
    var textoCuota by remember(valorCuotaBase) {
        mutableStateOf(valorCuotaBase.toString())
    }
    var textoQuincena by remember(quincenaActual) {
        mutableStateOf(quincenaActual.toString())
    }

    // Mensaje de aviso o confirmación al guardar cambios
    var mensajeAjustes by remember { mutableStateOf("") }

    // Paso 1 de seguridad: primer cuadro de advertencia
    var mostrarPrimeraConfirmacion by remember { mutableStateOf(false) }

    // Paso 2 de seguridad: exige escribir "LIMPIAR" para confirmar
    var mostrarSegundaConfirmacion by remember { mutableStateOf(false) }
    var palabraConfirmacion by remember { mutableStateOf("") }

    // --- Primer diálogo de confirmación para finalizar y limpiar cadena ---
    if (mostrarPrimeraConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarPrimeraConfirmacion = false },
            containerColor = PaletaRetro90s.fondoCrema,
            title = {
                Text(
                    text = "Finalizar y limpiar cadena",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PaletaRetro90s.textoOscuro
                )
            },
            text = {
                Text(
                    text = "Esta acción cerrará el ciclo actual y limpiará los registros de la cadena.\n\nTe recomendamos exportar un respaldo (.json) antes de continuar. ¿Deseas pasar al segundo paso de confirmación?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PaletaRetro90s.textoOscuro
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarPrimeraConfirmacion = false
                        palabraConfirmacion = ""
                        mostrarSegundaConfirmacion = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaletaRetro90s.rojo,
                        contentColor = PaletaRetro90s.textoClaro
                    )
                ) {
                    Text(text = "Continuar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarPrimeraConfirmacion = false }) {
                    Text(text = "Cancelar", color = PaletaRetro90s.textoOscuro)
                }
            }
        )
    }

    // --- Segundo diálogo de confirmación (escribir LIMPIAR) ---
    if (mostrarSegundaConfirmacion) {
        // El botón final solo se habilita si el usuario escribe exactamente LIMPIAR
        val palabraValida = palabraConfirmacion.trim() == "LIMPIAR"

        AlertDialog(
            onDismissRequest = {
                mostrarSegundaConfirmacion = false
                palabraConfirmacion = ""
            },
            containerColor = PaletaRetro90s.fondoCrema,
            title = {
                Text(
                    text = "Confirmación final",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PaletaRetro90s.rojo
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Escribe LIMPIAR en mayúsculas para confirmar que deseas finalizar y limpiar toda la cadena:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaletaRetro90s.textoOscuro
                    )
                    OutlinedTextField(
                        value = palabraConfirmacion,
                        onValueChange = { palabraConfirmacion = it },
                        label = {
                            Text(
                                text = "Escribe LIMPIAR",
                                color = PaletaRetro90s.textoOscuro
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PaletaRetro90s.textoOscuro,
                            unfocusedTextColor = PaletaRetro90s.textoOscuro,
                            focusedBorderColor = PaletaRetro90s.rojo,
                            unfocusedBorderColor = PaletaRetro90s.naranja,
                            cursorColor = PaletaRetro90s.rojo
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Llama a la función del ViewModel para limpiar la cadena
                        viewModel.finalizarYLimpiarCadena()
                        mostrarSegundaConfirmacion = false
                        palabraConfirmacion = ""
                        mensajeAjustes = "Se ejecutó finalizar y limpiar cadena."
                    },
                    enabled = palabraValida,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaletaRetro90s.rojo,
                        contentColor = PaletaRetro90s.textoClaro
                    )
                ) {
                    Text(text = "Finalizar y limpiar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarSegundaConfirmacion = false
                        palabraConfirmacion = ""
                    }
                ) {
                    Text(text = "Cancelar", color = PaletaRetro90s.textoOscuro)
                }
            }
        )
    }

    // Estructura principal con barra superior y lista vertical desplazable
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
                        text = "Ajustes de la cadena",
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
            // Muestra un mensaje cuando se guardan ajustes o hay algún aviso
            if (mensajeAjustes.isNotBlank()) {
                item(key = "aviso_ajustes") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.amarillo),
                        border = BorderStroke(1.dp, PaletaRetro90s.naranja)
                    ) {
                        Text(
                            text = mensajeAjustes,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PaletaRetro90s.textoOscuro,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Tarjeta 1: Edición de cuota y quincena activa
            item(key = "tarjeta_ajustes") {
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
                            text = "AJUSTES GENERALES",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = PaletaRetro90s.vino
                        )

                        Text(
                            text = "Cuota actual por puesto: ${formatearPesos(valorCuotaBase)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PaletaRetro90s.textoOscuro
                        )

                        OutlinedTextField(
                            value = textoCuota,
                            onValueChange = { textoCuota = it },
                            label = {
                                Text(
                                    text = "Valor de cuota por puesto (COP)",
                                    color = PaletaRetro90s.textoOscuro
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PaletaRetro90s.textoOscuro,
                                unfocusedTextColor = PaletaRetro90s.textoOscuro,
                                focusedBorderColor = PaletaRetro90s.vino,
                                unfocusedBorderColor = PaletaRetro90s.naranja,
                                cursorColor = PaletaRetro90s.vino
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = textoQuincena,
                            onValueChange = { textoQuincena = it },
                            label = {
                                Text(
                                    text = "Número de quincena activa",
                                    color = PaletaRetro90s.textoOscuro
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PaletaRetro90s.textoOscuro,
                                unfocusedTextColor = PaletaRetro90s.textoOscuro,
                                focusedBorderColor = PaletaRetro90s.vino,
                                unfocusedBorderColor = PaletaRetro90s.naranja,
                                cursorColor = PaletaRetro90s.vino
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                // Convertimos los textos a números enteros (Int)
                                val nuevaCuota = textoCuota.trim().toIntOrNull()
                                val nuevaQuincena = textoQuincena.trim().toIntOrNull()

                                if (nuevaCuota == null || nuevaCuota <= 0) {
                                    mensajeAjustes = "Ingresa un valor de cuota mayor a cero."
                                } else if (nuevaQuincena == null || nuevaQuincena <= 0) {
                                    mensajeAjustes = "Ingresa un número de quincena válido (1 o mayor)."
                                } else {
                                    // Guarda los cambios en el ViewModel
                                    viewModel.guardarAjustesCadena(
                                        nuevoValorCuota = nuevaCuota,
                                        nuevaQuincenaActual = nuevaQuincena
                                    )
                                    mensajeAjustes = "Ajustes guardados: cuota ${formatearPesos(nuevaCuota)} · Quincena $nuevaQuincena."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PaletaRetro90s.vino,
                                contentColor = PaletaRetro90s.textoClaro
                            )
                        ) {
                            Text(text = "Guardar ajustes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tarjeta 2: Cierre total / limpieza de la cadena con doble confirmación
            item(key = "tarjeta_finalizar_cadena") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.ciruela),
                    border = BorderStroke(1.5.dp, PaletaRetro90s.rojo)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "CIERRE DE CICLO",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = PaletaRetro90s.amarillo
                        )

                        Text(
                            text = "Finalizar y limpiar cadena",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PaletaRetro90s.textoClaro
                        )

                        Text(
                            text = "Usa esta opción solo al terminar por completo un ciclo de la cadena. Se pedirá doble confirmación.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PaletaRetro90s.textoClaro
                        )

                        OutlinedButton(
                            onClick = { mostrarPrimeraConfirmacion = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, PaletaRetro90s.amarillo),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PaletaRetro90s.textoClaro
                            )
                        ) {
                            Text(
                                text = "Finalizar y limpiar cadena",
                                fontWeight = FontWeight.Bold,
                                color = PaletaRetro90s.amarillo
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }
}

/**
 * Convierte un número entero (ej. 50000) a formato de pesos colombianos ("$50.000").
 * Usa Locale.of("es", "CO") para evitar el constructor marcado como obsoleto.
 */
private fun formatearPesos(valor: Int): String {
    val formato = NumberFormat.getNumberInstance(Locale.of("es", "CO"))
    return "$${formato.format(valor)}"
}