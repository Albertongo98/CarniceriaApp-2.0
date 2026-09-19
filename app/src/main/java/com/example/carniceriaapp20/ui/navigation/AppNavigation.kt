package com.example.carniceriaapp20.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.carniceriaapp20.ui.screens.generador.LabelGeneratorScreen
import com.example.carniceriaapp20.ui.screens.history.HistoryScreen
import com.example.carniceriaapp20.ui.screens.products.AddEditProductScreen
import com.example.carniceriaapp20.ui.screens.products.ProductListScreen
import com.example.carniceriaapp20.ui.screens.settings.SettingsScreen
import com.example.carniceriaapp20.ui.screens.tpv.TpvScreen
import com.example.carniceriaapp20.ui.screens.update.UpdateFromCsvScreen
import com.example.carniceriaapp20.ui.screens.splash.SplashScreen
import com.example.carniceriaapp20.ui.screens.reports.ReportsScreen

object Routes {
    const val SPLASH = "splash"
    const val TPV = "tpv"
    const val PRODUCT_LIST = "product_list"
    const val ADD_EDIT_PRODUCT = "add_edit_product"
    const val PRODUCT_CODE_ARG = "productCode"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val UPDATE_FROM_CSV = "update_from_csv"
    const val LABEL_GENERATOR = "label_generator"
    const val REPORTS = "reports"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(onTimeout = {
                navController.navigate(Routes.TPV) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }
        composable(Routes.TPV) {
            TpvScreen(navController = navController)
        }
        composable(Routes.PRODUCT_LIST) {
            ProductListScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddProduct = {
                    navController.navigate(Routes.ADD_EDIT_PRODUCT)
                },
                onEditProduct = { productCode ->
                    navController.navigate("${Routes.ADD_EDIT_PRODUCT}?${Routes.PRODUCT_CODE_ARG}=$productCode")
                }
            )
        }
        composable(
            route = "${Routes.ADD_EDIT_PRODUCT}?${Routes.PRODUCT_CODE_ARG}={${Routes.PRODUCT_CODE_ARG}}",
            arguments = listOf(navArgument(Routes.PRODUCT_CODE_ARG) {
                type = NavType.StringType
                nullable = true
            })
        ) {
            AddEditProductScreen(
                onSave = {
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.UPDATE_FROM_CSV) {
            UpdateFromCsvScreen()
        }
        composable(Routes.LABEL_GENERATOR) {
            LabelGeneratorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.REPORTS) {
            ReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
