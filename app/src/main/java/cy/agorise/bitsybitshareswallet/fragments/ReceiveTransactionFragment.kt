package cy.agorise.bitsybitshareswallet.fragments

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.common.primitives.UnsignedLong
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.jakewharton.rxbinding3.widget.textChanges
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.adapters.AssetsAdapter
import cy.agorise.bitsybitshareswallet.adapters.AutoSuggestAssetAdapter
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.Helper
import cy.agorise.bitsybitshareswallet.viewmodels.AssetViewModel
import cy.agorise.bitsybitshareswallet.viewmodels.UserAccountViewModel
import cy.agorise.graphenej.*
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.android.NetworkService
import cy.agorise.graphenej.api.android.RxBus
import cy.agorise.graphenej.api.calls.ListAssets
import cy.agorise.graphenej.models.JsonRpcResponse
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_receive_transaction.*
import java.lang.Exception
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class ReceiveTransactionFragment : Fragment(), ServiceConnection {
    private val TAG = this.javaClass.simpleName

    private val RESPONSE_LIST_ASSETS = 1
    private val REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 100

    /** Number of assets to request from the NetworkService to show as suggestions in the AutoCompleteTextView */
    private val AUTO_SUGGEST_ASSET_LIMIT = 5

    private val OTHER_ASSET = "other_asset"

    private lateinit var mUserAccountViewModel: UserAccountViewModel
    private lateinit var mAssetViewModel: AssetViewModel

    /** Current user account */
    private var mUserAccount: UserAccount? = null

    private var mDisposables = CompositeDisposable()

    private var mAsset: Asset? = null

    private var mAssetsAdapter: AssetsAdapter? = null

    private lateinit var mAutoSuggestAssetAdapter: AutoSuggestAssetAdapter

    private var mAssets = ArrayList<cy.agorise.bitsybitshareswallet.database.entities.Asset>()

    private var selectedAssetSymbol = ""

    /** Used to avoid erasing the QR code when the user selects an item from the AutoComplete suggestions */
    private var selectedInAutoCompleteTextView = false

    // Map used to keep track of request and response id pairs
    private val responseMap = HashMap<Long, Int>()

    /* Network service connection */
    private var mNetworkService: NetworkService? = null

    /** Flag used to keep track of the NetworkService binding state */
    private var mShouldUnbindNetwork: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.fragment_receive_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configure UserAccountViewModel to show the current account
        mUserAccountViewModel = ViewModelProviders.of(this).get(UserAccountViewModel::class.java)

        val userId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "")

        mUserAccountViewModel.getUserAccount(userId!!).observe(this,
            Observer<cy.agorise.bitsybitshareswallet.database.entities.UserAccount>{ user ->
                mUserAccount = UserAccount(user.id, user.name)
        })

        // Configure Assets spinner to show Assets already saved into the db
        mAssetViewModel = ViewModelProviders.of(this).get(AssetViewModel::class.java)

        mAssetViewModel.getAll().observe(this,
            Observer<List<cy.agorise.bitsybitshareswallet.database.entities.Asset>> { assets ->
                mAssets.clear()
                mAssets.addAll(assets)

                // Add an option at the end so the user can search for an asset other than the ones saved in the db
                val asset = cy.agorise.bitsybitshareswallet.database.entities.Asset(
                    OTHER_ASSET, "Other...", 0, "", ""
                )
                mAssets.add(asset)

                mAssetsAdapter = AssetsAdapter(context!!, android.R.layout.simple_spinner_item, mAssets)
                spAsset.adapter = mAssetsAdapter

                // Try to select the selectedAssetSymbol
                for (i in 0 until mAssetsAdapter!!.count) {
                    if (mAssetsAdapter!!.getItem(i)!!.symbol == selectedAssetSymbol) {
                        spAsset.setSelection(i)
                        break
                    }
                }
        })

        spAsset.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) { }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val asset = mAssetsAdapter!!.getItem(position)!!

                if (asset.id == OTHER_ASSET) {
                    tilAsset.visibility = View.VISIBLE
                    mAsset = null
                } else {
                    tilAsset.visibility = View.GONE
                    selectedAssetSymbol = asset.symbol

                    mAsset = Asset(asset.id, asset.symbol, asset.precision)
                }
                updateQR()
            }
        }

        // Use RxJava Debounce to create QR code only after the user stopped typing an amount
        mDisposables.add(
            tietAmount.textChanges()
                .debounce(1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateQR() }
        )

        // Add adapter to the Assets AutoCompleteTextView
        mAutoSuggestAssetAdapter = AutoSuggestAssetAdapter(context!!, android.R.layout.simple_dropdown_item_1line)
        actvAsset.setAdapter(mAutoSuggestAssetAdapter)

        // Use RxJava Debounce to avoid making calls to the NetworkService on every text change event and also avoid
        // the first call when the View is created
        mDisposables.add(
            actvAsset.textChanges()
                .skipInitialValue()
                .debounce(500, TimeUnit.MILLISECONDS)
                .map { it.toString().trim().toUpperCase() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (!selectedInAutoCompleteTextView) {
                        mAsset = null
                        updateQR()
                    }
                    selectedInAutoCompleteTextView = false

                    // Get a list of assets that match the already typed string by the user
                    if (it.length > 1 && mNetworkService != null) {
                        val id = mNetworkService!!.sendMessage(ListAssets(it, AUTO_SUGGEST_ASSET_LIMIT),
                            ListAssets.REQUIRED_API)
                        responseMap[id] = RESPONSE_LIST_ASSETS
                    }
                }
        )

        actvAsset.setOnItemClickListener { parent, _, position, _ ->
            val asset = parent.adapter.getItem(position) as cy.agorise.bitsybitshareswallet.database.entities.Asset
            mAsset = Asset(asset.id, asset.symbol, asset.precision)
            selectedInAutoCompleteTextView = true
            updateQR()
        }

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
                    RESPONSE_LIST_ASSETS            -> handleListAssets(message.result as List<Asset>)
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

    private fun handleListAssets(assetList: List<Asset>) {
        Log.d(TAG, "handleListAssets")
        val assets = ArrayList<cy.agorise.bitsybitshareswallet.database.entities.Asset>()
        for (_asset in assetList) {
            val asset = cy.agorise.bitsybitshareswallet.database.entities.Asset(
                _asset.objectId,
                _asset.symbol,
                _asset.precision,
                _asset.description ?: "",
                _asset.bitassetId ?: ""
            )

            assets.add(asset)
        }
        mAutoSuggestAssetAdapter.setData(assets)
        mAutoSuggestAssetAdapter.notifyDataSetChanged()
    }

    private fun updateQR() {
        if (mAsset == null) {
            ivQR.setImageDrawable(null)
            // TODO clean the please pay and to text at the bottom too
            return
        }

        // Try to obtain the amount from the Amount Text Field or make it zero otherwise
        val amount: Long = try {
            val tmpAmount = tietAmount.text.toString().toDouble()
            (tmpAmount * Math.pow(10.0, mAsset!!.precision.toDouble())).toLong()
        }catch (e: Exception) {
            0
        }

        val total = AssetAmount(UnsignedLong.valueOf(amount), mAsset!!)
        val totalInDouble = Util.fromBase(total)
        val items = arrayOf(LineItem("transfer", 1, totalInDouble))
        val invoice = Invoice(mUserAccount!!.name, "", "#bitsy", mAsset!!.symbol, items, "", "")
        Log.d(TAG, "invoice: " + invoice.toJsonString())
        try {
            val bitmap = encodeAsBitmap(Invoice.toQrCode(invoice), "#139657") // PalmPay green
            ivQR.setImageBitmap(bitmap)
            updateAmountAddressUI(total, mUserAccount!!.name)
        } catch (e: WriterException) {
            Log.e(TAG, "WriterException. Msg: " + e.message)
        }
    }

    /**
     * Encodes the provided data as a QR-code. Used to provide payment requests.
     * @param data: Data containing payment request data as the recipient's address and the requested amount.
     * @param color: The color used for the QR-code
     * @return Bitmap with the QR-code encoded data
     * @throws WriterException if QR Code cannot be generated
     */
    @Throws(WriterException::class)
    internal fun encodeAsBitmap(data: String, color: String): Bitmap? {
        val result: BitMatrix

        // Get measured width and height of the ImageView where the QR code will be placed
        var w = ivQR.width
        var h = ivQR.height

        // Gets minimum side length and sets both width and height to that value so the final
        // QR code has a squared shape
        val minSide = if (w < h) w else h
        h = minSide
        w = h

        try {
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = 0
            result = MultiFormatWriter().encode(
                data,
                BarcodeFormat.QR_CODE, w, h, hints
            )
        } catch (iae: IllegalArgumentException) {
            // Unsupported format
            return null
        }

        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (result.get(x, y)) Color.parseColor(color) else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }

    /**
     * Updates the UI to show amount and account to send the payment
     *
     * @param total Total Amount in crypto to be paid
     * @param account Account to pay total
     */
    private fun updateAmountAddressUI(total: AssetAmount, account: String) {
        val df = DecimalFormat("####."+("#".repeat(total.asset.precision)))
        df.roundingMode = RoundingMode.CEILING
        df.decimalFormatSymbols = DecimalFormatSymbols(Locale.getDefault())

        val amount = total.amount.toDouble() / Math.pow(10.toDouble(), total.asset.precision.toDouble())
        val strAmount = df.format(amount)

        val txtAmount = getString(R.string.template__please_pay, strAmount, total.asset.symbol)
        val txtAccount = getString(R.string.template__to, account)

        tvPleasePay.text = txtAmount
        tvTo.text = txtAccount
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_receive_transaction, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.menu_share) {
            verifyStoragePermission()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun verifyStoragePermission() {
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not already granted
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION)
        } else {
            // Permission is already granted
            shareQRScreenshot()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                shareQRScreenshot()
            } else {
                // TODO extract string resource
                Toast.makeText(context!!, "Storage permission is necessary to share QR codes.", Toast.LENGTH_SHORT).show()
            }
            return
        }
    }

    /**
     * This function takes a screenshot as a bitmap, saves it into a temporal cache image and then
     * sends an intent so the user can select the desired method to share the image.
     */
    private fun shareQRScreenshot() {
        // TODO improve, show errors where necessary so the user can fix it
        // Avoid sharing the QR code image if the fields are not filled correctly
        if (mAsset == null)
            return

        // Get Screenshot
        val screenshot = Helper.loadBitmapFromView(container)
        val imageUri = Helper.saveTemporalBitmap(context!!, screenshot)

        // Prepare information for share intent
        val subject = getString(R.string.msg__invoice_subject, mUserAccount?.name)
        val content = tvPleasePay.text.toString() + "\n" +
                tvTo.text.toString()

        // Create share intent and call it
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        shareIntent.putExtra(Intent.EXTRA_TEXT, content)
        shareIntent.type = "*/*"
        startActivity(Intent.createChooser(shareIntent, getString(R.string.text__share_with)))
    }

    override fun onResume() {
        super.onResume()

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