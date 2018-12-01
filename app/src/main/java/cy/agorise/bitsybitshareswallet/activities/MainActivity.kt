package cy.agorise.bitsybitshareswallet.activities

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.collection.LongSparseArray
import com.google.android.material.bottomnavigation.BottomNavigationView
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.entities.Balance
import cy.agorise.bitsybitshareswallet.fragments.BalancesFragment
import cy.agorise.bitsybitshareswallet.fragments.MerchantsFragment
import cy.agorise.bitsybitshareswallet.processors.TransfersLoader
import cy.agorise.bitsybitshareswallet.repositories.BalanceRepository
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.graphenej.AssetAmount
import cy.agorise.graphenej.RPC
import cy.agorise.graphenej.UserAccount
import cy.agorise.graphenej.api.ApiAccess
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.calls.GetAccountBalances
import cy.agorise.graphenej.models.JsonRpcResponse
import kotlinx.android.synthetic.main.activity_main.*
import java.util.ArrayList

class MainActivity : ConnectedActivity() {
    private val TAG = this.javaClass.simpleName

    private val requestMap = LongSparseArray<String>()

    /* Current user account */
    private var mCurrentAccount: UserAccount? = null

    private var mBalanceRepository: BalanceRepository? = null

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_receive -> {
                Toast.makeText(this, "Receive Fragment", Toast.LENGTH_SHORT).show()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_balances -> {
                loadBalancesFragment()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_merchants -> {
                loadMerchantsFragment()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_send -> {
                Toast.makeText(this, "Send Fragment", Toast.LENGTH_SHORT).show()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        navigation.selectedItemId = R.id.navigation_balances

        val userId = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "")
        if (userId != "")
            mCurrentAccount = UserAccount(userId)

        mBalanceRepository = BalanceRepository(this)
    }

    private fun loadBalancesFragment() {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_frame, BalancesFragment())
        ft.commit()
    }

    private fun loadMerchantsFragment() {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_frame, MerchantsFragment())
        ft.commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflates the menu and places it in the toolbar
        menuInflater.inflate(R.menu.menu_main, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return if (item!!.itemId == R.id.menu_settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun handleJsonRpcResponse(response: JsonRpcResponse<*>) {
        if (requestMap.get(response.id) == RPC.CALL_GET_ACCOUNT_BALANCES) {
            handleBalanceUpdate(response as JsonRpcResponse<List<AssetAmount>>)
        }
    }

    /**
     * Private method called whenever there's an update to the connection status
     * @param connectionStatusUpdate  Connection status update.
     */
    override fun handleConnectionStatusUpdate(connectionStatusUpdate: ConnectionStatusUpdate) {
        when (connectionStatusUpdate.updateCode) {
            ConnectionStatusUpdate.CONNECTED -> { /* Do nothing for now */ }
            ConnectionStatusUpdate.DISCONNECTED -> { /* Do nothing for now */ }
            ConnectionStatusUpdate.AUTHENTICATED -> {}//updateBalances() }
            ConnectionStatusUpdate.API_UPDATE -> {
                // In certain cases the information about the accounts is not complete, this may not be the best
                // solution but at least it works. Feel free to improve it or move it to a better place
                //MissingAccountsLoader(this, lifecycle)

                if (connectionStatusUpdate.api == ApiAccess.API_NETWORK_BROADCAST) {
                    Log.d(TAG, "ConnectionStatusUpdate: API_NETWORK_BROADCAST")
                    // Instantiating this loader is enough to kick-start the transfers loading procedure
                    TransfersLoader(this, lifecycle)

                    updateBalances()
                }
            }
        }
    }

    private fun updateBalances() {
        if (mNetworkService!!.isConnected) {
            val id = mNetworkService!!.sendMessage(
                GetAccountBalances(mCurrentAccount, ArrayList()),
                GetAccountBalances.REQUIRED_API
            )
            requestMap.put(id, RPC.CALL_GET_ACCOUNT_BALANCES)
        }
    }

    private fun handleBalanceUpdate(response: JsonRpcResponse<List<AssetAmount>>) {
        Log.d(TAG, "handleBalanceUpdate")
        val now = System.currentTimeMillis() / 1000
        val assetBalances = response.result
        val balances = ArrayList<Balance>()
        for (assetBalance in assetBalances) {
            val balance = Balance(
                assetBalance.asset.objectId,
                assetBalance.amount.toLong(),
                now
            )

            balances.add(balance)
        }
        mBalanceRepository!!.insertAll(balances)
    }
}
