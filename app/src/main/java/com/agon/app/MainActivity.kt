package com.agon.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agon.app.ui.screens.HomeScreen
import com.agon.app.ui.screens.ProfileScreen
import com.agon.app.ui.screens.SignInScreen
import com.agon.app.ui.screens.VideoPlayerScreen
import com.agon.app.ui.theme.GoogleMaterialTheme
import com.agon.app.ui.theme.GoogleBlue
import com.agon.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GoogleMaterialTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val viewModel: MainViewModel = viewModel()
    val navController = rememberNavController()
    val userProfile by viewModel.userProfile.collectAsState()
    val selectedVideo by viewModel.selectedVideo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Google Sign-In launcher
    val signInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.handleSignInResult(result.data)
        }
    }
    
    // Show video player if video is selected
    if (selectedVideo != null) {
        VideoPlayerScreen(
            video = selectedVideo!!,
            onBack = { viewModel.clearSelectedVideo() }
        )
    } else {
        // Check if user needs to sign in
        if (userProfile?.isSignedIn != true) {
            SignInScreen(
                isLoading = isLoading,
                onSignInClick = {
                    signInLauncher.launch(viewModel.getSignInIntent())
                }
            )
        } else {
            // Main app with navigation
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.White,
                bottomBar = {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 8.dp
                    ) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        
                        NavigationBarItem(
                            selected = currentRoute == "home",
                            onClick = { navController.navigate("home") { popUpTo(0) } },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == "home") 
                                        Icons.Filled.Home 
                                    else 
                                        Icons.Outlined.Home,
                                    contentDescription = "Главная"
                                )
                            },
                            label = { Text("Главная") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = GoogleBlue,
                                selectedTextColor = GoogleBlue,
                                indicatorColor = GoogleBlue.copy(alpha = 0.1f)
                            )
                        )
                        
                        NavigationBarItem(
                            selected = currentRoute == "profile",
                            onClick = { navController.navigate("profile") { popUpTo(0) } },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == "profile") 
                                        Icons.Filled.Person 
                                    else 
                                        Icons.Outlined.Person,
                                    contentDescription = "Профиль"
                                )
                            },
                            label = { Text("Профиль") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = GoogleBlue,
                                selectedTextColor = GoogleBlue,
                                indicatorColor = GoogleBlue.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.padding(paddingValues)
                ) {
                    composable("home") {
                        HomeScreen(viewModel = viewModel)
                    }
                    composable("profile") {
                        ProfileScreen(
                            viewModel = viewModel,
                            onSignOut = { viewModel.signOut() }
                        )
                    }
                }
            }
        }
    }
}
