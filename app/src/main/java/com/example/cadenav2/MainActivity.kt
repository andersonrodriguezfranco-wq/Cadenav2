package com.example.cadenav2

// ============================================================================
// BLOQUE 1: IMPORTACIONES Y ACTIVIDAD PRINCIPAL (MAINACTIVITY)
// Configura la actividad principal en Kotlin y Jetpack Compose (sin XML),
// habilita edge-to-edge y conecta el enrutador de sesiones con CadenaViewModel.
// ============================================================================

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.cadenav2.pantallas.ContenidoLoginCelular
import com.example.cadenav2.pantallas.PantallaAdministracion
import com.example.cadenav2.pantallas.PantallaConsultaUsuario
import com.example.cadenav2.pantallas.PantallaLoginCelular
import com.example.cadenav2.ui.theme.PaletaRetro90s
import com.example.cadenav2.viewmodel.CadenaViewModel

class MainActivity : ComponentActivity() {

    // Obtiene la instancia del ViewModel compartida durante el ciclo de vida de la actividad
    private val cadenaViewModel: CadenaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permite que el contenido ocupe el ancho completo bajo las barras del sistema
        enableEdgeToEdge()

        setContent {
            TemaCadenaExpressive90s {
                EnrutadorCadena(
                    viewModel = cadenaViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ============================================================================
// BLOQUE 2: TEMA MATERIAL DESIGN 3 EXPRESSIVE (PALETA RETRO DE LOS 90)
// Aplica la paleta PaletaRetro90s sobre MaterialTheme para mantener una
// estética coherente en login, consulta de usuario y administración.
// ============================================================================

@Composable
fun TemaCadenaExpressive90s(
    content: @Composable () -> Unit
) {
    // Mapea los colores retro de los 90 sobre el esquema de color de Material 3
    val esquemaColores = MaterialTheme.colorScheme.copy(
        primary = PaletaRetro90s.vino,
        onPrimary = PaletaRetro90s.textoClaro,
        primaryContainer = PaletaRetro90s.amarillo,
        onPrimaryContainer = PaletaRetro90s.textoOscuro,
        secondary = PaletaRetro90s.naranja,
        onSecondary = PaletaRetro90s.textoClaro,
        secondaryContainer = PaletaRetro90s.ciruela,
        onSecondaryContainer = PaletaRetro90s.textoClaro,
        background = PaletaRetro90s.fondoCrema,
        onBackground = PaletaRetro90s.textoOscuro,
        surface = PaletaRetro90s.fondoCrema,
        onSurface = PaletaRetro90s.textoOscuro,
        error = PaletaRetro90s.rojo,
        onError = PaletaRetro90s.textoClaro
    )

    MaterialTheme(
        colorScheme = esquemaColores,
        content = content
    )
}

// ============================================================================
// BLOQUE 3: ENRUTADOR PRINCIPAL POR SESIÓN Y ROL
// Observa sesionActiva desde CadenaViewModel y decide qué pantalla mostrar:
// 1) Sin sesión o con cambio obligatorio de clave -> PantallaLoginCelular
// 2) Familiar autenticado -> PantallaConsultaUsuario (modo solo lectura)
// 3) Administrador autenticado -> PantallaAdministracion (con menú lateral y
//    titulares unificados)
// ============================================================================

@Composable
fun EnrutadorCadena(
    viewModel: CadenaViewModel,
    modifier: Modifier = Modifier
) {
    // Escucha el estado de sesión en tiempo real
    val sesionActual by viewModel.sesionActiva.collectAsState()

    // Si hay sesión activa, presionar atrás cierra la sesión de forma limpia
    BackHandler(enabled = sesionActual != null) {
        viewModel.cerrarSesion()
    }

    val sesion = sesionActual
    when {
        // Caso 1: Usuario no autenticado o con cambio obligatorio de clave pendiente
        sesion == null || sesion.debeCambiarClave -> {
            PantallaLoginCelular(
                viewModel = viewModel,
                modifier = modifier
            )
        }

        // Caso 2: Familiar autenticado con clave actualizada -> consulta en solo lectura
        sesion.rol == CadenaViewModel.RolUsuario.USUARIO -> {
            PantallaConsultaUsuario(
                viewModel = viewModel,
                nombreFamiliar = sesion.nombreMostrado,
                celularFamiliar = sesion.celular,
                modifier = modifier,
                onAbrirCambioClave = { viewModel.cerrarSesion() },
                onCerrarSesion = { viewModel.cerrarSesion() }
            )
        }

        // Caso 3: Administrador autenticado -> panel completo con barra lateral y titulares unificados
        sesion.rol == CadenaViewModel.RolUsuario.ADMIN -> {
            PantallaAdministracion(
                viewModel = viewModel,
                modifier = modifier,
                onCerrarSesion = { viewModel.cerrarSesion() }
            )
        }
    }
}

// ============================================================================
// BLOQUE 4: PREVISUALIZACIÓN (@PREVIEW)
// Permite previsualizar la pantalla inicial de ingreso con Material Design 3
// Expressive y la paleta retro de los 90 en el editor de Android Studio.
// ============================================================================

@Preview(showBackground = true, name = "Login Inicial - M3 Expressive Retro 90s")
@Composable
private fun PreviewMainLogin() {
    TemaCadenaExpressive90s {
        ContenidoLoginCelular(
            sesionActiva = null,
            cargando = false
        )
    }
}