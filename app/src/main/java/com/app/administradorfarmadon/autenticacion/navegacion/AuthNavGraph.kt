package com.app.administradorfarmadon.autenticacion.navegacion

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.administradorfarmadon.autenticacion.expediente.logica.ConsultarExpedienteViewModel
import com.app.administradorfarmadon.autenticacion.expediente.ui.ConsultarExpedienteScreen
import com.app.administradorfarmadon.autenticacion.login.logica.LoginViewModel
import com.app.administradorfarmadon.autenticacion.login.ui.LoginScreen
import com.app.administradorfarmadon.autenticacion.registro.contenedor.logica.RegistroFarmaciaViewModel
import com.app.administradorfarmadon.autenticacion.registro.contenedor.ui.RegistroFarmaciaScreen
import com.app.administradorfarmadon.disenotemaapp.ui.ThemeViewModel

@Composable
fun AuthNavGraph(
    loginViewModel: LoginViewModel,
    themeViewModel: ThemeViewModel,
    onRegistroExitoso: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable(
            route = "login?email={email}",
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val emailPrellenado = backStackEntry.arguments?.getString("email")
            LoginScreen(
                viewModel = loginViewModel,
                themeViewModel = themeViewModel,
                emailPrellenado = emailPrellenado,
                onNavigateToRegistro = { navController.navigate("registro") },
                onNavigateToRegistroCorrection = { uid ->
                    navController.navigate("registro?uid=$uid")
                },
                onNavigateToExpediente = { navController.navigate("consultar_expediente") }
            )
        }

        composable(
            route = "registro?uid={uid}",
            arguments = listOf(
                navArgument("uid") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid")
            val registroViewModel: RegistroFarmaciaViewModel = viewModel()

            LaunchedEffect(uid) {
                if (uid != null) {
                    registroViewModel.precargarSolicitudCorreccion(uid)
                }
            }

            RegistroFarmaciaScreen(
                viewModel = registroViewModel,
                onBackClick = { navController.popBackStack() },
                onRegistroExitoso = {
                    onRegistroExitoso()
                },
                onNavigateToLogin = { email ->
                    navController.navigate("login?email=${Uri.encode(email.orEmpty())}") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("consultar_expediente") {
            val expedienteViewModel: ConsultarExpedienteViewModel = viewModel()
            ConsultarExpedienteScreen(
                viewModel = expedienteViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToCorregir = { uid ->
                    navController.navigate("registro?uid=$uid")
                }
            )
        }
    }
}
