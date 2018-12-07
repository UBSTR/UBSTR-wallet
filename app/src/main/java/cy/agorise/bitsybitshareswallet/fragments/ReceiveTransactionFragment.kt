package cy.agorise.bitsybitshareswallet.fragments

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.common.primitives.UnsignedLong
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.jakewharton.rxbinding2.widget.RxTextView
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.viewmodels.AssetViewModel
import cy.agorise.bitsybitshareswallet.viewmodels.UserAccountViewModel
import cy.agorise.graphenej.*
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_receive_transaction.*
import java.util.*
import java.util.concurrent.TimeUnit

class ReceiveTransactionFragment : Fragment() {
    private val TAG = this.javaClass.simpleName

    private lateinit var mUserAccountViewModel: UserAccountViewModel
    private lateinit var mAssetViewModel: AssetViewModel

    /** Current user account */
    private var mUserAccount: UserAccount? = null

    private var mDisposables = CompositeDisposable()

    private var mAsset: Asset? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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

        mAssetViewModel = ViewModelProviders.of(this).get(AssetViewModel::class.java)

        mAssetViewModel.getAll().observe(this,
            Observer<List<cy.agorise.bitsybitshareswallet.database.entities.Asset>> { assets ->
                val adapter = ArrayAdapter<cy.agorise.bitsybitshareswallet.database.entities.Asset>(context!!,
                    android.R.layout.simple_dropdown_item_1line, assets)
                actvAsset.setAdapter(adapter)
        })

        actvAsset.setOnItemClickListener { parent, _, position, _ ->
            val asset = parent.adapter.getItem(position) as cy.agorise.bitsybitshareswallet.database.entities.Asset
            mAsset = Asset(asset.id, asset.symbol, asset.precision)
            updateQR()
        }

        // Use RxJava Debounce to avoid making calls to the NetworkService on every text change event
//        mDisposables.add(
//            RxTextView.textChanges(tietAmount)
//                .debounce(500, TimeUnit.MILLISECONDS)
//                .map { it.toString().trim() }
//                .filter { it.length > 1 }
//                .subscribe {
//
//                }
//        )
    }

    private fun updateQR() {
        val amount: Long = 10

        val total = Util.fromBase(AssetAmount(UnsignedLong.valueOf(amount), mAsset!!))
        val items = arrayOf(LineItem("transfer", 1, total))
        val invoice = Invoice(mUserAccount!!.name, "", "#bitsy", mAsset!!.symbol, items, "", "")
        Log.d(TAG, "invoice: " + invoice.toJsonString())
        try {
            val bitmap = encodeAsBitmap(Invoice.toQrCode(invoice), "#139657") // PalmPay green
            ivQR.setImageBitmap(bitmap)
//            updateAmountAddressUI(total, mUserAccount!!.getName())
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

//    /**
//     * Updates the UI to show amount and address to send the payment
//     *
//     * @param amount Amount in crypto to be paid
//     * @param address Address to pay amount
//     */
//    private fun updateAmountAddressUI(amount: Double, account: String) {
//        // Trick to format correctly really small floating point numbers
//        val df = DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
//        df.maximumFractionDigits = 340
//
//        val cryptoAmount = Helper.getCryptoAmountLocaleFormatted(
//            locale, amount,
//            inputCoinType.toLowerCase(), this
//        )
//
//        val txtAmount = getString(R.string.please_pay_s_s, cryptoAmount, inputCoinType.toUpperCase())
//        val txtAddress = getString(R.string.to_s, account)
//
//        tvTotalCryptoAmount.setText(txtAmount)
//        tvReceivingAddress.setText(txtAddress)
//    }
}