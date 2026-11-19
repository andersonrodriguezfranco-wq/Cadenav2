package com.example.cadenav2.pantallas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cadenav2.ui.theme.PaletaRetro90s
import com.example.cadenav2.viewmodel.CadenaViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Modelo auxiliar para mostrar cada titular en la pantalla de corrección de pagos
 * de la quincena activa, junto con su cantidad de puestos, monto y estado actual.
 */
private data class EstadoPagoTitular(
    val titular: String,
    val cantidadPuestos: Int,
    val montoTotal: Int,
    val pagado: Boolean
)

/**
 * Pantalla para revisar y corregir pagos de la quincena actual.
 * Muestra a todos los titulares para que el administrador pueda marcar o desmarcar
 * un aporte sin que desaparezca de la lista mientras corrige.
 */
@OptIn(ExperimentalMaterial3Api::class) // Habilita componentes Material 3 como TopAppBar
@Composable
fun PantallaCorregirPagos(
    viewModel: CadenaViewModel, // Fuente de datos y acciones conectadas a Firestore
    onVolver: () -> Unit, // Acción ejecutada al tocar la flecha para regresar
    modifier: Modifier = Modifier // Modificador opcional desde la vista que llama a esta pantalla
) {
    // Observamos los estados reactivos del ViewModel:
    // "by collectAsState()" hace que la interfaz se actualice si cambian los datos en la nube
    val puestos by viewModel.puestos.collectAsState()
    val quincenaActual by viewModel.quincenaActual.collectAsState()
    val valorCuotaBase by viewModel.valorCuotaBase.collectAsState()

    // Agrupamos por titular para consolidar puestos y evitar sumar dos veces a la misma persona
    val listaTitulares = remember(puestos, valorCuotaBase) {
        puestos
            .filter { it.titular.isNotBlank() } // Filtramos filas sin titular asignado
            .groupBy { it.titular.trim().uppercase(Locale.getDefault()) } // Agrupamos por nombre sin importar mayúsculas
            .map { (_, puestosDelTitular) ->
                val principal = puestosDelTitular.first() // Primer registro como referencia del titular
                val nombreVisible = principal.titular.trim()
                val cantidad = principal.puestosComprados.coerceAtLeast(1)
                val cuotaUnitaria = principal.valorCuotaIndividual.takeIf { it > 0 } ?: valorCuotaBase
                val monto = if (principal.aporteTotalQuincenal > 0) {
                    principal.aporteTotalQuincenal
                } else {
                    cantidad * cuotaUnitaria
                }
                // Consideramos que ya pagó si todos sus puestos tienen marcado pagadoQuincenaActual
                val yaPago = puestosDelTitular.all { it.pagadoQuincenaActual }

                EstadoPagoTitular(
                    titular = nombreVisible,
                    cantidadPuestos = cantidad,
                    montoTotal = monto,
                    pagado = yaPago
                )
            }
            .sortedBy { it.titular.uppercase(Locale.getDefault()) } // Orden alfabético para ubicar rápido a alguien
    }

    // Scaffold organiza la barra superior y el contenido principal de la pantalla
    Scaffold(
        modifier = modifier.fillMaxSize(), // Ocupa todo el espacio de la pantalla
        containerColor = PaletaRetro90s.fondoCrema, // Fondo crema de la paleta de los 90s
        topBar = {
            TopAppBar(
                // Botón izquierdo para volver atrás
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = PaletaRetro90s.textoOscuro // Tinta oscura para buena lectura
                        )
                    }
                },
                // Título con el número de la quincena activa (ej. "Corregir pagos Q#6")
                title = {
                    Column {
                        Text(
                            text = "Corregir pagos Q#$quincenaActual",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PaletaRetro90s.textoOscuro
                        )
                        Text(
                            text = "Activa o desactiva aportes de la quincena",
                            style = MaterialTheme.typography.labelMedium,
                            color = PaletaRetro90s.textoOscuro
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
        // LazyColumn muestra los titulares en una lista vertical desplazable
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), // Respeta el alto de la barra superior
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp) // Separación entre tarjetas
        ) {
            // Tarjeta introductoria breve para guiar al administrador
            item(key = "aviso_correccion") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.vino),
                    border = BorderStroke(1.2.dp, PaletaRetro90s.naranja)
                ) {
                    Text(
                        text = "Usa el interruptor de cada familiar para corregir si un pago se marcó por error o faltaba registrar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaletaRetro90s.textoClaro, // Texto claro sobre fondo vino
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // Cada elemento representa a un familiar y permite cambiar su estado de pago
            items(
                items = listaTitulares,
                key = { item -> item.titular.uppercase(Locale.getDefault()) }
            ) { item ->
                FilaCorreccionPago(
                    item = item,
                    onCambiarPago = { nuevoEstado ->
                        // Notifica al ViewModel para actualizar el pago en Firestore
                        viewModel.alternarPagoTitular(item.titular, nuevoEstado)
                    }
                )
            }

            // Espacio inferior para comodidad al desplazarse hasta el final
            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }
}

/**
 * Fila individual con los datos del titular, su monto y un interruptor (Switch)
 * para marcar o desmarcar su pago en la quincena activa.
 */
@Composable
private fun FilaCorreccionPago(
    item: EstadoPagoTitular,
    onCambiarPago: (Boolean) -> Unit
) {
    // Tomamos la primera letra del nombre para el círculo inicial
    val inicial = item.titular.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val textoPuestos = if (item.cantidadPuestos == 1) "1 puesto" else "${item.cantidadPuestos} puestos"

    // El borde resalta si ya está marcado como pagado o si sigue pendiente
    val colorBorde = if (item.pagado) PaletaRetro90s.ciruela else PaletaRetro90s.naranja

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.fondoCrema),
        border = BorderStroke(1.2.dp, colorBorde)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Círculo amarillo con la inicial del titular
                Box(
                    modifier = Modifier
                        .size(42.dp)
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

                // Nombre, monto y estado actual ("Pagado" o "Pendiente")
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.titular.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PaletaRetro90s.textoOscuro,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${formatearPesos(item.montoTotal)} · $textoPuestos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaletaRetro90s.textoOscuro
                    )
                    Text(
                        text = if (item.pagado) "Estado: Pagado" else "Estado: Pendiente",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.pagado) PaletaRetro90s.vino else PaletaRetro90s.rojo
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Interruptor para alternar entre Pagado y Pendiente
            Switch(
                checked = item.pagado,
                onCheckedChange = onCambiarPago,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PaletaRetro90s.textoOscuro,
                    checkedTrackColor = PaletaRetro90s.amarillo,
                    uncheckedThumbColor = PaletaRetro90s.textoClaro,
                    uncheckedTrackColor = PaletaRetro90s.ciruela
                )
            )
        }
    }
}

/**
 * Convierte un número entero (ej. 50000) a formato de pesos colombianos ("$50.000").
 */
private fun formatearPesos(valor: Int): String {
    val formato = NumberFormat.getNumberInstance(Locale("es", "CO"))
    return "$${formato.format(valor)}"
}