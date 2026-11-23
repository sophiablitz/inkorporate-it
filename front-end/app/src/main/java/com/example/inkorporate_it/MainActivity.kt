package com.example.inkorporate_it

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.inkorporate_it.model.Avatar
import com.example.inkorporate_it.model.AvatarSaver
import com.example.inkorporate_it.ui.frontendscreens.HomeScreen
import com.example.inkorporate_it.ui.game.GameScreen
import com.example.inkorporate_it.ui.theme.InkorporateitTheme
import kotlinx.serialization.Serializable
import kotlin.random.Random

/*
 * The app's screens, as type-safe Navigation Compose routes.
 * @Serializable lets Navigation Compose turn each one into a destination.
 *
 * Game carries the join code: null = create a new game, a 6-digit code = join that game.
 * The code stays a String so a leading 0 ("012345") is kept.
 */
@Serializable
sealed interface AppRoute {
    @Serializable
    data object Home : AppRoute

    @Serializable
    data class Game(val code: String? = null) : AppRoute
    @Serializable
    data object Settings : AppRoute

    @Serializable
    data object FreeDraw : AppRoute
}

class MainActivity : ComponentActivity() {
    // Names picked at random on first launch. Add more team names here.
    private val DefaultNames = listOf("Alejandro", "Jerry", "Jorge", "Ismael")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // dynamicColor = false: use the team's Material Theme Builder colors,
            // not the phone's wallpaper colors (Theme.kt defaults to wallpaper colors).
            InkorporateitTheme(dynamicColor = false) {
                // Remembers which screens are open (the back stack) and moves between them.
                val navController = rememberNavController()

                // The player, kept in memory only (how to save it is not decided).
                // First launch gets a random team name and the default blob.
                // rememberSaveable + PlayerSaver keep the player when the phone rotates,
                // or dark mode / font size changes. Closing the app still forgets it.
                var avatar by rememberSaveable(stateSaver = AvatarSaver) {
                    mutableStateOf(Avatar())
                }
                var username by rememberSaveable { mutableStateOf(randomDefaultUsername()) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // innerPadding keeps content clear of the status and navigation bars.
                    // The phone's Back gesture pops the back stack, so it returns to Home.
                    NavHost(
                        navController = navController,
                        startDestination = AppRoute.Home,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable<AppRoute.Home> {
                            HomeScreen(
                                username,
                                avatar,
                                onUsernameChange = { username = it },
                                onAvatarChange = { avatar = it },
                                onSettings = { navController.navigate(AppRoute.Settings) },
                                onCreateGame = { navController.navigate(AppRoute.Game(code = null)) },
                                // HomeScreen only calls this with a complete 6-digit code.
                                onJoinGame = { code -> navController.navigate(AppRoute.Game(code = code)) },
                                onFreeDraw = { navController.navigate(AppRoute.FreeDraw) },
                            )
                        }
                        // The whole game (lobby, rounds, end screens). GameScreen creates its
                        // GameViewModel here, so the ViewModel lasts exactly as long as this
                        // screen is on the back stack and is cleared when the player leaves.
                        // The player's name and avatar are passed in directly, not in the route:
                        // Avatar holds a Compose Color, which a route can't carry.
                        composable<AppRoute.Game> { backStackEntry ->
                            val route = backStackEntry.toRoute<AppRoute.Game>()
                            GameScreen(
                                gameCode = route.code,
                                username = username,
                                avatar = avatar,
                                onLeave = { navController.popBackStack() },
                            )
                        }
                        composable<AppRoute.Settings> {
                            PlaceholderScreen(
                                title = "Settings (not decided yet)",
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable<AppRoute.FreeDraw> {
                            PlaceholderScreen(
                                title = "Free Draw (placeholder)",
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }

    /*
     * The player you get on first launch: a random team name and the default blob.
     * `random` can be swapped in tests so the result is predictable.
     */
    private fun randomDefaultUsername(random: Random = Random): String {
        return DefaultNames.random(random)
    }
}

// An empty stand-in screen: a title and a Back button.
@Composable
fun PlaceholderScreen(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onBack) { Text("Back") }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaceholderScreenPreview() {
    InkorporateitTheme(dynamicColor = false) {
        PlaceholderScreen(title = "Settings (not decided yet)", onBack = {})
    }
}
