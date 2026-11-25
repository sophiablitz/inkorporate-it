package com.example.inkorporate_it

import android.app.Application
import com.example.inkorporate_it.data.network.GameSocket
import com.example.inkorporate_it.data.network.KtorGameSocket

/**
 * The app itself. Android creates exactly one of these when the app starts, before
 * any screen, and keeps it until the app's process ends. Registered in
 * AndroidManifest.xml with android:name=".InkorporateApp".
 *
 * It holds the long-lived pieces the whole app shares. Rotation and screen changes
 * recreate MainActivity but never this, so the connection survives them.
 */
class InkorporateApp : Application() {

    /** The one connection to the server. Created the first time something asks for it. */
    val socket: GameSocket by lazy { KtorGameSocket() }
}
