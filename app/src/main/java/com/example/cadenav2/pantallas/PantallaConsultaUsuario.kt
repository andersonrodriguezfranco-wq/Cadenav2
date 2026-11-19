package com.example.cadenav2.pantallas

// ============================================================================
// BLOQUE 1: IMPORTACIONES Y CONFIGURACIÓN DEL ARCHIVO
// Reúne únicamente los componentes de Jetpack Compose, Material 3, íconos,
// la paleta retro de los 90 y el ViewModel para consultar los datos en vivo.
// ============================================================================

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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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

// ============================================================================
// BLOQUE 2: PANTALLA PRINCIPAL DE CONSULTA Y CÁLCULOS DE CUENTA
// Filtra los puestos del familiar consultado, calcula su aporte quincenal,
// revisa si está al día en la quincena activa y prepara los datos de
// solo lectura para evitar cualquier modificación indebida desde la consulta.
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConsultaUsuario(
    viewModel: CadenaViewModel,
    nombreFamiliar: String,
    modifier: Modifier = Modifier,
    celularFamiliar: String = "",
    onAbrirCambioClave: () -> Unit = {},
    onCerrarSesion: () -> Unit = {}
) {
    // Observamos los estados reactivos principales expuestos por CadenaViewModel
    val puestos by viewModel.puestos.collectAsState()
    val quincenaActual by viewModel.quincenaActual.collectAsState()
    val valorCuotaBase by viewModel.valorCuotaBase.collectAsState()

    // Filtramos los registros de puestos correspondientes al familiar consultado
    val misRegistros = remember(puestos, nombreFamiliar) {
        puestos.filter { puesto ->
            puesto.titular.trim().equals(nombreFamiliar.trim(), ignoreCase = true)
        }
    }

    // Calculamos el total de puestos comprados por el familiar
    val totalMisPuestos = remember(misRegistros) {
        if (misRegistros.isEmpty()) {
            0
        } else {
            val sumaExplicita = misRegistros.sumOf { it.puestosComprados }
            if (misRegistros.size > 1 && misRegistros.all { it.puestosComprados == 1 }) {
                misRegistros.size
            } else {
                sumaExplicita.coerceAtLeast(misRegistros.size)
            }
        }
    }

    // Calculamos el aporte que le corresponde pagar al familiar en esta quincena
    val miAporteQuincenal = remember(misRegistros, totalMisPuestos, valorCuotaBase) {
        if (misRegistros.isEmpty()) {
            0
        } else {
            val cuotaUnitaria = misRegistros.first().valorCuotaIndividual.takeIf { it > 0 }
                ?: valorCuotaBase
            totalMisPuestos * cuotaUnitaria
        }
    }

    // Verificamos si todos los puestos del familiar están marcados como pagados hoy
    val estaAlDia = remember(misRegistros) {
        misRegistros.isNotEmpty() && misRegistros.all { it.pagadoQuincenaActual }
    }

    // Extraemos y ordenamos los números de quincena en los que el familiar recibe el pozo
    val misTurnos = remember(misRegistros) {
        misRegistros.map { it.numeroPuesto }.distinct().sorted()
    }

    // Revisamos si el turno de recibir el pozo coincide con la quincena activa
    val recibeEstaQuincena = remember(misTurnos, quincenaActual) {
        misTurnos.contains(quincenaActual)
    }

    // Identificamos quién recibe el pozo en la quincena actual para dar contexto
    val titularQueRecibeHoy = remember(puestos, quincenaActual) {
        puestos.firstOrNull { it.numeroPuesto == quincenaActual }
            ?.titular
            ?.ifBlank { "Sin asignar" }
            ?: "Por definir"
    }

    // Calculamos el progreso de pagos de toda la cadena en la quincena activa
    val totalPuestosCadena = remember(puestos) {
        puestos.sumOf { it.puestosComprados.coerceAtLeast(1) }
    }
    val puestosPagadosCadena = remember(puestos) {
        puestos.filter { it.pagadoQuincenaActual }
            .sumOf { it.puestosComprados.coerceAtLeast(1) }
    }
    val progresoRecaudacion = remember(puestosPagadosCadena, totalPuestosCadena) {
        if (totalPuestosCadena > 0) {
            (puestosPagadosCadena.toFloat() / totalPuestosCadena.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    // ========================================================================
    // BLOQUE 3: ESTRUCTURA VISUAL (SCAFFOLD Y BARRA SUPERIOR)
    // Configura el contenedor principal con el fondo crema retro de los 90 y la
    // barra superior con accesos para cambiar contraseña o cerrar sesión.
    // ========================================================================
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PaletaRetro90s.fondoCrema,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mi Cuenta",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PaletaRetro90s.textoOscuro
                        )
                        Text(
                            text = "Modo consulta · Solo lectura",
                            style = MaterialTheme.typography.labelMedium,
                            color = PaletaRetro90s.vino
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAbrirCambioClave) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Cambiar contraseña",
                            tint = PaletaRetro90s.textoOscuro
                        )
                    }
                    IconButton(onClick = onCerrarSesion) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = PaletaRetro90s.rojo
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ================================================================
            // BLOQUE 4: TARJETA DE BIENVENIDA Y ESTADO QUINCENAL DEL USUARIO
            // Resume el nombre, celular, quincena actual, aporte a pagar, estado
            // de pago (PAGADO / PENDIENTE) y aviso si recibe el pozo hoy.
            // ================================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.amarillo),
                    border = BorderStroke(1.5.dp, PaletaRetro90s.naranja)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hola, ${nombreFamiliar.ifBlank { "Familiar" }}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PaletaRetro90s.textoOscuro,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (celularFamiliar.isNotBlank()) {
                                    Text(
                                        text = "Celular: $celularFamiliar",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PaletaRetro90s.textoOscuro
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PaletaRetro90s.vino)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Quincena $quincenaActual",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PaletaRetro90s.textoClaro
                                )
                            }
                        }

                        HorizontalDivider(color = PaletaRetro90s.naranja.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tu aporte esta quincena",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PaletaRetro90s.textoOscuro
                                )
                                Text(
                                    text = formatearPesosConsulta(miAporteQuincenal),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PaletaRetro90s.textoOscuro
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            val colorFondoEstado = if (estaAlDia) {
                                PaletaRetro90s.ciruela
                            } else {
                                PaletaRetro90s.rojo
                            }
                            val textoEstado = if (misRegistros.isEmpty()) {
                                "Sin puestos"
                            } else if (estaAlDia) {
                                "PAGADO"
                            } else {
                                "PENDIENTE"
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colorFondoEstado)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (estaAlDia) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.Schedule
                                    },
                                    contentDescription = textoEstado,
                                    tint = PaletaRetro90s.textoClaro,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = textoEstado,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PaletaRetro90s.textoClaro
                                )
                            }
                        }

                        if (recibeEstaQuincena) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PaletaRetro90s.naranja)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Esta quincena es tu turno de recibir el pozo.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PaletaRetro90s.textoClaro
                                )
                            }
                        }
                    }
                }
            }

            // ================================================================
            // BLOQUE 5: RESUMEN DE PUESTOS, DETALLE POR TURNO Y AVANCE GENERAL
            // Presenta los puestos del familiar, su listado individual, quién
            // recibe en la quincena activa y el porcentaje general de pagos.
            // ================================================================
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "RESUMEN DE TUS PUESTOS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = PaletaRetro90s.vino
                        )

                        FilaDatoConsultaUsuario(
                            etiqueta = "Puestos comprados",
                            valor = "$totalMisPuestos puesto(s)"
                        )
                        FilaDatoConsultaUsuario(
                            etiqueta = "Valor por puesto",
                            valor = formatearPesosConsulta(valorCuotaBase)
                        )
                        FilaDatoConsultaUsuario(
                            etiqueta = "Turno(s) para recibir",
                            valor = if (misTurnos.isEmpty()) {
                                "Sin turno asignado"
                            } else {
                                misTurnos.joinToString(", ") { "Quincena #$it" }
                            }
                        )
                    }
                }
            }

            if (misRegistros.isNotEmpty()) {
                item {
                    Text(
                        text = "Detalle por puesto asignado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PaletaRetro90s.textoOscuro
                    )
                }

                items(
                    items = misRegistros,
                    key = { puesto -> puesto.id.ifBlank { "${puesto.titular}_${puesto.numeroPuesto}" } }
                ) { puesto ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.fondoCrema),
                        border = BorderStroke(1.dp, PaletaRetro90s.naranja)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(PaletaRetro90s.vino),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${puesto.numeroPuesto}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = PaletaRetro90s.textoClaro
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Turno Quincena ${puesto.numeroPuesto}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PaletaRetro90s.textoOscuro
                                    )
                                    Text(
                                        text = "Cantidad: ${puesto.puestosComprados} puesto(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PaletaRetro90s.textoOscuro
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = if (puesto.pagadoQuincenaActual) "Al día" else "Pendiente",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (puesto.pagadoQuincenaActual) {
                                    PaletaRetro90s.ciruela
                                } else {
                                    PaletaRetro90s.rojo
                                }
                            )
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.fondoCrema),
                        border = BorderStroke(1.dp, PaletaRetro90s.rojo)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Sin registros",
                                tint = PaletaRetro90s.rojo
                            )
                            Text(
                                text = "Aún no tienes puestos registrados a tu nombre en esta cadena.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PaletaRetro90s.textoOscuro
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PaletaRetro90s.fondoCrema),
                    border = BorderStroke(1.2.dp, PaletaRetro90s.ciruela)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ESTADO GENERAL DE LA QUINCENA $quincenaActual",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = PaletaRetro90s.ciruela
                        )

                        FilaDatoConsultaUsuario(
                            etiqueta = "Recibe en esta quincena",
                            valor = titularQueRecibeHoy
                        )

                        FilaDatoConsultaUsuario(
                            etiqueta = "Puestos al día en el grupo",
                            valor = "$puestosPagadosCadena de $totalPuestosCadena"
                        )

                        LinearProgressIndicator(
                            progress = { progresoRecaudacion },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = PaletaRetro90s.vino,
                            trackColor = PaletaRetro90s.amarillo
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedButton(
                            onClick = onAbrirCambioClave,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.2.dp, PaletaRetro90s.vino),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PaletaRetro90s.textoOscuro
                            )
                        ) {
                            Text(
                                text = "Cambiar mi contraseña",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// BLOQUE 6: COMPONENTES AUXILIARES DE APOYO Y FORMATEO DE MONEDA
// Proporciona filas de lectura alineadas y el formato de moneda en pesos
// colombianos para mantener limpia la pantalla principal de consulta.
// ============================================================================

@Composable
private fun FilaDatoConsultaUsuario(
    etiqueta: String,
    valor: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = PaletaRetro90s.textoOscuro
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = PaletaRetro90s.textoOscuro
        )
    }
}

private fun localeColombiaConsulta(): Locale {
    return Locale.Builder().setLanguage("es").setRegion("CO").build()
}

private fun formatearPesosConsulta(valor: Int): String {
    val formato = NumberFormat.getNumberInstance(localeColombiaConsulta())
    return "$${formato.format(valor)}"
}