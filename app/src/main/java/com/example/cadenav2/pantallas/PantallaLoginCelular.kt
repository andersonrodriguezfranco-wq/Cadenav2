package com.example.cadenav2.pantallas

// ============================================================================
// BLOQUE 1: IMPORTACIONES Y CONFIGURACIÓN DEL ARCHIVO
// Reúne componentes de Compose, Material 3, íconos, vista previa (@Preview),
// la paleta retro de los 90 y el ViewModel de la cadena.
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cadenav2.ui.theme.PaletaRetro90s
import com.example.cadenav2.viewmodel.CadenaViewModel


// ============================================================================
// BLOQUE 2: PUNTO DE ENTRADA CON VIEWMODEL
// Observa la sesión activa y el estado de carga desde CadenaViewModel y
// conecta las acciones de login, cambio obligatorio de clave y cierre.
// ============================================================================

@Composable
fun PantallaLoginCelular(
    viewModel: CadenaViewModel,
    modifier: Modifier = Modifier
) {
    // Observa la sesión actual para saber si hay cambio obligatorio de clave pendiente
    val sesionActiva by viewModel.sesionActiva.collectAsState()
    val cargando by viewModel.cargando.collectAsState()

    ContenidoLoginCelular(
        sesionActiva = sesionActiva,
        cargando = cargando,
        modifier = modifier,
        onIngresar = { celular, clave, alTerminar ->
            viewModel.iniciarSesionPorCelular(
                celularIngresado = celular,
                claveIngresada = clave,
                onResultado = alTerminar
            )
        },
        onGuardarNuevaClave = { nuevaClave, alTerminar ->
            viewModel.completarCambioClaveObligatorio(
                nuevaClave = nuevaClave,
                onResultado = alTerminar
            )
        },
        onCancelarCambio = {
            viewModel.cerrarSesion()
        }
    )
}

