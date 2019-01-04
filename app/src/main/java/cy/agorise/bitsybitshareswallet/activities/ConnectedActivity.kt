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
import cy.agorise.bitsybitshareswallet.repositories.AssetRepository
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.viewmodels.BalanceViewModel
import cy.agorise.bitsybitshareswallet.viewmodels.TransferViewModel
import cy.agorise.bitsybitshareswallet.viewmodels.UserAccountViewModel
import cy.agorise.graphenej.Asset
import cy.agorise.graphenej.AssetAmount
import cy.agorise.graphenej.UserAccount
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.android.NetworkService
import cy.agorise.graphenej.api.android.RxBus
import cy.agorise.graphenej.api.calls.*
import cy.agorise.graphenej.models.AccountProperties
import cy.agorise.graphenej.models.BlockHeader
import cy.agorise.graphenej.models.FullAccountDetails
import cy.agorise.graphenej.models.JsonRpcResponse
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * The app uses the single Activity methodology, but this activity was created so that MainActivity can extend from it.
 * This class manages everything related to keeping the information in the database updated using graphenej's
 * NetworkService, leaving to MainActivity only the Navigation work and some other UI features.
 */
abstract class ConnectedActivity : AppCompatActivity(), ServiceConnection {

    companion object {
        private const val TAG = "ConnectedActivity"

        private const val RESPONSE_GET_FULL_ACCOUNTS = 1
        private const val RESPONSE_GET_ACCOUNTS = 2
        private const val RESPONSE_GET_ACCOUNT_BALANCES = 3
        private const val RESPONSE_GET_ASSETS = 4
        private const val RESPONSE_GET_BLOCK_HEADER = 5
    }

    private lateinit var mUserAccountViewModel: UserAccountViewModel
    private lateinit var mBalanceViewModel: BalanceViewModel
    private lateinit var mTransferViewModel: TransferViewModel

    private lateinit var mAssetRepository: AssetRepository

    /* Current user account */
    protected var mCurrentAccount: UserAccount? = null

    private val mHandler = Handler()

    // Disposable returned at the bus subscription
    private var mDisposable: Disposable? = null

    private var storedOpCount: Long = -1

    private var missingUserAccounts = ArrayList<UserAccount>()
    private var missingAssets = ArrayList<Asset>()

    /* Network service connection */
    protected var mNetworkService: NetworkService? = null

    // Map used to keep track of request and response id pairs
    private val responseMap = HashMap<Long, Int>()

    /** Map used to keep track of request id and block number pairs */
    private val requestIdToBlockNumberMap = HashMap<Long, Long>()

    private var blockNumberWithMissingTime = 0L

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

        mAssetRepository = AssetRepository(this)

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
                for (assetId in assetIds)
                    missingAssets.add(Asset(assetId))

