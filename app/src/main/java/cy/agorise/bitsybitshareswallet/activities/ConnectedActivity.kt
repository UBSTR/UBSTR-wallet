package cy.agorise.bitsybitshareswallet.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import cy.agorise.bitsybitshareswallet.database.entities.Balance
import cy.agorise.bitsybitshareswallet.processors.TransfersLoader
import cy.agorise.bitsybitshareswallet.repositories.BalanceRepository
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.viewmodels.BalanceViewModel
import cy.agorise.bitsybitshareswallet.viewmodels.UserAccountViewModel
import cy.agorise.graphenej.Asset
import cy.agorise.graphenej.AssetAmount
import cy.agorise.graphenej.UserAccount
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.android.NetworkService
import cy.agorise.graphenej.api.android.RxBus
import cy.agorise.graphenej.api.calls.GetAccountBalances
import cy.agorise.graphenej.api.calls.GetAccounts
import cy.agorise.graphenej.api.calls.GetFullAccounts
import cy.agorise.graphenej.api.calls.GetObjects
import cy.agorise.graphenej.models.AccountProperties
import cy.agorise.graphenej.models.FullAccountDetails
import cy.agorise.graphenej.models.JsonRpcResponse
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.*
import kotlin.collections.ArrayList

/**
 * Class in charge of managing the connection to graphenej's NetworkService
 */
abstract class ConnectedActivity : AppCompatActivity(), ServiceConnection {
    private val TAG = this.javaClass.simpleName

    private lateinit var mUserAccountViewModel: UserAccountViewModel
    private lateinit var mBalanceViewModel: BalanceViewModel

    private lateinit var mBalanceRepository: BalanceRepository

    /* Current user account */
    protected var mCurrentAccount: UserAccount? = null

    private val mHandler = Handler()

    // Disposable returned at the bus subscription
    private var mDisposable: Disposable? = null

    private var storedOpCount: Long = -1

    private var missingUserAccounts = ArrayList<UserAccount>()
    private var missingAssets = ArrayList<String>()

    /* Network service connection */
    protected var mNetworkService: NetworkService? = null

