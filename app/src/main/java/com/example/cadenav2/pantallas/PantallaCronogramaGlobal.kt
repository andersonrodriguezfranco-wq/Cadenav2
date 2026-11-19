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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cadenav2.ui.theme.PaletaRetro90s
import com.example.cadenav2.viewmodel.CadenaViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Representa una fila del cronograma: qué número de quincena es,
 * quién recibe el pozo en ese turno, cuánto recibe y en qué estado está.
 */
private data class FilaTurnoCronograma(
    val numeroQuincena: Int,
    val titular: String,
    val montoPozo: Int,
    val estado: EstadoTurno
)

/**
 * Posibles estados de una quincena dentro del cronograma completo.
 */
private enum class EstadoTurno {
    COMPLETADO, // Quincena que ya pasó
    ACTUAL,     // Quincena que se está jugando ahora mismo
    PENDIENTE   // Quincena futura
}

/**
 * Pantalla que muestra el cronograma global de turnos de toda la cadena,
 * usando la paleta noventera de los 90s y textos fáciles de leer.
 */
@OptIn(ExperimentalMaterial3Api::class) // Habilita componentes Material 3 como TopAppBar
@Composable
fun PantallaCronogramaGlobal(
    viewModel: CadenaViewModel, // Fuente de datos de la cadena (puestos, quincena actual, cuotas)
    onVolver: () -> Unit, // Acción al pulsar la flecha de regresar
    modifier: Modifier = Modifier // Modificador opcional desde el contenedor padre
) {
    // Observamos los flujos de datos del ViewModel para que la UI reaccione a cambios
    val puestos by viewModel.puestos.collectAsState()
    val quincenaActual by viewModel.quincenaActual.collectAsState()
    val valorCuotaBase by viewModel.valorCuotaBase.collectAsState()

    // Calculamos el monto total que se reúne en una quincena (el pozo que recibe el titular del turno)
    val montoPozoQuincena = remember(puestos, valorCuotaBase) {
        puestos
            .filter { it.titular.isNotBlank() } // Solo contamos puestos que tengan un titular asignado
            .groupBy { it.titular.trim().uppercase(Locale.getDefault()) } // Agrupamos por titular para no repetir
            .values
            .sumOf { puestosDelTitular ->
                val principal = puestosDelTitular.first()
                val cantidad = principal.puestosComprados.coerceAtLeast(1)
                val cuota = principal.valorCuotaIndividual.takeIf { it > 0 } ?: valorCuotaBase
                if (principal.aporteTotalQuincenal > 0) {
                    principal.aporteTotalQuincenal
                } else {
                    cantidad * cuota
                }
            }
    }

    // Construimos la lista ordenada de turnos (Quincena 1, Quincena 2, ...)
    val listaTurnos = remember(puestos, quincenaActual, montoPozoQuincena) {
        // Cantidad total de turnos a mostrar (al menos 18 o la cantidad de puestos registrados)
        val totalTurnos = puestos.size.coerceAtLeast(18)

        // Ordenamos los puestos por su número de puesto para asignar cada turno en orden
        val puestosOrdenados = puestos.sortedBy { it.numeroPuesto }

        // Creamos un elemento por cada número de quincena del 1 al totalTurnos
        (1..totalTurnos).map { numeroTurno ->
            // Buscamos el puesto asignado a este turno; si no hay exacto, tomamos el de la posición
            val puestoAsignado = puestosOrdenados.find { it.numeroPuesto == numeroTurno }
                ?: puestosOrdenados.getOrNull(numeroTurno - 1)

            // Nombre del titular que cobra en esta quincena
            val nombreTitular = puestoAsignado?.titular?.trim()?.ifBlank { "Puesto disponible" }
                ?: "Puesto disponible"

            // Determinamos si este turno ya pasó, es el actual o viene después
            val estadoTurno = when {
                numeroTurno < quincenaActual -> EstadoTurno.COMPLETADO
                numeroTurno == quincenaActual -> EstadoTurno.ACTUAL
                else -> EstadoTurno.PENDIENTE
            }

            FilaTurnoCronograma(
                numeroQuincena = numeroTurno,
                titular = nombreTitular,
                montoPozo = montoPozoQuincena,
                estado = estadoTurno
            )
        }
    }

    // Scaffold organiza la barra superior y el cuerpo de la pantalla
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PaletaRetro90s.fondoCrema, // Fondo crema de la paleta de los 90s
        topBar = {
            TopAppBar(
                // Botón para volver atrás
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = PaletaRetro90s.textoOscuro
                        )
                    }
                },
                // Título y subtítulo de la pantalla
                title = {
                    Column {
                        Text(
                            text = "Cronograma global",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PaletaRetro90s.textoOscuro
                        )
                        Text(
                            text = "Turno actual: Quincena $quincenaActual",
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
        // LazyColumn dibuja únicamente las tarjetas visibles en pantalla para mayor fluidez
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = listaTurnos,
                key = { turno -> turno.numeroQuincena } // Clave única por quincena
            ) { turno ->
                TarjetaTurnoCronograma(turno = turno)
            }

            // Espacio al final para que la última tarjeta no quede pegada al borde inferior
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

/**
 * Tarjeta individual que muestra una quincena, quién recibe y una etiqueta con su estado.
 */
@Composable
private fun TarjetaTurnoCronograma(
    turno: FilaTurnoCronograma
) {
    // Elegimos colores de tarjeta, insignia y texto según si el turno es pasado, actual o futuro
    val (colorTarjeta, colorBorde, colorBadge, textoBadge, colorTextoBadge) = when (turno.estado) {
        EstadoTurno.ACTUAL -> {
            // Quincena activa: resalta con vino de los 90s y texto claro
            QuintuplaEstiloTurno(
                tarjeta = PaletaRetro90s.vino,
                borde = PaletaRetro90s.naranja,
                badge = PaletaRetro90s.amarillo,
                etiqueta = "RECIBE HOY",
                textoBadge = PaletaRetro90s.textoOscuro
            )
        }
        EstadoTurno.COMPLETADO -> {
            // Turno ya cumplido: fondo crema con borde naranja y etiqueta naranja/ciruela
            QuintuplaEstiloTurno(
                tarjeta = PaletaRetro90s.fondoCrema,
                borde = PaletaRetro90s.ciruela,
                badge = PaletaRetro90s.naranja,
                etiqueta = "COBRÓ",
                textoBadge = PaletaRetro90s.textoClaro
            )
        }
        EstadoTurno.PENDIENTE -> {
            // Turno futuro: fondo crema con borde rojo y etiqueta amarilla de fácil lectura
            QuintuplaEstiloTurno(
                tarjeta = PaletaRetro90s.fondoCrema,
                borde = PaletaRetro90s.rojo,
                badge = PaletaRetro90s.amarillo,
                etiqueta = "PENDIENTE",
                textoBadge = PaletaRetro90s.textoOscuro
            )
        }
    }

    // Color principal de texto dentro de la tarjeta (claro si la tarjeta es oscura, oscuro si es crema)
    val colorTextoPrincipal = if (turno.estado == EstadoTurno.ACTUAL) {
        PaletaRetro90s.textoClaro
    } else {
        PaletaRetro90s.textoOscuro
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colorTarjeta),
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
                // Círculo con el número de quincena (ej. 1, 2, 3...)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (turno.estado == EstadoTurno.ACTUAL) {
                                PaletaRetro90s.amarillo
                            } else {
                                PaletaRetro90s.ciruela
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Q${turno.numeroQuincena}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (turno.estado == EstadoTurno.ACTUAL) {
                            PaletaRetro90s.textoOscuro
                        } else {
                            PaletaRetro90s.textoClaro
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Nombre del titular y valor estimado del pozo
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = turno.titular.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorTextoPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Pozo estimado: ${formatearPesos(turno.montoPozo)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorTextoPrincipal
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Etiqueta pequeña a la derecha indicando el estado del turno
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colorBadge
            ) {
                Text(
                    text = textoBadge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorTextoBadge,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }
    }
}

/**
 * Pequeña estructura auxiliar para devolver los 5 colores/textos del estado de una fila.
 */
private data class QuintuplaEstiloTurno(
    val tarjeta: Color,
    val borde: Color,
    val badge: Color,
    val etiqueta: String,
    val textoBadge: Color
)

/**
 * Formatea un entero como moneda en pesos colombianos (ejemplo: 50000 -> "$50.000").
 */
private fun formatearPesos(valor: Int): String {
    val formato = NumberFormat.getNumberInstance(Locale("es", "CO"))
    return "$${formato.format(valor)}"
}