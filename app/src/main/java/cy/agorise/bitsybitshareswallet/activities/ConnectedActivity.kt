package cy.agorise.bitsybitshareswallet.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.graphenej.api.android.NetworkService
import cy.agorise.graphenej.api.calls.GetFullAccounts
import java.util.ArrayList

/**
 * Class in charge of managing the connection to graphenej's NetworkService
 */
abstract class ConnectedActivity : AppCompatActivity(), ServiceConnection {

    private val TAG = "ConnectedActivity"

    private val mHandler = Handler()

    /* Network service connection */
    protected var mNetworkService: NetworkService? = null

    /**
     * Flag used to keep track of the NetworkService binding state
     */
    private var mShouldUnbindNetwork: Boolean = false

    override fun onResume() {
        super.onResume()

        val intent = Intent(this, NetworkService::class.java)
        if (bindService(intent, this, Context.BIND_AUTO_CREATE)) {
            mShouldUnbindNetwork = true
        } else {
            Log.e(TAG, "Binding to the network service failed.")
        }
//        mHandler.postDelayed(mCheckMissingPaymentsTask, Constants.MISSING_PAYMENT_CHECK_PERIOD)

//        storedOpCount = PreferenceManager.getDefaultSharedPreferences(this)
//            .getLong(Constants.KEY_ACCOUNT_OPERATION_COUNT, -1)
    }

    /**
     * Task used to perform a redundant payment check.
     */
//    private val mCheckMissingPaymentsTask = object : Runnable {
//        override fun run() {
//            if (mNetworkService != null && mNetworkService.isConnected()) {
//                val userId = PreferenceManager
//                    .getDefaultSharedPreferences(this@ConnectedActivity)
//                    .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "")
//                if (userId != "") {
//                    // Checking that we actually have a user id registered in the shared preferences
//                    val userAccounts = ArrayList<String>()
//                    userAccounts.add(userId)
//                    recurrentAccountUpdateId = mNetworkService.sendMessage(
//                        GetFullAccounts(userAccounts, false),
//                        GetFullAccounts.REQUIRED_API
//                    )
//                } else {
//                    Log.w(TAG, "User id is empty")
//                }
//            } else {
//                Log.w(TAG, "NetworkService is null or is not connected. mNetworkService: $mNetworkService")
//            }
//            mHandler.postDelayed(this, Constants.MISSING_PAYMENT_CHECK_PERIOD)
//        }
//    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        // We've bound to LocalService, cast the IBinder and get LocalService instance
        val binder = service as NetworkService.LocalBinder
        mNetworkService = binder.service
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }
}