                mHandler.postDelayed(mRequestMissingAssetsTask, Constants.NETWORK_SERVICE_RETRY_PERIOD)
            }
        })

        //Configure TransferViewModel to obtain the Transfer's block numbers with missing time information, one by one
        mTransferViewModel = ViewModelProviders.of(this).get(TransferViewModel::class.java)

        mTransferViewModel.getTransferBlockNumberWithMissingTime().observe(this, Observer<Long>{ blockNumber ->
            if (blockNumber != null && blockNumber != blockNumberWithMissingTime) {
                blockNumberWithMissingTime = blockNumber
                Log.d(TAG, "Block number: $blockNumber, Time: ${System.currentTimeMillis()}")
                mHandler.postDelayed(mRequestBlockMissingTimeTask, 10)
            }
        })

        mDisposable = RxBus.getBusInstance()
            .asFlowable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { handleIncomingMessage(it) }
    }

    private fun handleIncomingMessage(message: Any?) {
        if (message is JsonRpcResponse<*>) {

            if (message.error == null) {
                if (responseMap.containsKey(message.id)) {
                    val responseType = responseMap[message.id]
                    when (responseType) {
                        RESPONSE_GET_FULL_ACCOUNTS      ->
                            handleAccountDetails((message.result as List<*>)[0] as FullAccountDetails)

                        RESPONSE_GET_ACCOUNTS           ->
                            handleAccountProperties(message.result as List<AccountProperties>)

                        RESPONSE_GET_ACCOUNT_BALANCES   ->
                            handleBalanceUpdate(message.result as List<AssetAmount>)

                        RESPONSE_GET_ASSETS             ->
                            handleAssets(message.result as List<Asset>)

                        RESPONSE_GET_BLOCK_HEADER       -> {
                            val blockNumber = requestIdToBlockNumberMap[message.id] ?: 0L
                            handleBlockHeader(message.result as BlockHeader, blockNumber)
                            requestIdToBlockNumberMap.remove(message.id)
                        }
                    }
                    responseMap.remove(message.id)
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
            if (message.updateCode == ConnectionStatusUpdate.DISCONNECTED) {
                // If we got a disconnection notification, we should clear our response map, since
                // all its stored request ids will now be reset
                responseMap.clear()
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

    /**
     * Receives a list of missing [AccountProperties] from which it extracts the required information to
     * create a list of BiTSy's UserAccount objects and stores them into the database
     */
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

        mBalanceViewModel.insertAll(balances)
    }

    /**
     * Receives a list of missing [Asset] from which it extracts the required information to
     * create a list of BiTSy's Asset objects and stores them into the database
     */
    private fun handleAssets(_assets: List<Asset>) {
        val assets = ArrayList<cy.agorise.bitsybitshareswallet.database.entities.Asset>()

        for (_asset in _assets) {
            val asset = cy.agorise.bitsybitshareswallet.database.entities.Asset(
                _asset.objectId,
                _asset.symbol,
                _asset.precision,
                _asset.description ?: "",
                _asset.bitassetId ?: ""
            )

            assets.add(asset)
        }

        mAssetRepository.insertAll(assets)
        missingAssets.clear()
    }

    /**
     * Receives the [BlockHeader] related to a Transfer's missing time and saves it into the database.
     */
    private fun handleBlockHeader(blockHeader: BlockHeader, blockNumber: Long) {

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")

        try {
            val date = dateFormat.parse(blockHeader.timestamp)
            mTransferViewModel.setBlockTime(blockNumber, date.time / 1000)
        } catch (e: ParseException) {
            Log.e(TAG, "ParseException. Msg: " + e.message)
        }
    }

    private fun updateBalances() {
        if (mNetworkService!!.isConnected) {
            val id = mNetworkService!!.sendMessage(GetAccountBalances(mCurrentAccount, ArrayList()),
                GetAccountBalances.REQUIRED_API)

            responseMap[id] = RESPONSE_GET_ACCOUNT_BALANCES
        }
    }

    /**
     * Task used to obtain the missing UserAccounts from Graphenej's NetworkService.
     */
    private val mRequestMissingUserAccountsTask = object : Runnable {
        override fun run() {
            if (mNetworkService!!.isConnected) {
                val id = mNetworkService!!.sendMessage(GetAccounts(missingUserAccounts), GetAccounts.REQUIRED_API)

                responseMap[id] = RESPONSE_GET_ACCOUNTS
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
                val id = mNetworkService!!.sendMessage(GetAssets(missingAssets), GetAssets.REQUIRED_API)

                responseMap[id] = RESPONSE_GET_ASSETS
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
                    val id = mNetworkService!!.sendMessage(GetFullAccounts(userAccounts, false),
                        GetFullAccounts.REQUIRED_API)

                    responseMap[id] = RESPONSE_GET_FULL_ACCOUNTS
                }
            } else {
                Log.w(TAG, "NetworkService is null or is not connected. mNetworkService: $mNetworkService")
            }
            mHandler.postDelayed(this, Constants.MISSING_PAYMENT_CHECK_PERIOD)

        }
    }

    /**
     * Task used to obtain the missing time from a block from Graphenej's NetworkService.
     */
    private val mRequestBlockMissingTimeTask = object : Runnable {
        override fun run() {

            if (mNetworkService != null && mNetworkService!!.isConnected) {
                val id = mNetworkService!!.sendMessage(GetBlockHeader(blockNumberWithMissingTime),
                    GetBlockHeader.REQUIRED_API)

                responseMap[id] = RESPONSE_GET_BLOCK_HEADER
                requestIdToBlockNumberMap[id] = blockNumberWithMissingTime
            } else {
                mHandler.postDelayed(this, Constants.MISSING_PAYMENT_CHECK_PERIOD)
            }
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
}