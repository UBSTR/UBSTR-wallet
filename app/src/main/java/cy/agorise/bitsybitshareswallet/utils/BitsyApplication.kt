package cy.agorise.bitsybitshareswallet.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import cy.agorise.graphenej.api.ApiAccess
import cy.agorise.graphenej.api.android.NetworkServiceManager

class BitsyApplication : Application(), Application.ActivityLifecycleCallbacks {

    val BITSHARES_NODE_URLS = arrayOf(
        // PP private nodes
        "wss://nl.palmpay.io/ws",
        "wss://mx.palmpay.io/ws",

        // Other public nodes
        "wss://bitshares.nu/ws", // Stockholm, Sweden
        "wss://bitshares.openledger.info/ws", // Openledger node
        "wss://dallas.bitshares.apasia.tech/ws", // Dallas, USA
        "wss://atlanta.bitshares.apasia.tech/ws", // Atlanta, USA
        "wss://miami.bitshares.apasia.tech/ws", // Miami, USA
        "wss://valley.bitshares.apasia.tech/ws", // Silicon Valley, USA
        "wss://england.bitshares.apasia.tech/ws", // London, UK
        "wss://netherlands.bitshares.apasia.tech/ws", // Amsterdam, Netherlands
        "wss://australia.bitshares.apasia.tech/ws", // Sidney, Australia
        "wss://bit.btsabc.org/ws", // Hong Kong, China
        "wss://node.btscharts.com/ws", // Beijing, Chinawss://node.btscharts.com/ws
        "wss://ws.gdex.top", // Shanghai, China
        "wss://dex.rnglab.org", // Amsterdam, Netherlands
        "wss://api.bts.blckchnd.com", // Falkenstein, Germany
        "wss://api-ru.bts.blckchnd.com", // Moscow, Russia
        "wss://crazybit.online", // Shenzhen, China?
        "wss://citadel.li/node"
    )

    override fun onCreate() {
        super.onCreate()

        // Specifying some important information regarding the connection, such as the
        // credentials and the requested API accesses
        val requestedApis = ApiAccess.API_DATABASE or ApiAccess.API_HISTORY or ApiAccess.API_NETWORK_BROADCAST
        val networkManager = NetworkServiceManager.Builder()
            .setUserName("")
            .setPassword("")
            .setRequestedApis(requestedApis)
            .setCustomNodeUrls(setupNodes())
            .setAutoConnect(true)
            .setNodeLatencyVerification(true)
            .build(this)

        /*
        * Registering this class as a listener to all activity's callback cycle events, in order to
        * better estimate when the user has left the app and it is safe to disconnect the websocket connection
        */
        registerActivityLifecycleCallbacks(networkManager)

        /*
        * Registering this class as a listener to all activity's callback cycle events, in order to
        * better estimate when the user has left the app and it is safe to disconnect the WebSocket connection
        * TODO is it necessary??
        */
        registerActivityLifecycleCallbacks(this)
    }

    private fun setupNodes(): String {
        val stringBuilder = StringBuilder()
        for (url in BITSHARES_NODE_URLS) {
            stringBuilder.append(url).append(",")
        }
        stringBuilder.replace(stringBuilder.length - 1, stringBuilder.length, "")
        return stringBuilder.toString()
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onActivityStarted(activity: Activity?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onActivityResumed(activity: Activity?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onActivityPaused(activity: Activity?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onActivityStopped(activity: Activity?) { }

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) { }

    override fun onActivityDestroyed(activity: Activity?) { }
}