package cy.agorise.bitsybitshareswallet.processors

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import cy.agorise.bitsybitshareswallet.entities.Transfer
import cy.agorise.bitsybitshareswallet.models.HistoricalOperationEntry
import cy.agorise.bitsybitshareswallet.repositories.TransferRepository
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.graphenej.*
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.android.NetworkService
import cy.agorise.graphenej.api.android.RxBus
import cy.agorise.graphenej.api.calls.GetRelativeAccountHistory
import cy.agorise.graphenej.errors.ChecksumException
import cy.agorise.graphenej.models.BlockHeader
import cy.agorise.graphenej.models.JsonRpcResponse
import cy.agorise.graphenej.models.OperationHistory
import cy.agorise.graphenej.operations.TransferOperation
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import org.bitcoinj.core.DumpedPrivateKey
import org.bitcoinj.core.ECKey
import java.util.*

/**
 * This class is responsible for loading the local database with all past transfer operations of the
 * currently selected account.
 *
 * The procedure used to load the database in 3 steps:
 *
 * 1- Load all transfer operations
 * 2- Load all missing times
 * 3- Load all missing equivalent times
 *
 * Since the 'get_relative_account_history' will not provide either timestamps nor equivalent values
 * for every transfer, we must first load all historical transfer operations, and then proceed to
 * handle those missing columns.
 */
class TransfersLoader(private var mContext: Context?, private val mLifeCycle: Lifecycle) : LifecycleObserver {
    private val TAG = this.javaClass.name

    /* Constant used to fix the number of historical transfers to fetch from the network in one batch */
    private val HISTORICAL_TRANSFER_BATCH_SIZE = 100

    /* Constant used to split the missing times and equivalent values in batches of constant time */
    private val MISSING_TIMES_BATCH_SIZE = 100

    /* Constants used to specify which type of conversion will be used. A direct conversion is performed
    * only between BTS -> bitUSD. For all other assets, we'll need to perform a two step conversion
    * in the form XXX -> BTS -> bitUSD. That is the basic difference between direct and indirect conversions.
    */
    private val DIRECT_CONVERSION: Short = 0
    private val INDIRECT_CONVERSION: Short = 1

    private val RESPONSE_GET_RELATIVE_ACCOUNT_HISTORY = 0
    private val RESPONSE_GET_MARKET_HISTORY = 1

    // The current conversion type
    private var mConversionType: Short = -1

    /* Variable used in the 2-step currency conversion, in order to temporally hold the value of the
     * XXX -> BTS conversion. */
    private var coreCurrencyEqValue: AssetAmount? = null

    private var mDisposable: Disposable? = null

    /* Current user account */
    private var mCurrentAccount: UserAccount? = null

    /** Variable holding the current user's private key in the WIF format */
    private var wifKey: String? = null

    /** Repository to access and update Transfers */
    private var transferRepository: TransferRepository? = null

    /* Network service connection */
    private var mNetworkService: NetworkService? = null

    /* Counter used to keep track of the transfer history batch count */
    private var historicalTransferCount = 0

    /* List of block numbers with missing date information in the database */
    private var missingTimes: LinkedList<Long>? = null

    /* List of transactions for which we don't have the equivalent value data */
    private var missingEquivalentValues: LinkedList<HistoricalOperationEntry>? = null

    /* Reference to the current operation entry that we're currently working with */
    private var mTransferEntry: HistoricalOperationEntry? = null

    /* Map used to exclude operation ids that were already checked for equivalent value, but could
    * not be obtained for some reason. This is important in order to add some finality to the
    * procedure
    */
    private val eqValueBlacklist = HashMap<String, Boolean>()

//    /*
//    * Account loader, used to fetch extra information about any new account that we might come
//    * across after updating the transfers information
//    */
//    private var missingAccountsLoader: MissingAccountsLoader? = null

//    /*
//     * Assets loader, used to fetch missing asset data after the transfers table has been updated
//     */
//    private var missingAssetsLoader: MissingAssetsLoader? = null

    // Used to keep track of the current state
    private var mState = State.IDLE

