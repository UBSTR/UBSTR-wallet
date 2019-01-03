package cy.agorise.bitsybitshareswallet.fragments

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.common.primitives.UnsignedLong
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.jakewharton.rxbinding3.widget.textChanges
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.adapters.BalancesDetailsAdapter
import cy.agorise.bitsybitshareswallet.database.joins.BalanceDetail
import cy.agorise.bitsybitshareswallet.repositories.AuthorityRepository
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.CryptoUtils
import cy.agorise.bitsybitshareswallet.utils.disable
import cy.agorise.bitsybitshareswallet.utils.enable
import cy.agorise.bitsybitshareswallet.viewmodels.BalanceDetailViewModel
import cy.agorise.graphenej.*
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.android.NetworkService
import cy.agorise.graphenej.api.android.RxBus
import cy.agorise.graphenej.api.calls.BroadcastTransaction
import cy.agorise.graphenej.api.calls.GetAccountByName
import cy.agorise.graphenej.api.calls.GetDynamicGlobalProperties
import cy.agorise.graphenej.api.calls.GetRequiredFees
import cy.agorise.graphenej.models.AccountProperties
import cy.agorise.graphenej.models.DynamicGlobalProperties
import cy.agorise.graphenej.models.JsonRpcResponse
import cy.agorise.graphenej.operations.TransferOperationBuilder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_send_transaction.*
import me.dm7.barcodescanner.zxing.ZXingScannerView
import org.bitcoinj.core.DumpedPrivateKey
import org.bitcoinj.core.ECKey
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.ArrayList
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.crypto.AEADBadTagException

class SendTransactionFragment : Fragment(), ZXingScannerView.ResultHandler, ServiceConnection {
    private val TAG = this.javaClass.simpleName

    // Camera Permission
    private val REQUEST_CAMERA_PERMISSION = 1

    private val RESPONSE_GET_ACCOUNT_BY_NAME = 1
    private val RESPONSE_GET_DYNAMIC_GLOBAL_PARAMETERS = 2
    private val RESPONSE_GET_REQUIRED_FEES = 3
    private val RESPONSE_BROADCAST_TRANSACTION = 4

    private var isCameraPreviewVisible = false
    private var isToAccountCorrect = false
    private var isAmountCorrect = false

    private var mBalancesDetails: List<BalanceDetail>? = null

    private lateinit var mBalanceDetailViewModel: BalanceDetailViewModel

    private var mBalancesDetailsAdapter: BalancesDetailsAdapter? = null

    private var selectedAssetSymbol = ""

    /** Current user account */
    private var mUserAccount: UserAccount? = null

    /** User account to which send the funds */
    private var mSelectedUserAccount: UserAccount? = null

    private var mDisposables = CompositeDisposable()

    /* Network service connection */
    private var mNetworkService: NetworkService? = null

    /** Flag used to keep track of the NetworkService binding state */
    private var mShouldUnbindNetwork: Boolean = false

    // Map used to keep track of request and response id pairs
    private val responseMap = HashMap<Long, Int>()

    private var transaction: Transaction? = null

    /** Variable holding the current user's private key in the WIF format */
    private var wifKey: String? = null

    /** Repository to access and update Authorities */
    private var authorityRepository: AuthorityRepository? = null