// ============================================================================
// BLOQUE 3: CONTENIDO VISUAL (LOGIN POR CELULAR Y CAMBIO OBLIGATORIO DE CLAVE)
// Muestra el formulario de ingreso por número de celular + contraseña o, si
// sesionActiva.debeCambiarClave es true, el formulario obligatorio de clave.
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContenidoLoginCelular(
    sesionActiva: CadenaViewModel.SesionActiva?,
    cargando: Boolean,
    modifier: Modifier = Modifier,
    onIngresar: (String, String, (Boolean, String) -> Unit) -> Unit = { _, _, _ -> },
    onGuardarNuevaClave: (String, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    onCancelarCambio: () -> Unit = {}
) {
    // Determina si la sesión actual exige cambiar la contraseña inicial
    val modoCambioObligatorio = sesionActiva != null && sesionActiva.debeCambiarClave

    // Estados de los campos de inicio de sesión
    var celularTexto by remember { mutableStateOf("") }
    var claveTexto by remember { mutableStateOf("") }
    var mostrarClaveLogin by remember { mutableStateOf(false) }

    // Estados de los campos de cambio obligatorio de contraseña
    var nuevaClaveTexto by remember { mutableStateOf("") }
    var confirmarClaveTexto by remember { mutableStateOf("") }
    var mostrarNuevaClave by remember { mutableStateOf(false) }

    // Mensaje de validación o respuesta del intento actual
    var mensajeFeedback by remember { mutableStateOf("") }
    var esError by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PaletaRetro90s.fondoCrema,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (modoCambioObligatorio) "Cambio de clave" else "Cadena Familiar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PaletaRetro90s.textoOscuro
                        )
                        Text(
                            text = if (modoCambioObligatorio) {
                                "Primer ingreso · Obligatorio"
                            } else {
                                "Acceso por número de celular"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = PaletaRetro90s.vino
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(16.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta superior destacada con estética retro de los 90
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
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PaletaRetro90s.vino)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (modoCambioObligatorio) "SEGURIDAD" else "BIENVENIDO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PaletaRetro90s.textoClaro
                        )
                    }

                    Text(
                        text = if (modoCambioObligatorio) {
                            "Hola, ${sesionActiva?.nombreMostrado.orEmpty()}"
                        } else {
                            "Ingresa con tu celular"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = PaletaRetro90s.textoOscuro
                    )

                    Text(
                        text = if (modoCambioObligatorio) {
                            "Es tu primer ingreso con clave inicial. Crea una nueva contraseña para continuar."
                        } else {
                            "Tu número de teléfono celular es tu usuario para consultar o administrar la cadena."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaletaRetro90s.textoOscuro
                    )
                }
            }

            if (!modoCambioObligatorio) {
                // ============================================================
                // BLOQUE 3A: FORMULARIO DE LOGIN POR CELULAR Y CONTRASEÑA
                // ============================================================
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
                            text = "INICIAR SESIÓN",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = PaletaRetro90s.vino
                        )

                        OutlinedTextField(
                            value = celularTexto,
                            onValueChange = {
                                celularTexto = it
                                mensajeFeedback = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Número de celular") },
                            placeholder = { Text("Ej. 3001234567") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = "Celular",
                                    tint = PaletaRetro90s.vino
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PaletaRetro90s.vino,
                                unfocusedBorderColor = PaletaRetro90s.naranja,
                                focusedTextColor = PaletaRetro90s.textoOscuro,
                                unfocusedTextColor = PaletaRetro90s.textoOscuro
                            )
                        )

                        OutlinedTextField(
                            value = claveTexto,
                            onValueChange = {
                                claveTexto = it
                                mensajeFeedback = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Contraseña") },
                            placeholder = { Text("Ingresa tu clave") },
                            singleLine = true,
                            visualTransformation = if (mostrarClaveLogin) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Contraseña",
                                    tint = PaletaRetro90s.vino
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { mostrarClaveLogin = !mostrarClaveLogin }) {
                                    Icon(
                                        imageVector = if (mostrarClaveLogin) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = "Mostrar u ocultar contraseña",
                                        tint = PaletaRetro90s.textoOscuro
                                    )
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PaletaRetro90s.vino,
                                unfocusedBorderColor = PaletaRetro90s.naranja,
                                focusedTextColor = PaletaRetro90s.textoOscuro,
                                unfocusedTextColor = PaletaRetro90s.textoOscuro
                            )
                        )

                        if (mensajeFeedback.isNotBlank()) {
                            HorizontalDivider(color = PaletaRetro90s.naranja.copy(alpha = 0.4f))
                            Text(
                                text = mensajeFeedback,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (esError) PaletaRetro90s.rojo else PaletaRetro90s.ciruela
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                onIngresar(celularTexto, claveTexto) { ok, msg ->
                                    esError = !ok
                                    mensajeFeedback = msg
                                    if (ok) {
                                        claveTexto = ""
                                    }
                                }
                            },
                            enabled = !cargando,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PaletaRetro90s.vino,
                                contentColor = PaletaRetro90s.textoClaro
                            )
                        ) {
                            Text(
                                text = if (cargando) "Validando..." else "Ingresar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // ============================================================
                // BLOQUE 3B: FORMULARIO DE CAMBIO OBLIGATORIO DE CONTRASEÑA
                // ============================================================
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "ESTABLECER NUEVA CONTRASEÑA",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = PaletaRetro90s.ciruela
                        )

                        OutlinedTextField(
                            value = nuevaClaveTexto,
                            onValueChange = {
                                nuevaClaveTexto = it
                                mensajeFeedback = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nueva contraseña (mín. 4 caracteres)") },
                            singleLine = true,
                            visualTransformation = if (mostrarNuevaClave) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Nueva contraseña",
                                    tint = PaletaRetro90s.ciruela
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { mostrarNuevaClave = !mostrarNuevaClave }) {
                                    Icon(
                                        imageVector = if (mostrarNuevaClave) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = "Mostrar u ocultar nueva contraseña",
                                        tint = PaletaRetro90s.textoOscuro
                                    )
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PaletaRetro90s.ciruela,
                                unfocusedBorderColor = PaletaRetro90s.naranja,
                                focusedTextColor = PaletaRetro90s.textoOscuro,
                                unfocusedTextColor = PaletaRetro90s.textoOscuro
                            )
                        )

                        OutlinedTextField(
                            value = confirmarClaveTexto,
                            onValueChange = {
                                confirmarClaveTexto = it
                                mensajeFeedback = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirmar nueva contraseña") },
                            singleLine = true,
                            visualTransformation = if (mostrarNuevaClave) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Confirmar contraseña",
                                    tint = PaletaRetro90s.ciruela
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PaletaRetro90s.ciruela,
                                unfocusedBorderColor = PaletaRetro90s.naranja,
                                focusedTextColor = PaletaRetro90s.textoOscuro,
                                unfocusedTextColor = PaletaRetro90s.textoOscuro
                            )
                        )

                        if (mensajeFeedback.isNotBlank()) {
                            HorizontalDivider(color = PaletaRetro90s.naranja.copy(alpha = 0.4f))
                            Text(
                                text = mensajeFeedback,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (esError) PaletaRetro90s.rojo else PaletaRetro90s.ciruela
                            )
                        }

                        Button(
                            onClick = {
                                // Verifica que ambas contraseñas coincidan antes de guardar
                                if (nuevaClaveTexto.trim() != confirmarClaveTexto.trim()) {
                                    esError = true
                                    mensajeFeedback = "Las contraseñas no coinciden."
                                    return@Button
                                }
                                onGuardarNuevaClave(nuevaClaveTexto) { ok, msg ->
                                    esError = !ok
                                    mensajeFeedback = msg
                                    if (ok) {
                                        nuevaClaveTexto = ""
                                        confirmarClaveTexto = ""
                                    }
                                }
                            },
                            enabled = !cargando,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PaletaRetro90s.ciruela,
                                contentColor = PaletaRetro90s.textoClaro
                            )
                        ) {
                            Text(
                                text = if (cargando) "Guardando..." else "Guardar nueva contraseña",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                nuevaClaveTexto = ""
                                confirmarClaveTexto = ""
                                mensajeFeedback = ""
                                onCancelarCambio()
                            },
                            enabled = !cargando,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.2.dp, PaletaRetro90s.vino),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PaletaRetro90s.textoOscuro
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Salir y cambiar después",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// BLOQUE 4: VISTAS PREVIAS (@PREVIEW)
// Permite inspeccionar en Android Studio tanto el login por celular como el
// modo de cambio obligatorio de clave en el primer ingreso.
// ============================================================================

@Preview(showBackground = true, name = "Login por Celular")
@Composable
private fun PreviewPantallaLoginCelular() {
    ContenidoLoginCelular(
        sesionActiva = null,
        cargando = false
    )
}

@Preview(showBackground = true, name = "Cambio Obligatorio de Clave")
@Composable
private fun PreviewCambioClaveObligatorio() {
    ContenidoLoginCelular(
        sesionActiva = CadenaViewModel.SesionActiva(
            rol = CadenaViewModel.RolUsuario.USUARIO,
            nombreMostrado = "Maye",
            celular = "3001234567",
            debeCambiarClave = true
        ),
        cargando = false
    )
}