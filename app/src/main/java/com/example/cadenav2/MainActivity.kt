package com.example.cadenav2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.cadenav2.pantallas.PantallaAdministracion
import com.example.cadenav2.ui.theme.CadenaFamiliarTheme
import com.example.cadenav2.ui.theme.PaletaRetro90s
import com.example.cadenav2.viewmodel.CadenaViewModel

/**
 * Actividad principal de la aplicación con el tema noventero de los 90s.
 */
class MainActivity : ComponentActivity() {

    // Instancia del ViewModel vinculada al ciclo de vida de MainActivity
    private val cadenaViewModel: CadenaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Habilita el dibujo edge-to-edge con soporte para barras del sistema
        enableEdgeToEdge()

        setContent {
            // Aplicamos el tema de la paleta retro de los 90s a toda la jerarquía
            CadenaFamiliarTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = PaletaRetro90s.fondoCrema
                ) { innerPadding ->
                    PantallaAdministracion(
                        viewModel = cadenaViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}