    /* This is one of the of the recipient account's public key, it will be used for memo encoding */
    private var destinationPublicKey: PublicKey? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_send_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "")

        if (userId != "")
            mUserAccount = UserAccount(userId)

        // Use Navigation SafeArgs to decide if we should activate or not the camera feed
        val safeArgs = SendTransactionFragmentArgs.fromBundle(arguments!!)
        if (safeArgs.openCamera)
            verifyCameraPermission()

        fabOpenCamera.setOnClickListener { if (isCameraPreviewVisible) stopCameraPreview() else verifyCameraPermission() }

        // Configure BalanceDetailViewModel to show the current balances
        mBalanceDetailViewModel = ViewModelProviders.of(this).get(BalanceDetailViewModel::class.java)

        mBalanceDetailViewModel.getAll().observe(this, Observer<List<BalanceDetail>> { balancesDetails ->
            mBalancesDetails = balancesDetails
            mBalancesDetailsAdapter = BalancesDetailsAdapter(context!!, android.R.layout.simple_spinner_item, mBalancesDetails!!)
            spAsset.adapter = mBalancesDetailsAdapter

            // Try to select the selectedAssetSymbol
            for (i in 0 until mBalancesDetailsAdapter!!.count) {
                if (mBalancesDetailsAdapter!!.getItem(i)!!.symbol == selectedAssetSymbol) {
                    spAsset.setSelection(i)
                    break
                }
            }
        })

        spAsset.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) { }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val balance = mBalancesDetailsAdapter!!.getItem(position)!!
                selectedAssetSymbol = balance.symbol

                val amount = balance.amount.toDouble() / Math.pow(10.0, balance.precision.toDouble())

                tvAvailableAssetAmount.text =
                        String.format("%." + Math.min(balance.precision, 8) + "f %s", amount, balance.symbol)
            }
        }

        fabSendTransaction.setOnClickListener { startSendTransferOperation() }
        fabSendTransaction.disable(R.color.lightGray)

        authorityRepository = AuthorityRepository(context!!)

        mDisposables.add(
            authorityRepository!!.getWIF(userId!!, AuthorityType.ACTIVE.ordinal)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { encryptedWIF ->
                    try {
                        wifKey = CryptoUtils.decrypt(context!!, encryptedWIF)
                    } catch (e: AEADBadTagException) {
                        Log.e(TAG, "AEADBadTagException. Class: " + e.javaClass + ", Msg: " + e.message)
                    }

                }
        )

        // Use RxJava Debounce to avoid making calls to the NetworkService on every text change event
        mDisposables.add(
            tietTo.textChanges()
                .debounce(500, TimeUnit.MILLISECONDS)
                .map { it.toString().trim() }
                .filter { it.length > 1 }
                .subscribe {
                    val id = mNetworkService!!.sendMessage(GetAccountByName(it!!), GetAccountByName.REQUIRED_API)
                    responseMap[id] = RESPONSE_GET_ACCOUNT_BY_NAME
                }
        )

        // Use RxJava Debounce to update the Amount error only after the user stops writing for > 500 ms
        mDisposables.add(
            tietAmount.textChanges()
                .debounce(500, TimeUnit.MILLISECONDS)
                .filter { it.isNotEmpty() }
                .map { it.toString().trim().toDouble() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { validateAmount(it!!) }
        )

        // Connect to the RxBus, which receives events from the NetworkService
        mDisposables.add(
            RxBus.getBusInstance()
                .asFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { handleIncomingMessage(it) }
        )
    }

    private fun handleIncomingMessage(message: Any?) {
        if (message is JsonRpcResponse<*>) {
            if (responseMap.containsKey(message.id)) {
                val responseType = responseMap[message.id]
                when (responseType) {
                    RESPONSE_GET_ACCOUNT_BY_NAME            -> handleAccountName(message.result)
                    RESPONSE_GET_DYNAMIC_GLOBAL_PARAMETERS  -> handleDynamicGlobalProperties(message.result)
                    RESPONSE_GET_REQUIRED_FEES              -> handleRequiredFees(message.result)
                    RESPONSE_BROADCAST_TRANSACTION          -> handleBroadcastTransaction(message)
                }
                responseMap.remove(message.id)
            }
        } else if (message is ConnectionStatusUpdate) {
            if (message.updateCode == ConnectionStatusUpdate.DISCONNECTED) {
                // If we got a disconnection notification, we should clear our response map, since
                // all its stored request ids will now be reset
                responseMap.clear()
            }
        }
    }

    private fun handleAccountName(result: Any?) {
        if (result is AccountProperties) {
            mSelectedUserAccount = UserAccount(result.id, result.name)
            destinationPublicKey = result.active.keyAuths.keys.iterator().next()
            tilTo.isErrorEnabled = false
            isToAccountCorrect = true
        } else {
            mSelectedUserAccount = null
            destinationPublicKey = null
            tilTo.error = "Invalid account"
            isToAccountCorrect = false
        }

        enableDisableSendFAB()
    }

    private fun handleDynamicGlobalProperties(result: Any?) {
        if (result is DynamicGlobalProperties) {
            val expirationTime = (result.time.time / 1000) + Transaction.DEFAULT_EXPIRATION_TIME
            val headBlockId = result.head_block_id
            val headBlockNumber = result.head_block_number

            transaction!!.blockData = BlockData(headBlockNumber, headBlockId, expirationTime)

            val asset = Asset(mBalancesDetailsAdapter!!.getItem(spAsset.selectedItemPosition)!!.id)

            val id = mNetworkService!!.sendMessage(GetRequiredFees(transaction!!, asset), GetRequiredFees.REQUIRED_API)
            responseMap[id] = RESPONSE_GET_REQUIRED_FEES
        } else {
            // TODO unableToSendTransactionError()
        }
    }

    private fun handleRequiredFees(result: Any?) {
        if (result is List<*> && result[0] is AssetAmount) {
            Log.d(TAG, "GetRequiredFees: " + transaction.toString())
            transaction!!.setFees(result as List<AssetAmount>) // TODO find how to remove this warning

            val id = mNetworkService!!.sendMessage(BroadcastTransaction(transaction), BroadcastTransaction.REQUIRED_API)
            responseMap[id] = RESPONSE_BROADCAST_TRANSACTION
        } else {
            // TODO unableToSendTransactionError()
        }
    }

    private fun handleBroadcastTransaction(message: JsonRpcResponse<*>) {
        if (message.result == null && message.error == null) {
            // TODO extract string resources
            Toast.makeText(context!!, "Transaction sent!", Toast.LENGTH_SHORT).show()

            // Remove information from the text fields and disable send button
            tietTo.setText("")
            tietAmount.setText("")
            tietMemo.setText("")
            isToAccountCorrect = false
            isAmountCorrect = false
            enableDisableSendFAB()
        } else {
            // TODO extract error messages to show a better explanation to the user
            Toast.makeText(context!!, message.error.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun verifyCameraPermission() {
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not already granted
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            // Permission is already granted
            startCameraPreview()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startCameraPreview()
            } else {
                // TODO extract string resource
                Toast.makeText(context!!, "Camera permission is necessary to read QR codes.", Toast.LENGTH_SHORT).show()
            }
            return
        }
    }

    private fun startCameraPreview() {
        cameraPreview.visibility = View.VISIBLE
        fabOpenCamera.setImageResource(R.drawable.ic_close)
        isCameraPreviewVisible = true

        // Configure QR scanner
        cameraPreview.setFormats(listOf(BarcodeFormat.QR_CODE))
        cameraPreview.setAspectTolerance(0.5f)
        cameraPreview.setAutoFocus(true)
        cameraPreview.setLaserColor(R.color.colorAccent)
        cameraPreview.setMaskColor(R.color.colorAccent)
        cameraPreview.setResultHandler(this)
        cameraPreview.startCamera()
    }

    private fun stopCameraPreview() {
        cameraPreview.visibility = View.INVISIBLE
        fabOpenCamera.setImageResource(R.drawable.ic_camera)
        isCameraPreviewVisible = false
        cameraPreview.stopCamera()
    }

    override fun handleResult(result: Result?) {
        try {
            val invoice = Invoice.fromQrCode(result!!.text)

            Log.d(TAG, "QR Code read: " + invoice.toJsonString())

            tietTo.setText(invoice.to)

            for (i in 0 until mBalancesDetailsAdapter!!.count) {
                if (mBalancesDetailsAdapter!!.getItem(i)!!.symbol == invoice.currency.toUpperCase()) {
                    spAsset.setSelection(i)
                    break
                }
            }
            tietMemo.setText(invoice.memo)


            var amount = 0.0
            for (nextItem in invoice.lineItems) {
                amount += nextItem.quantity * nextItem.price
            }
            // TODO Improve pattern to account for different asset precisions
            val df = DecimalFormat("####.#####")
            df.roundingMode = RoundingMode.CEILING
            df.decimalFormatSymbols = DecimalFormatSymbols(Locale.getDefault())
            tietAmount.setText(df.format(amount))

        }catch (e: Exception) {
            Log.d(TAG, "Invoice error: " + e.message)
        }
    }

    private fun validateAmount(amount: Double) {
        if (mBalancesDetailsAdapter?.isEmpty != false) return
        val balance = mBalancesDetailsAdapter?.getItem(spAsset.selectedItemPosition) ?: return
        val currentAmount = balance.amount.toDouble() / Math.pow(10.0, balance.precision.toDouble())

        if (currentAmount < amount) {
            // TODO extract string resource
            tilAmount.error = "Not enough funds"
            isAmountCorrect = false
        } else {
            tilAmount.isErrorEnabled = false
            isAmountCorrect = true
        }

        enableDisableSendFAB()
    }

    private fun enableDisableSendFAB() {
        if (isToAccountCorrect && isAmountCorrect)
            fabSendTransaction.enable(R.color.colorSend)
        else
            fabSendTransaction.disable(R.color.lightGray)
    }

    private fun startSendTransferOperation() {
        // Create TransferOperation
        if (mNetworkService!!.isConnected) {
            val balance = mBalancesDetailsAdapter!!.getItem(spAsset.selectedItemPosition)!!
            val amount = (tietAmount.text.toString().toDouble() * Math.pow(10.0, balance.precision.toDouble())).toLong()

            val transferAmount = AssetAmount(UnsignedLong.valueOf(amount), Asset(balance.id))

            val operationBuilder = TransferOperationBuilder()
                .setSource(mUserAccount)
                .setDestination(mSelectedUserAccount)
                .setTransferAmount(transferAmount)

            val privateKey = ECKey.fromPrivate(DumpedPrivateKey.fromBase58(null, wifKey).key.privKeyBytes)

            // Add memo if exists TODO enable memo
//            val memoMsg = tietMemo.text.toString()
//            if (memoMsg.isNotEmpty()) {
//                val nonce = SecureRandomGenerator.getSecureRandom().nextLong().toBigInteger()
//                val encryptedMemo = Memo.encryptMessage(privateKey, destinationPublicKey!!, nonce, memoMsg)
//                val from = Address(ECKey.fromPublicOnly(privateKey.pubKey))
//                val to = Address(destinationPublicKey!!.key)
//                val memo = Memo(from, to, nonce, encryptedMemo)
//                operationBuilder.setMemo(memo)
//            }

            val operations = ArrayList<BaseOperation>()
            operations.add(operationBuilder.build())

            transaction = Transaction(privateKey, null, operations)

            val id = mNetworkService!!.sendMessage(GetDynamicGlobalProperties(),
                GetDynamicGlobalProperties.REQUIRED_API)
            responseMap[id] =  RESPONSE_GET_DYNAMIC_GLOBAL_PARAMETERS
        } else
            Log.d(TAG, "Network Service is not connected")
    }

    override fun onResume() {
        super.onResume()
        if (isCameraPreviewVisible)
            startCameraPreview()

        val intent = Intent(context, NetworkService::class.java)
        if (context?.bindService(intent, this, Context.BIND_AUTO_CREATE) == true) {
            mShouldUnbindNetwork = true
        } else {
            Log.e(TAG, "Binding to the network service failed.")
        }
    }

    override fun onPause() {
        super.onPause()

        // Unbinding from network service
        if (mShouldUnbindNetwork) {
            context?.unbindService(this)
            mShouldUnbindNetwork = false
        }

        if (!isCameraPreviewVisible)
            stopCameraPreview()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!mDisposables.isDisposed) mDisposables.dispose()
    }

    override fun onServiceDisconnected(name: ComponentName?) { }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        // We've bound to LocalService, cast the IBinder and get LocalService instance
        val binder = service as NetworkService.LocalBinder
        mNetworkService = binder.service
    }
}
