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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cadenav2.ui.theme.PaletaRetro90s
import com.example.cadenav2.viewmodel.CadenaViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Pantalla que muestra el resumen general de todo el ciclo de la cadena.
 * Está pensada para verse clara y ordenada usando los colores de la paleta de los 90s.
 */
@OptIn(ExperimentalMaterial3Api::class) // Permite usar componentes experimentales de Material 3 como TopAppBar
@Composable
fun PantallaResumenCiclo(
    viewModel: CadenaViewModel, // Recibe el ViewModel, que guarda y entrega los datos de la app
    onVolver: () -> Unit, // Función que se ejecuta cuando el usuario toca la flecha para regresar
    modifier: Modifier = Modifier // Permite ajustar tamaño o márgenes desde fuera si hace falta
) {
    // Escuchamos los datos que vienen del ViewModel.
    // "by collectAsState()" hace que la pantalla se actualice sola cuando cambie un dato.
    val puestos by viewModel.puestos.collectAsState()
    val quincenaActual by viewModel.quincenaActual.collectAsState()
    val valorCuotaBase by viewModel.valorCuotaBase.collectAsState()

    // "remember" guarda estos cálculos y solo los vuelve a hacer si cambian "puestos" o "valorCuotaBase".
    // Así evitamos calcular lo mismo en cada redibujo de la pantalla.
    val totalPuestos = remember(puestos) {
        puestos.size.coerceAtLeast(18) // Si hay menos de 18 cargados, mostramos al menos 18 como base
    }

    val totalQuincenas = totalPuestos // En la cadena, cada puesto corresponde a una quincena del ciclo

    // Sumamos cuánto debería reunirse en una sola quincena según los miembros registrados
    val recaudadoPorQuincena = remember(puestos, valorCuotaBase) {
        puestos
            .filter { it.titular.isNotBlank() } // Ignoramos registros vacíos sin titular
            .groupBy { it.titular.trim().uppercase(Locale.getDefault()) } // Agrupamos por titular para no duplicar
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

    // Calculamos cuánto dinero se ha recogido en la quincena activa y cuánto va en el ciclo
    val recaudadoQuincenaActual = remember(puestos, valorCuotaBase) {
        puestos
            .filter { it.titular.isNotBlank() }
            .groupBy { it.titular.trim().uppercase(Locale.getDefault()) }
            .values
            .filter { puestosDelTitular -> puestosDelTitular.all { it.pagadoQuincenaActual } }
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

    // Quincenas ya cerradas antes de la quincena actual
    val quincenasCompletadas = (quincenaActual - 1).coerceAtLeast(0)

    // Estimación acumulada del ciclo: quincenas anteriores completas + lo recaudado hoy
    val totalAcumuladoCiclo = (quincenasCompletadas * recaudadoPorQuincena) + recaudadoQuincenaActual

    // Porcentaje de avance del ciclo (entre 0.0 y 1.0 para la barra de progreso)
    val avanceCiclo = if (totalQuincenas > 0) {
        (quincenaActual.coerceAtMost(totalQuincenas).toFloat() / totalQuincenas.toFloat())
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    // Scaffold organiza la estructura básica: barra superior + contenido principal
    Scaffold(
        modifier = modifier.fillMaxSize(), // Ocupa todo el alto y ancho disponible
        containerColor = PaletaRetro90s.fondoCrema, // Fondo crema de la paleta de los 90s
        topBar = {
            TopAppBar(
                // Botón izquierdo con ícono de flecha hacia atrás
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = PaletaRetro90s.textoOscuro // Color oscuro fácil de leer
                        )
                    }
                },
                // Título de la pantalla
                title = {
                    Text(
                        text = "Resumen del ciclo",
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
        // LazyColumn es una lista desplazable verticalmente; aquí ponemos tarjetas ordenadas
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), // Respeta el espacio de la barra superior
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp) // Separación uniforme entre tarjetas
        ) {
            // Primera tarjeta: muestra el estado general del ciclo y una barra de progreso
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.vino),
                    border = BorderStroke(1.5.dp, PaletaRetro90s.naranja)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "AVANCE GENERAL DE LA CADENA",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PaletaRetro90s.amarillo
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Quincena $quincenaActual de $totalQuincenas",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = PaletaRetro90s.textoClaro
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { avanceCiclo },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = PaletaRetro90s.amarillo,
                            trackColor = PaletaRetro90s.ciruela
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$quincenasCompletadas quincenas cerradas · $totalPuestos puestos en el ciclo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PaletaRetro90s.textoClaro
                        )
                    }
                }
            }

            // Segunda tarjeta: total acumulado estimado en lo que va del ciclo
            item {
                TarjetaDatoResumen(
                    etiqueta = "ACUMULADO DEL CICLO",
                    valor = formatearPesos(totalAcumuladoCiclo),
                    detalle = "Incluye quincenas cerradas y lo recaudado en la quincena $quincenaActual",
                    colorBorde = PaletaRetro90s.naranja
                )
            }

            // Tercera tarjeta: cuánto se reúne en cada quincena y cuál es la cuota base
            item {
                TarjetaDatoResumen(
                    etiqueta = "RECAUDO POR QUINCENA",
                    valor = formatearPesos(recaudadoPorQuincena),
                    detalle = "Cuota base: ${formatearPesos(valorCuotaBase)} por puesto",
                    colorBorde = PaletaRetro90s.rojo
                )
            }
        }
    }
}

/**
 * Componente reutilizable para mostrar una métrica importante con etiqueta, valor grande y nota.
 * Separar esto en una función pequeña evita repetir código de tarjetas.
 */
@Composable
private fun TarjetaDatoResumen(
    etiqueta: String,
    valor: String,
    detalle: String,
    colorBorde: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.fondoCrema),
        border = BorderStroke(1.2.dp, colorBorde)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = PaletaRetro90s.textoOscuro
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = PaletaRetro90s.textoOscuro
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodyMedium,
                color = PaletaRetro90s.textoOscuro
            )
        }
    }
}

/**
 * Convierte un número entero (por ejemplo 50000) en un texto con formato de pesos ("$50.000").
 */
private fun formatearPesos(valor: Int): String {
    val formato = NumberFormat.getNumberInstance(Locale("es", "CO"))
    return "$${formato.format(valor)}"
}