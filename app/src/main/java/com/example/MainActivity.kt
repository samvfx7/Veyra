package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AppDatabase
import com.example.data.ProjectRepository
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.EditorViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HomeViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VeyraApp()
                }
            }
        }
    }
}

@Composable
fun VeyraApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { ProjectRepository(database.projectDao()) }
    
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val homeViewModel = remember { HomeViewModel(repository) }
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToEditor = { projectId ->
                    navController.navigate("editor/$projectId")
                }
            )
        }
        
        composable(
            route = "editor/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            val editorViewModel = remember(projectId) { EditorViewModel(repository, projectId) }
            
            EditorScreen(
                viewModel = editorViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
