package cy.agorise.bitsybitshareswallet.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.viewmodels.UserAccountViewModel
import cy.agorise.graphenej.UserAccount
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.android.NetworkService
import cy.agorise.graphenej.api.android.RxBus
import cy.agorise.graphenej.api.calls.GetAccounts
import cy.agorise.graphenej.models.AccountProperties
import cy.agorise.graphenej.models.FullAccountDetails
import cy.agorise.graphenej.models.HistoryOperationDetail
import cy.agorise.graphenej.models.JsonRpcResponse
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

/**
 * Class in charge of managing the connection to graphenej's NetworkService
 */
abstract class ConnectedActivity : AppCompatActivity(), ServiceConnection {
    private val TAG = this.javaClass.simpleName

    private lateinit var mUserAccountViewModel: UserAccountViewModel

    private val mHandler = Handler()

    // Disposable returned at the bus subscription
    private var mDisposable: Disposable? = null

    private var storedOpCount: Long = -1

    private var missingUserAccounts = ArrayList<UserAccount>()

    /* Network service connection */
    protected var mNetworkService: NetworkService? = null

    /**
     * Flag used to keep track of the NetworkService binding state
     */
    private var mShouldUnbindNetwork: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDisposable = RxBus.getBusInstance()
            .asFlowable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { message ->
                if (message is JsonRpcResponse<*>) {
                    // Generic processing taken care by subclasses
                    handleJsonRpcResponse(message)
                    // Payment detection focused responses
                    if (message.error == null) {
                        if (message.result is List<*> && (message.result as List<*>).size > 0) {
                            if ((message.result as List<*>)[0] is AccountProperties) {
                                handleAccountProperties(message.result as List<AccountProperties>)
                            }
//                            if ((message.result as List<*>)[0] is FullAccountDetails) {
//                                if (message.id == recurrentAccountUpdateId) {
//                                    handleAccountDetails((message.result as List<*>)[0] as FullAccountDetails)
//                                } else if (message.id == postProcessingAccountUpdateId) {
//                                    handleAccountUpdate((message.result as List<*>)[0] as FullAccountDetails)
//                                }
//                            }
//                        } else if (message.result is HistoryOperationDetail && message.id == accountOpRequestId) {
//                            handleNewOperations(message.result as HistoryOperationDetail)
                        }
                    } else {
                        // In case of error
                        Log.e(TAG, "Got error message from full node. Msg: " + message.error.message)
                        Toast.makeText(
                            this@ConnectedActivity,
                            String.format("Error from full node. Msg: %s", message.error.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else if (message is ConnectionStatusUpdate) {
                    handleConnectionStatusUpdate(message)
                    if (message.updateCode == ConnectionStatusUpdate.DISCONNECTED) {
//                        recurrentAccountUpdateId = -1
//                        accountOpRequestId = -1
//                        isProcessingTx = false
                    }
                }
            }

        // Configure UserAccountViewModel to show the current account
        mUserAccountViewModel = ViewModelProviders.of(this).get(UserAccountViewModel::class.java)

        mUserAccountViewModel.getMissingUserAccountIds().observe(this, Observer<List<String>>{ userAccountIds ->
            if (!userAccountIds.isEmpty()) {
                for (userAccountId in userAccountIds)
                    missingUserAccounts.add(UserAccount(userAccountId))

                mHandler.postDelayed(mRequestMissingUserAccountsTask, Constants.NETWORK_SERVICE_RETRY_PERIOD)
            }
        })
    }

    private fun handleAccountProperties(accountPropertiesList: List<AccountProperties>) {
        val userAccounts = ArrayList<cy.agorise.bitsybitshareswallet.database.entities.UserAccount>()

        for (accountProperties in accountPropertiesList) {
            val userAccount = cy.agorise.bitsybitshareswallet.database.entities.UserAccount(
                accountProperties.id,
                accountProperties.name,
                accountProperties.membership_expiration_date == Constants.LIFETIME_EXPIRATION_DATE
            )

            userAccounts.add(userAccount)
        }

        mUserAccountViewModel.insertAll(userAccounts)
    }

    /**
     * Task used to obtain the missing UserAccounts.
     */
    private val mRequestMissingUserAccountsTask = object : Runnable {
        override fun run() {
            if (mNetworkService!!.isConnected) {
                mNetworkService!!.sendMessage(GetAccounts(missingUserAccounts), GetAccounts.REQUIRED_API)
            } else {
                mHandler.postDelayed(this, Constants.NETWORK_SERVICE_RETRY_PERIOD)
            }
        }
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

    override fun onServiceDisconnected(name: ComponentName?) { }

    override fun onPause() {
        super.onPause()
        // Unbinding from network service
        if (mShouldUnbindNetwork) {
            unbindService(this)
            mShouldUnbindNetwork = false
        }
//        mHandler.removeCallbacks(mCheckMissingPaymentsTask)
        mHandler.removeCallbacks(mRequestMissingUserAccountsTask)
    }

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

    override fun onDestroy() {
        super.onDestroy()
        if (!mDisposable!!.isDisposed) mDisposable!!.dispose()
    }

    /**
     * Method to be implemented by all subclasses in order to be notified of JSON-RPC responses.
     * @param response
     */
    internal abstract fun handleJsonRpcResponse(response: JsonRpcResponse<*>)

    /**
     * Method to be implemented by all subclasses in order to be notified of connection status updates
     * @param connectionStatusUpdate
     */
    internal abstract fun handleConnectionStatusUpdate(connectionStatusUpdate: ConnectionStatusUpdate)
}