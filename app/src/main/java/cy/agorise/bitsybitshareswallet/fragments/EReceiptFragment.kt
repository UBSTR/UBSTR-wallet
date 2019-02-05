package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs

import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetail
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.viewmodels.EReceiptViewModel
import kotlinx.android.synthetic.main.fragment_e_receipt.*
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class EReceiptFragment : Fragment() {

    private val args: EReceiptFragmentArgs by navArgs()

    private lateinit var mEReceiptViewModel: EReceiptViewModel
    private lateinit var mLocale: Locale

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.fragment_e_receipt, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mLocale = resources.configuration.locale

        val userId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "") ?: ""

        val transferId = args.transferId

        mEReceiptViewModel = ViewModelProviders.of(this).get(EReceiptViewModel::class.java)

        mEReceiptViewModel.get(userId, transferId).observe(this, Observer<TransferDetail> { transferDetail ->
            bindTransferDetail(transferDetail)
        })
    }

    private fun bindTransferDetail(transferDetail: TransferDetail) {
        vPaymentDirection.setBackgroundColor(resources.getColor(
            if(transferDetail.direction) R.color.colorReceive else R.color.colorSend
        ))

        tvFrom.text = transferDetail.from ?: ""
        tvTo.text = transferDetail.to ?: ""

        // Show the crypto amount correctly formatted
        val df = DecimalFormat("####."+("#".repeat(transferDetail.assetPrecision)))
        df.roundingMode = RoundingMode.CEILING
        df.decimalFormatSymbols = DecimalFormatSymbols(Locale.getDefault())

        val amount = transferDetail.assetAmount.toDouble() /
                Math.pow(10.toDouble(), transferDetail.assetPrecision.toDouble())
        val assetAmount = "${df.format(amount)} ${transferDetail.getUIAssetSymbol()}"
        tvAmount.text = assetAmount

        // TODO show the equivalent value
        tvEquivalentValue.text = "-"

        // Memo
        if (transferDetail.memo != "")
            tvMemo.text = getString(R.string.template__memo, transferDetail.memo)
        else
            tvMemo.visibility = View.GONE

        // Date
        val dateFormat = SimpleDateFormat("dd MMM HH:mm:ss z", mLocale)
        tvDate.text = getString(R.string.template__date, dateFormat.format(transferDetail.date * 1000))

        // Transaction #
        formatTransferTextView(transferDetail.id)
    }

    /** Formats the transfer TextView to show a link to explore the given transfer
     * in a BitShares explorer */
    private fun formatTransferTextView(transferId: String) {
        val tx = Html.fromHtml(getString(R.string.template__tx,
            "<a href=\"http://bitshares-explorer.io/#/operations/$transferId\">$transferId</a>"
        ))
        tvTransferID.text = tx
        tvTransferID.movementMethod = LinkMovementMethod.getInstance()
    }
}