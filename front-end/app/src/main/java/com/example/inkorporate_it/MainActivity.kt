package com.example.inkorporate_it

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.model.PlayerSaver
import com.example.inkorporate_it.model.randomDefaultPlayer
import com.example.inkorporate_it.ui.frontendscreens.HomeScreen
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

/*
 * TEMPORARY screen switch.
 * Navigation is UNDECIDED (Ismael's area). Until it exists, this enum and the
 * `when` below decide which screen shows. Lobby, Edit avatar, Settings and
 * Free Draw are empty placeholders. Replace all of this when the real navigation lands.
 */
enum class Screen { Home, Lobby, EditAvatar, Settings, FreeDraw }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // dynamicColor = false: use the team's Material Theme Builder colors,
            // not the phone's wallpaper colors (Theme.kt defaults to wallpaper colors).
            InkorporateitTheme(dynamicColor = false) {
                // Which screen is showing. Starts on Home.
                var screen by rememberSaveable { mutableStateOf(Screen.Home) }

                // The player, kept in memory only (how to save it is not decided).
                // First launch gets a random team name and the default blob.
                // rememberSaveable + PlayerSaver keep the player when the phone rotates,
                // or dark mode / font size changes. Closing the app still forgets it.
                var player by rememberSaveable(stateSaver = PlayerSaver) {
                    mutableStateOf(randomDefaultPlayer())
                }

                // The phone's Back gesture returns to Home from any placeholder.
                BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // innerPadding keeps content clear of the status and navigation bars.
                    val contentModifier = Modifier.padding(innerPadding)

                    when (screen) {
                        Screen.Home -> HomeScreen(
                            player = player,
                            onPlayerChange = { newPlayer -> player = newPlayer },
                            onSettings = { screen = Screen.Settings },
                            onEditAvatar = { screen = Screen.EditAvatar },
                            onCreateGame = { screen = Screen.Lobby },
                            // STUB: the real join call is UNDECIDED. Always succeeds for now.
                            onJoinGame = { _ -> screen = Screen.Lobby },
                            onFreeDraw = { screen = Screen.FreeDraw },
                            modifier = contentModifier
                        )
                        Screen.Lobby -> PlaceholderScreen(
                            title = "Lobby (Jerry's screen)",
                            onBack = { screen = Screen.Home },
                            modifier = contentModifier
                        )
                        Screen.EditAvatar -> PlaceholderScreen(
                            title = "Edit avatar (not decided yet)",
                            onBack = { screen = Screen.Home },
                            modifier = contentModifier
                        )
                        Screen.Settings -> PlaceholderScreen(
                            title = "Settings (not decided yet)",
                            onBack = { screen = Screen.Home },
                            modifier = contentModifier
                        )
                        Screen.FreeDraw -> PlaceholderScreen(
                            title = "Free Draw (placeholder)",
                            onBack = { screen = Screen.Home },
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
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
        PlaceholderScreen(title = "Lobby (Jerry's screen)", onBack = {})
    }
}