    /**
     * Flag used to keep track of the NetworkService binding state
     */
    private var mShouldUnbindNetwork: Boolean = false

    private val DEBUG = false

    private var lastId: Long = 0

    private var lastEquivalentValueBlockNum: Long = 0

    private var lastMissingTimeBlockNum: Long = 0

    // Map used to keep track of request and response id pairs
    private val responseMap = HashMap<Long, Int>()

    private val mNetworkServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as NetworkService.LocalBinder
            mNetworkService = binder.service

            // Start the transfers update
            startTransfersUpdateProcedure()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {}
    }

    /**
     * Enum class used to keep track of the current state of the loader
     */
    private enum class State {
        IDLE,
        LOADING_MISSING_TIMES,
        LOADING_EQ_VALUES,
        CANCELLED,
        FINISHED
    }

    init {
        this.mLifeCycle.addObserver(this)
        transferRepository = TransferRepository(mContext!!)

        val pref = PreferenceManager.getDefaultSharedPreferences(mContext)
        val userId = pref.getString(Constants.KEY_CURRENT_ACCOUNT_ID, "")
        if (userId != "") {
            mCurrentAccount = UserAccount(userId)
//            wifkey = database.getWif(mContext, mCurrentAccount, AuthorityType.MEMO) TODO RESTORE
            mDisposable = RxBus.getBusInstance()
                .asFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(Consumer { message ->
                    if (mState == State.FINISHED) return@Consumer
                    if (message is JsonRpcResponse<*>) {
                        if (message.result is List<*>) {
                            if (responseMap.containsKey(message.id)) {
                                val responseType = responseMap[message.id]
                                when (responseType) {
                                    RESPONSE_GET_RELATIVE_ACCOUNT_HISTORY -> handleOperationList(message.result as List<OperationHistory>)
//                                    RESPONSE_GET_MARKET_HISTORY -> handlePastMarketData(message.result as List<BucketObject>)
                                }
                                responseMap.remove(message.id)
                            }
                        } else if (message.result is BlockHeader) {
//                            handleMissingTimeResponse(message.result as BlockHeader)
                        }
                    } else if (message is ConnectionStatusUpdate) {
                        if (message.updateCode == ConnectionStatusUpdate.DISCONNECTED) {
                            // If we got a disconnection notification, we should clear our response map, since
                            // all its stored request ids will now be reset
                            responseMap.clear()
                        }
                    }
                })
        } else {
            // If there is no current user, we should not do anything
            mState = State.CANCELLED
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    internal fun onStart() {
        if (mState != State.CANCELLED) {
            val intent = Intent(mContext, NetworkService::class.java)
            if (mContext!!.bindService(intent, mNetworkServiceConnection, Context.BIND_AUTO_CREATE)) {
                mShouldUnbindNetwork = true
            } else {
                Log.e(TAG, "Binding to the network service failed.")
            }
        }
    }

    /**
     * Starts the procedure that will try to update the 'transfers' table
     */
    private fun startTransfersUpdateProcedure() {
        if (DEBUG) {
            // If we are in debug mode, we first erase all entries in the 'transfer' table
            transferRepository!!.deleteAll()
        }
        val disposable = transferRepository!!.getCount()
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { transferCount ->
                 if (transferCount > 0) {
                    // If we already have some transfers in the database, we might want to skip the request
                    // straight to the last batch
                    historicalTransferCount = Math.floor((transferCount / HISTORICAL_TRANSFER_BATCH_SIZE).toDouble()).toInt()
                }
                // Retrieving account transactions
                loadNextOperationsBatch()
            }
    }

    /**
     * Handles a freshly obtained list of OperationHistory instances. This is how the full node
     * answers our 'get_relative_account_history' API call.
     *
     * This response however, has to be processed before being stored in the local database.
     *
     * @param operationHistoryList List of OperationHistory instances
     */
    private fun handleOperationList(operationHistoryList: List<OperationHistory>) {
        historicalTransferCount++

        val insertedCount = transferRepository!!.insertAll(processOperationList(operationHistoryList))
//        Log.d(TAG, String.format("Inserted count: %d, list size: %d", insertedCount, operationHistoryList.size))
        if (/* insertedCount == 0 && */ operationHistoryList.isEmpty()) {
            // We finally reached the end of the transactions, so now we must check for missing
            // transfer times
//            mState = State.LOADING_MISSING_TIMES
//            missingTimes = database.getMissingTransferTimes(lastMissingTimeBlockNum, MISSING_TIMES_BATCH_SIZE)
//            if (missingTimes!!.size > 0) {
//                // Proceed to load missing times
//                processNextMissingTime()
//            } else {
//                // If we got no missing times, proceed to check for missing equivalent values
//                mState = State.LOADING_EQ_VALUES
//
//                // If we're done loading missing transfer times, we check for missing equivalent values.
//                missingEquivalentValues = database.getMissingEquivalentValues(
//                    lastEquivalentValueBlockNum,
//                    PalmPayDatabase.DEFAULT_EQUIVALENT_VALUE_BATCH_SIZE
//                )
//
//                // Processing one of the missing values entries
//                processNextEquivalentValue()
//            }
        } else {

            // If we inserted more than one operation, we cannot yet be sure we've reached the
            // end of the operation list, so we issue another call to the 'get_relative_account_history'
            // API call
            loadNextOperationsBatch()
        }
    }

    /**
     * Method used to issue a new 'get_relative_account_history' API call. This is expected to retrieve
     * at most HISTORICAL_TRANSFER_BATCH_SIZE operations.
     */
    private fun loadNextOperationsBatch() {
        val stop = historicalTransferCount * HISTORICAL_TRANSFER_BATCH_SIZE
        val start = stop + HISTORICAL_TRANSFER_BATCH_SIZE
        lastId = mNetworkService!!.sendMessage(
            GetRelativeAccountHistory(
                mCurrentAccount,
                stop,
                HISTORICAL_TRANSFER_BATCH_SIZE,
                start
            ), GetRelativeAccountHistory.REQUIRED_API
        )
        responseMap[lastId] = RESPONSE_GET_RELATIVE_ACCOUNT_HISTORY
    }

    /**
     * Method that will transform a list of OperationHistory instances to a list of
     * HistoricalOperationEntry.
     *
     * The HistoricalOperationEntry class is basically a wrapper around the OperationHistory class
     * provided by the Graphenej library. It is used to better reflect what we store in the internal
     * database for every transfer and expands the OperationHistory class basically adding
     * two things:
     *
     * 1- A timestamp
     * 2- An AssetAmount instance to represent the equivalent value in a fiat value
     *
     * @param operations    List of OperationHistory instances
     * @return              List of HistoricalOperationEntry instances
     */
    private fun processOperationList(operations: List<OperationHistory>): List<Transfer> {
        val transfers = ArrayList<Transfer>()

        if (wifKey == null) {
            // In case of key storage corruption, we give up on processing this list of operations
            return transfers
        }
        val memoKey = DumpedPrivateKey.fromBase58(null, wifKey!!).key
        val publicKey = PublicKey(ECKey.fromPublicOnly(memoKey.pubKey))
        val myAddress = Address(publicKey.key)


        for (historicalOp in operations) {
            if (historicalOp.operation == null || historicalOp.operation !is TransferOperation) {
                // Some historical operations might not be transfer operations.
                // As of right now non-transfer operations get deserialized as null
                continue
            }

            val entry = HistoricalOperationEntry()
            val op = historicalOp.operation as TransferOperation

            val memo = op.memo
            if (memo.byteMessage != null) {
                val destinationAddress = memo.destination
                try {
                    if (destinationAddress.toString() == myAddress.toString()) {
                        val decryptedMessage = Memo.decryptMessage(memoKey, memo.source, memo.nonce, memo.byteMessage)
                        memo.plaintextMessage = decryptedMessage
                    }
                } catch (e: ChecksumException) {
                    Log.e(TAG, "ChecksumException. Msg: " + e.message)
                } catch (e: NullPointerException) {
                    // This is expected in case the decryption fails, so no need to log this event.
                    Log.e(TAG, "NullPointerException. Msg: " + e.message)
                } catch (e: Exception) {
                    Log.e(TAG, "Exception while decoding memo. Msg: " + e.message)
                }
            }

            val transfer = Transfer(
                historicalOp.objectId,
                historicalOp.blockNum,
                entry.timestamp,
                op.fee.amount.toLong(),
                op.fee.asset.objectId,
                op.from.objectId,
                op.to.objectId,
                op.assetAmount.amount.toLong(),
                op.assetAmount.asset.objectId,
                memo.plaintextMessage,
                memo.source.toString(),
                memo.destination.toString()
            )
            // TODO build transfer object, save Wif in ImportBrainkeyActivity

            transfers.add(transfer)
        }
        return transfers
    }

//    /**
//     * Handles the response to the 'get_block_header' API call
//     * @param blockHeader   The requested block header
//     */
//    private fun handleMissingTimeResponse(blockHeader: BlockHeader) {
//        if (missingTimes == null || missingTimes!!.size == 0 || mState == State.CANCELLED) {
//            // If the missingTimes attribute is null, this means that the request was probably issued
//            // by another instance of this class.
//            Log.d(TAG, "Cancelling loader instance")
//            mState = State.CANCELLED
//            return
//        }
//        val updated = database.setBlockTime(blockHeader, missingTimes!!.peek())
//        if (!updated) {
//            Log.w(TAG, "Failed to update time from transaction at block: " + missingTimes!!.peek())
//        } else {
//            Log.d(TAG, "Missing time updated")
//        }
//
//        // Removing missing time from stack
//        lastMissingTimeBlockNum = missingTimes!!.poll()
//
//        if (!processNextMissingTime()) {
//            Log.d(
//                TAG,
//                String.format("Checking missing time for transfers later than block %d", lastMissingTimeBlockNum)
//            )
//            missingTimes = database.getMissingTransferTimes(lastMissingTimeBlockNum, MISSING_TIMES_BATCH_SIZE)
//            if (missingTimes!!.size == 0) {
//                // If we have no more missing times to handle, we can finally proceed to the last step, which
//                // is to take care of the missing equivalent values
//                Log.d(TAG, "Would be trying to check equivalent values")
//                // If we're done loading missing transfer times, we check for missing equivalent values.
//                missingEquivalentValues = database.getMissingEquivalentValues(
//                    lastEquivalentValueBlockNum,
//                    PalmPayDatabase.DEFAULT_EQUIVALENT_VALUE_BATCH_SIZE
//                )
//
//                // Processing one of the missing values entries
//                processNextEquivalentValue()
//            } else {
//                processNextMissingTime()
//            }
//        }
//    }

//    private fun processNextEquivalentValue() {
//        if (missingEquivalentValues!!.size > 0) {
//            Log.d(TAG, String.format("Found %d missing equivalent values", missingEquivalentValues!!.size))
//            val missingAssets = database.getMissingAssets()
//            if (missingAssets.size == 0 &&
//                mTransferEntry != null &&
//                mTransferEntry!!.historicalTransfer!!.operation is TransferOperation
//            ) {
//                mTransferEntry = missingEquivalentValues!!.peek()
//                lastEquivalentValueBlockNum = mTransferEntry!!.historicalTransfer!!.blockNum
//                val transferOperation = mTransferEntry!!.historicalTransfer!!.operation as TransferOperation
//                val transferredAsset = transferOperation.assetAmount.asset
//                if (transferredAsset == Constants.bitUSD) {
//                    // Easier case, in which the transferred asset was already the designated
//                    // smartcoin (bitUSD)
//                    mTransferEntry!!.equivalentValue = AssetAmount(
//                        transferOperation.assetAmount.amount,
//                        transferredAsset
//                    )
//                    database.updateEquivalentValue(mTransferEntry)
//
//                    // Removing the now solved equivalent value
//                    missingEquivalentValues!!.poll()
//
//                    // Processing the next missing equivalent value
//                    processNextEquivalentValue()
//                } else if (transferredAsset == Constants.BTS) {
//                    // If the transferred asset was the core BTS currency, then we must just
//                    // perform a single network request to find out how much a single BTS was
//                    // worth at the time of this operation
//                    val quote = Constants.bitUSD
//                    val bucket: Long = 86400
//                    val end = mTransferEntry!!.timestamp * 1000
//                    val start = end - 86400 * 1000
//                    lastId = mNetworkService!!.sendMessage(
//                        GetMarketHistory(transferredAsset, quote, bucket, start, end),
//                        GetMarketHistory.REQUIRED_API
//                    )
//                    responseMap[lastId] = RESPONSE_GET_MARKET_HISTORY
//
//                    // Direct conversion
//                    mConversionType = DIRECT_CONVERSION
//                } else {
//                    // If the transferred asset was not bitUSD not BTS, then we cannot be sure
//                    // that a market for this asset (which can either be a smartcoin or a User Issued Asset)
//                    // even exists. For this reason, we opt to convert this value to BTS first, and
//                    // then find out its equivalent value in bitUSD at the time of the transfer.
//                    Log.d(TAG, "Missing equivalent value was of other denomination")
//                    val quote = Constants.BTS
//                    val bucket: Long = 86400
//                    val end = mTransferEntry!!.timestamp * 1000
//                    val start = end - 86400 * 1000
//                    lastId = mNetworkService!!.sendMessage(
//                        GetMarketHistory(transferredAsset, quote, bucket, start, end),
//                        GetMarketHistory.REQUIRED_API
//                    )
//                    responseMap[lastId] = RESPONSE_GET_MARKET_HISTORY
//
//                    // Indirect conversion
//                    mConversionType = INDIRECT_CONVERSION
//                }
//            }
//        } else {
//            Log.d(TAG, "Reached the end of the equivalent values stack")
//            missingEquivalentValues = database.getMissingEquivalentValues(
//                lastEquivalentValueBlockNum,
//                PalmPayDatabase.DEFAULT_EQUIVALENT_VALUE_BATCH_SIZE
//            )
//
//            Log.d(TAG, String.format("Got %d entries initially", missingEquivalentValues!!.size))
//
//            // Clearing the equivalent values stack from blacklisted item
//            for (i in missingEquivalentValues!!.indices) {
//                val entry = missingEquivalentValues!!.peek()
//                if (eqValueBlacklist[entry.historicalTransfer!!.objectId] != null) {
//                    // Removing this equivalent value from the stack, since we've already tried to solve it
//                    missingEquivalentValues!!.poll()
//
//                    Log.d(TAG, "Removed entry from stack thanks to the black list")
//                }
//            }
//
//            if (missingEquivalentValues!!.size > 0) {
//                Log.d(TAG, "Got a new stack")
//                processNextEquivalentValue()
//            } else {
//                Log.d(TAG, "Really reached the final")
////                missingAssetsLoader = MissingAssetsLoader(mContext, mLifeCycle)
////                missingAccountsLoader = MissingAccountsLoader(mContext!!, mLifeCycle)
//                mState = State.FINISHED
//            }
//        }
//    }

//    /**
//     * Handle the requested past market data, in order to calculate the equivalent value
//     * @param buckets   List of market buckets
//     */
//    private fun handlePastMarketData(buckets: List<BucketObject>) {
//        if (buckets.isNotEmpty()) {
//            // Taking the last bucket from the list
//            val bucket = buckets[buckets.size - 1]
//
//            // Obtaining the transferred amount
//            val transferAmount =
//                (mTransferEntry!!.historicalTransfer!!.operation as TransferOperation).assetAmount
//
//            // Obtaining the full asset data of both base and quote
//            var base = database.fillAssetDetails(bucket.key.base)
//            var quote = database.fillAssetDetails(bucket.key.quote)
//            if (mConversionType == DIRECT_CONVERSION) {
//                // Doing conversion and updating the database
//                val converter = Converter(base, quote, bucket)
//                val convertedBaseValue = converter.convert(transferAmount, Converter.CLOSE_VALUE)
//                val equivalentValue = AssetAmount(UnsignedLong.valueOf(convertedBaseValue), Constants.bitUSD)
//
//                mTransferEntry!!.equivalentValue = equivalentValue
//                database.updateEquivalentValue(mTransferEntry)
//
//                // Removing the now solved equivalent value
//                missingEquivalentValues!!.poll()
//
//                // Process the next equivalent value, in case we have one
//                processNextEquivalentValue()
//            } else if (mConversionType == INDIRECT_CONVERSION) {
//                if (coreCurrencyEqValue == null) {
//                    // We are in the first step of an indirect conversion
//
//                    val originalTransfer =
//                        (mTransferEntry!!.historicalTransfer!!.operation as TransferOperation).assetAmount
//
//                    // Doing conversion and updating the database
//                    val converter = Converter(base, quote, bucket)
//                    val convertedBaseValue = converter.convert(originalTransfer, Converter.CLOSE_VALUE)
//                    coreCurrencyEqValue = AssetAmount(UnsignedLong.valueOf(convertedBaseValue), base)
//
//                    base = database.fillAssetDetails(Constants.BTS)
//                    quote = database.fillAssetDetails(Constants.bitUSD)
//
//                    // Performing the 2nd step of the equivalent value calculation. We already hold the
//                    // relationship XXX -> BTS, now we need the BTS -> bitUSD for this time bucket.
//                    val bucketWindow: Long = 86400
//                    val end = mTransferEntry!!.timestamp * 1000
//                    val start = end - 86400 * 1000
//                    lastId = mNetworkService!!.sendMessage(
//                        GetMarketHistory(base, quote, bucketWindow, start, end),
//                        GetMarketHistory.REQUIRED_API
//                    )
//                    responseMap[lastId] = RESPONSE_GET_MARKET_HISTORY
//                } else {
//                    // We are in the second step of an indirect transaction
//
//                    // Doing the conversion
//                    val converter = Converter(base, quote, bucket)
//                    val convertedFinalValue = converter.convert(coreCurrencyEqValue, Converter.CLOSE_VALUE)
//
//                    // Obtaining the equivalent value in bitUSD
//                    val equivalentValue = AssetAmount(UnsignedLong.valueOf(convertedFinalValue), Constants.bitUSD)
//
//                    mTransferEntry!!.equivalentValue = equivalentValue
//                    database.updateEquivalentValue(mTransferEntry)
//
//                    // Removing the now solved equivalent value
//                    missingEquivalentValues!!.poll()
//
//                    // Re-setting some fields
//                    mConversionType = -1
//                    coreCurrencyEqValue = null
//
//                    // Process the next equivalent value, in case we have one
//                    processNextEquivalentValue()
//                }
//            }
//        } else {
//            // Removing the for-now skipped equivalent value
//            val skipped = missingEquivalentValues!!.poll()
//
//            // Adding the skipped operation to a blacklist
//            eqValueBlacklist[skipped.historicalTransfer!!.objectId] = true
//
//            Log.w(TAG, "Got no buckets in the selected period")
//            processNextEquivalentValue()
//        }
//    }

//    /**
//     * Method used to issue a new 'get_block_header' API call to the full node, in case we
//     * still have some missing times left in the missingTimes list.
//     *
//     * @return True if we did issue the API, false if the list was empty and nothing was done
//     */
//    private fun processNextMissingTime(): Boolean {
//        if (missingTimes!!.size > 0) {
//            val blockNum = missingTimes!!.peek()
//            lastId = mNetworkService!!.sendMessage(GetBlockHeader(blockNum!!), GetBlockHeader.REQUIRED_API)
//            return true
//        } else {
//            return false
//        }
//    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    internal fun onDestroy() {
        if (mDisposable != null && !mDisposable!!.isDisposed) mDisposable!!.dispose()
        if (mShouldUnbindNetwork) {
            mContext!!.unbindService(mNetworkServiceConnection)
            mShouldUnbindNetwork = false
        }
        mContext = null
    }
}