    /**
     * Flag used to keep track of the NetworkService binding state
     */
    private var mShouldUnbindNetwork: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "")
        if (userId != "")
            mCurrentAccount = UserAccount(userId)

        mBalanceRepository = BalanceRepository(this)

        // Configure UserAccountViewModel to obtain the missing account ids
        mUserAccountViewModel = ViewModelProviders.of(this).get(UserAccountViewModel::class.java)

        mUserAccountViewModel.getMissingUserAccountIds().observe(this, Observer<List<String>>{ userAccountIds ->
            if (userAccountIds.isNotEmpty()) {
                missingUserAccounts.clear()
                for (userAccountId in userAccountIds)
                    missingUserAccounts.add(UserAccount(userAccountId))

                mHandler.postDelayed(mRequestMissingUserAccountsTask, Constants.NETWORK_SERVICE_RETRY_PERIOD)
            }
        })

        // Configure UserAccountViewModel to obtain the missing account ids
        mBalanceViewModel = ViewModelProviders.of(this).get(BalanceViewModel::class.java)

        mBalanceViewModel.getMissingAssetIds().observe(this, Observer<List<String>>{ assetIds ->
            if (assetIds.isNotEmpty()) {
                missingAssets.clear()
                missingAssets.addAll(assetIds)

                mHandler.postDelayed(mRequestMissingAssetsTask, Constants.NETWORK_SERVICE_RETRY_PERIOD)
            }
        })

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
                            if ((message.result as List<*>)[0] is FullAccountDetails) {
                                handleAccountDetails((message.result as List<*>)[0] as FullAccountDetails)
                            } else if ((message.result as List<*>)[0] is AccountProperties) {
                                handleAccountProperties(message.result as List<AccountProperties>)
                            } else if ((message.result as List<*>)[0] is AssetAmount) {
                                handleBalanceUpdate(message.result as List<AssetAmount>)
                            } else if ((message.result as List<*>)[0] is Asset) {
                                handleAssets(message.result as List<Asset>)
                            }
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
                }
            }
    }

    /**
     * Method called whenever a response to the 'get_full_accounts' API call has been detected.
     * @param accountDetails    De-serialized account details object
     */
    private fun handleAccountDetails(accountDetails: FullAccountDetails) {
        val latestOpCount = accountDetails.statistics.total_ops
        Log.d(TAG, "handleAccountDetails. prev count: $storedOpCount, current count: $latestOpCount")

        if (latestOpCount == 0L) {
            Log.d(TAG, "The node returned 0 total_ops for current account and may not have installed the history plugin. " +
                    "\nAsk the NetworkService to remove the node from the list and connect to another one.")
            mNetworkService!!.removeCurrentNodeAndReconnect()
        } else if (storedOpCount == -1L) {
            // Initial case when the app starts
            storedOpCount = latestOpCount
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putLong(Constants.KEY_ACCOUNT_OPERATION_COUNT, latestOpCount).apply()
            TransfersLoader(this)
            updateBalances()
        } else if (latestOpCount > storedOpCount) {
            storedOpCount = latestOpCount
            TransfersLoader(this)
            updateBalances()
        }
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
        missingUserAccounts.clear()
    }

    private fun handleBalanceUpdate(assetAmountList: List<AssetAmount>) {
        Log.d(TAG, "handleBalanceUpdate")
        val now = System.currentTimeMillis() / 1000
        val balances = ArrayList<Balance>()
        for (assetAmount in assetAmountList) {
            val balance = Balance(
                assetAmount.asset.objectId,
                assetAmount.amount.toLong(),
                now
            )

            balances.add(balance)
        }
        mBalanceRepository.insertAll(balances)
    }

    private fun handleAssets(assets: List<Asset>) {
        Log.d(TAG, "handleAssets")
    }

    private fun updateBalances() {
        if (mNetworkService!!.isConnected) {
            mNetworkService!!.sendMessage(
                GetAccountBalances(mCurrentAccount, ArrayList()),
                GetAccountBalances.REQUIRED_API
            )
        }
    }

    /**
     * Task used to obtain the missing UserAccounts from Graphenej's NetworkService.
     */
    private val mRequestMissingUserAccountsTask = object : Runnable {
        override fun run() {
            if (mNetworkService!!.isConnected) {
                mNetworkService!!.sendMessage(GetAccounts(missingUserAccounts), GetAccounts.REQUIRED_API)
            } else if (missingUserAccounts.isNotEmpty()){
                mHandler.postDelayed(this, Constants.NETWORK_SERVICE_RETRY_PERIOD)
            }
        }
    }

    /**
     * Task used to obtain the missing Assets from Graphenej's NetworkService.
     */
    private val mRequestMissingAssetsTask = object : Runnable {
        override fun run() {
            if (mNetworkService!!.isConnected) {
                // TODO use GetAssets to obtain the missing assets and save them into the db
                mNetworkService!!.sendMessage(GetObjects(missingAssets), GetAccounts.REQUIRED_API)
            } else if (missingAssets.isNotEmpty()){
                mHandler.postDelayed(this, Constants.NETWORK_SERVICE_RETRY_PERIOD)
            }
        }
    }

    /**
     * Task used to perform a redundant payment check.
     */
    private val mCheckMissingPaymentsTask = object : Runnable {
        override fun run() {
            if (mNetworkService != null && mNetworkService!!.isConnected) {
                if (mCurrentAccount != null) {
                    val userAccounts = ArrayList<String>()
                    userAccounts.add(mCurrentAccount!!.objectId)
                    mNetworkService!!.sendMessage(
                        GetFullAccounts(userAccounts, false),
                        GetFullAccounts.REQUIRED_API
                    )
                }
            } else {
                Log.w(TAG, "NetworkService is null or is not connected. mNetworkService: $mNetworkService")
            }
            mHandler.postDelayed(this, Constants.MISSING_PAYMENT_CHECK_PERIOD)
        }
    }

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
        mHandler.removeCallbacks(mCheckMissingPaymentsTask)
        mHandler.removeCallbacks(mRequestMissingUserAccountsTask)
        mHandler.removeCallbacks(mRequestMissingAssetsTask)
    }

    override fun onResume() {
        super.onResume()

        val intent = Intent(this, NetworkService::class.java)
        if (bindService(intent, this, Context.BIND_AUTO_CREATE)) {
            mShouldUnbindNetwork = true
        } else {
            Log.e(TAG, "Binding to the network service failed.")
        }
        mHandler.postDelayed(mCheckMissingPaymentsTask, Constants.MISSING_PAYMENT_CHECK_PERIOD)
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