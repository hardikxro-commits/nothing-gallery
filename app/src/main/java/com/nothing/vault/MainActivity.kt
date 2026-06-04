package com.nothing.vault

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nothing.vault.data.model.VaultPhoto
import com.nothing.vault.data.repository.VaultRepository
import com.nothing.vault.ui.screens.DecoyScreen
import com.nothing.vault.ui.screens.ImportScreen
import com.nothing.vault.ui.screens.LockScreen
import com.nothing.vault.ui.screens.PhotoViewerScreen
import com.nothing.vault.ui.screens.SetupScreen
import com.nothing.vault.ui.screens.CreateFolderScreen
import com.nothing.vault.ui.screens.VaultGalleryScreen
import com.nothing.vault.ui.theme.NothingTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()

        val app = application as VaultApp
        val repository = app.repository

        setContent {
            NothingTheme {
                NothingApp(repository = repository)
            }
        }
    }
}

@Composable
fun NothingApp(repository: VaultRepository) {
    val navController = rememberNavController()
    val startDestination = if (repository.isFirstLaunch()) "setup" else "lock"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) },
        exitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) }
    ) {
        composable("setup") {
            SetupScreen(
                repository = repository,
                onComplete = {
                    navController.navigate("lock") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }

        composable("lock") {
            LockScreen(
                repository = repository,
                onFolderUnlocked = { folderId ->
                    navController.navigate("gallery/$folderId") {
                        popUpTo("lock") { inclusive = true }
                    }
                },
                onDecoy = {
                    navController.navigate("decoy") {
                        popUpTo("lock") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "decoy",
            enterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(600)) },
            exitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(600)) }
        ) {
            DecoyScreen()
        }

        composable(
            route = "gallery/{folderId}",
            arguments = listOf(navArgument("folderId") { type = NavType.StringType }),
            enterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) },
            exitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) }
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: return@composable
            VaultGalleryScreen(
                folderId = folderId,
                repository = repository,
                onPhotoClick = { photo ->
                    navController.navigate("viewer/${photo.folderId}/${photo.id}")
                },
                onImport = {
                    navController.navigate("import/$folderId")
                },
                onLock = {
                    navController.navigate("lock") {
                        popUpTo("gallery/$folderId") { inclusive = true }
                    }
                },
                onCreateFolder = {
                    navController.navigate("create-folder")
                }
            )
        }

        composable(
            route = "create-folder",
            enterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) },
            exitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) }
        ) {
            CreateFolderScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onCreated = { newFolderId ->
                    navController.navigate("gallery/$newFolderId") {
                        popUpTo("create-folder") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "viewer/{folderId}/{photoId}",
            arguments = listOf(
                navArgument("folderId") { type = NavType.StringType },
                navArgument("photoId") { type = NavType.StringType }
            ),
            enterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) },
            exitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) }
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: return@composable
            val photoId = backStackEntry.arguments?.getString("photoId") ?: return@composable
            val photos = remember { repository.getPhotos(folderId) }
            val currentIndex = photos.indexOfFirst { it.id == photoId }
            val photo = photos.getOrNull(currentIndex)

            if (photo != null) {
                PhotoViewerScreen(
                    photo = photo,
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() },
                    onSwipeNext = if (currentIndex < photos.size - 1) {
                        { navController.navigate("viewer/$folderId/${photos[currentIndex + 1].id}") { launchSingleTop = true; restoreState = true } }
                    } else null,
                    onSwipePrev = if (currentIndex > 0) {
                        { navController.navigate("viewer/$folderId/${photos[currentIndex - 1].id}") { launchSingleTop = true; restoreState = true } }
                    } else null
                )
            }
        }

        composable(
            route = "import/{folderId}",
            arguments = listOf(navArgument("folderId") { type = NavType.StringType }),
            enterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) },
            exitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) }
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: return@composable
            ImportScreen(
                folderId = folderId,
                repository = repository,
                onBack = { navController.popBackStack() },
                onComplete = {
                    navController.popBackStack()
                }
            )
        }
    }
}
