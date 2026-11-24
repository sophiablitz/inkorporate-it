package com.example.inkorporate_it

import android.app.Application
import com.example.inkorporate_it.data.network.GameSocket
import com.example.inkorporate_it.data.network.KtorGameSocket
import com.example.inkorporate_it.data.network.StubServerSocket

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
    val socket: GameSocket by lazy {
        if (USE_STUB_SERVER) StubServerSocket() else KtorGameSocket()
    }

    companion object {
        /**
         * true: play against StubServerSocket, a pretend server inside the app.
         * false: connect to the real server (run it with ./gradlew :server:run).
         * Flip to false once the server answers the protocol's messages.
         */
        const val USE_STUB_SERVER = true
    }
}
