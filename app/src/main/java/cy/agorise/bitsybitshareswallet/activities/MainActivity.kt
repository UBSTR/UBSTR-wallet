package cy.agorise.bitsybitshareswallet.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.collection.LongSparseArray
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.database.entities.Balance
import cy.agorise.bitsybitshareswallet.fragments.BalancesFragment
import cy.agorise.bitsybitshareswallet.fragments.MerchantsFragment
import cy.agorise.bitsybitshareswallet.fragments.ReceiveTransactionFragment
import cy.agorise.bitsybitshareswallet.fragments.SendTransactionFragment
import cy.agorise.bitsybitshareswallet.processors.TransfersLoader
import cy.agorise.bitsybitshareswallet.repositories.BalanceRepository
import cy.agorise.graphenej.AssetAmount
import cy.agorise.graphenej.RPC
import cy.agorise.graphenej.api.ApiAccess
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.calls.GetAccountBalances
import cy.agorise.graphenej.models.JsonRpcResponse
import kotlinx.android.synthetic.main.activity_main.*
import java.util.ArrayList

class MainActivity : ConnectedActivity() {
    private val TAG = this.javaClass.simpleName

    private val requestMap = LongSparseArray<String>()

    private var mBalanceRepository: BalanceRepository? = null

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_receive -> {
                loadFragment(ReceiveTransactionFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_balances -> {
                loadFragment(BalancesFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_merchants -> {
                loadFragment(MerchantsFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_send -> {
                loadFragment(SendTransactionFragment())
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

        mBalanceRepository = BalanceRepository(this)
    }

    private fun loadFragment(fragment: Fragment) {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_frame, fragment)
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
