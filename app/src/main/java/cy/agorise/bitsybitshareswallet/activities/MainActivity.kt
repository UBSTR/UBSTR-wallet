package cy.agorise.bitsybitshareswallet.activities

import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.view.MenuItem
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.models.JsonRpcResponse
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : ConnectedActivity() {
    private val TAG = this.javaClass.simpleName

    private lateinit var appBarConfiguration : AppBarConfiguration

    // Handler and Runnable used to add a timer for user inaction and close the app if enough time has passed
    private lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Sets the theme to night mode if it has been selected by the user
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, false)) {
            setTheme(R.style.Theme_Bitsy_Dark_NoActionBar)
        }
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        val host: NavHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment? ?: return

        // Set up Action Bar
        val navController = host.navController

        appBarConfiguration = AppBarConfiguration(navController.graph)

        setupActionBarWithNavController(navController, appBarConfiguration)

        mHandler = Handler()
        mRunnable = Runnable {
            if (PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(Constants.KEY_AUTO_CLOSE_ACTIVATED, false))
                finish()
            else
                restartHandler()
        }
        startHandler()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        restartHandler()
    }

    private fun restartHandler() {
        stopHandler()
        startHandler()
    }

    private fun stopHandler() {
        mHandler.removeCallbacks(mRunnable)
    }

    private fun startHandler() {
        mHandler.postDelayed(mRunnable, 3 * 60 * 1000) //for 3 minutes
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Have the NavigationUI look for an action or destination matching the menu
        // item id and navigate there if found.
        // Otherwise, bubble up to the parent.
        return item.onNavDestinationSelected(findNavController(R.id.navHostFragment))
                || super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        // Allows NavigationUI to support proper up navigation or the drawer layout
        // drawer menu, depending on the situation
        return findNavController(R.id.navHostFragment).navigateUp(appBarConfiguration)
    }

    override fun handleJsonRpcResponse(response: JsonRpcResponse<*>) {

    }

    /**
     * Private method called whenever there's an update to the connection status
     * @param connectionStatusUpdate  Connection status update.
     */
    override fun handleConnectionStatusUpdate(connectionStatusUpdate: ConnectionStatusUpdate) {

    }
}
