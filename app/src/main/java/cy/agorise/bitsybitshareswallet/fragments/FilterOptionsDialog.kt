package cy.agorise.bitsybitshareswallet.fragments


import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import androidx.fragment.app.DialogFragment
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.Observer
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.adapters.BalancesDetailsAdapter
import cy.agorise.bitsybitshareswallet.database.joins.BalanceDetail
import cy.agorise.bitsybitshareswallet.viewmodels.BalanceDetailViewModel
import cy.agorise.bitsybitshareswallet.views.DatePickerFragment
import java.text.SimpleDateFormat
import java.util.*
import kotlin.ClassCastException


/**
 * Creates a Dialog that communicates with {@link TransactionsActivity} to give it parameters about
 * how to filter the list of Transactions
 */
class FilterOptionsDialog : DialogFragment() {

    companion object {

        const val KEY_FILTER_TRANSACTION_DIRECTION = "key_filter_transaction_direction"
        const val KEY_FILTER_DATE_RANGE_ALL = "key_filter_date_range_all"
        const val KEY_FILTER_START_DATE = "key_filter_start_date"
        const val KEY_FILTER_END_DATE = "key_filter_end_date"
        const val KEY_FILTER_ASSET_ALL = "key_filter_asset_all"
        const val KEY_FILTER_ASSET = "key_filter_asset"
        const val KEY_FILTER_FIAT_AMOUNT_ALL = "key_filter_fiat_amount_all"
        const val KEY_FILTER_FROM_FIAT_AMOUNT = "filter_from_fiat_amount"
        const val KEY_FILTER_TO_FIAT_AMOUNT = "filter_to_fiat_amount"

        const val KEY_TIMESTAMP = "key_timestamp"

        const val START_DATE_PICKER = 0
        const val END_DATE_PICKER = 1

        fun newInstance(filterTransactionsDirection: Int, filterDateRangeAll: Boolean,
                        filterStartDate: Long, filterEndDate: Long, filterAssetAll: Boolean,
                        filterAsset: String, filterFiatAmountAll: Boolean,
                        filterFromFiatAmount: Long, filterToFiatAmount: Long): FilterOptionsDialog {
            val frag = FilterOptionsDialog()
            val args = Bundle()
            args.putInt(KEY_FILTER_TRANSACTION_DIRECTION, filterTransactionsDirection)
            args.putBoolean(KEY_FILTER_DATE_RANGE_ALL, filterDateRangeAll)
            args.putLong(KEY_FILTER_START_DATE, filterStartDate)
            args.putLong(KEY_FILTER_END_DATE, filterEndDate)
            args.putBoolean(KEY_FILTER_ASSET_ALL, filterAssetAll)
            args.putString(KEY_FILTER_ASSET, filterAsset)
            args.putBoolean(KEY_FILTER_FIAT_AMOUNT_ALL, filterFiatAmountAll)
            args.putLong(KEY_FILTER_FROM_FIAT_AMOUNT, filterFromFiatAmount)
            args.putLong(KEY_FILTER_TO_FIAT_AMOUNT, filterToFiatAmount)
            frag.arguments = args
            return frag
        }

    }

    // Widgets TODO use android-kotlin-extensions {onViewCreated}
    private lateinit var rbTransactionAll: RadioButton
    private lateinit var rbTransactionSent: RadioButton
    private lateinit var rbTransactionReceived: RadioButton
    private lateinit var cbDateRange: CheckBox
    private lateinit var llDateRange: LinearLayout
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var cbAsset: CheckBox
    private lateinit var sAsset: Spinner
    private lateinit var cbFiatAmount: CheckBox
    private lateinit var llFiatAmount: LinearLayout
//    lateinit var etFromFiatAmount: CurrencyEditText
//    lateinit var etToFiatAmount: CurrencyEditText

    private var mCallback: OnFilterOptionsSelectedListener? = null

    private lateinit var mDatePickerHandler: DatePickerHandler

    private var dateFormat: SimpleDateFormat = SimpleDateFormat("d/MMM/yyyy",
        Resources.getSystem().configuration.locale)

    private var startDate: Long = 0
    private var endDate: Long = 0

//    /**
//     * Variable used to keep track of the current user's currency
//     */
//    private val mUserCurrency = RuntimeData.EXTERNAL_CURRENCY

    private lateinit var mBalanceDetailViewModel: BalanceDetailViewModel

    private var mBalancesDetailsAdapter: BalancesDetailsAdapter? = null

    /**
     * DatePicker message handler.
     */
    inner class DatePickerHandler : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val bundle = msg.data
            val timestamp = bundle.get(KEY_TIMESTAMP) as Long
            //Log.d(TAG, "timestamp: $timestamp")
            when (msg.arg1) {
                START_DATE_PICKER -> {
                    startDate = timestamp

                    updateDateTextViews()
                }
                END_DATE_PICKER -> {
                    endDate = timestamp

                    // Make sure there is at least one moth difference between start and end time
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = endDate
                    calendar.add(Calendar.MONTH, -1)

                    val tmpTime = calendar.timeInMillis

                    if (tmpTime < startDate)
                        startDate = tmpTime

                    updateDateTextViews()
                }
            }
        }
    }

    private fun updateDateTextViews() {
        var date = Date(startDate)
        tvStartDate.text = dateFormat.format(date)

        date = Date(endDate)
        tvEndDate.text = dateFormat.format(date)
    }

    // Container Activity must implement this interface
    interface OnFilterOptionsSelectedListener {
        fun onFilterOptionsSelected(filterTransactionsDirection: Int,
                                    filterDateRangeAll: Boolean,
                                    filterStartDate: Long,
                                    filterEndDate: Long,
                                    filterAssetAll: Boolean,
                                    filterAsset: String,
                                    filterFiatAmountAll: Boolean,
                                    filterFromFiatAmount: Long,
                                    filterToFiatAmount: Long)
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        onAttachToParentFragment(parentFragment!!)

        // Initialize handler for communication with the DatePicker
        mDatePickerHandler = DatePickerHandler()

        val builder = AlertDialog.Builder(context!!)
            .setTitle(getString(R.string.title_filter_options))
            .setPositiveButton(getString(R.string.button__filter)) { _, _ ->  validateFields() }
            .setNegativeButton(getString(android.R.string.cancel)) { _, _ ->  dismiss() }

        // Inflate layout
        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(R.layout.dialog_filter_options, null)

        // Initialize Transactions direction
        rbTransactionAll = view.findViewById(R.id.rbTransactionAll)
        rbTransactionSent = view.findViewById(R.id.rbTransactionSent)
        rbTransactionReceived = view.findViewById(R.id.rbTransactionReceived)
        val radioButtonChecked = arguments!!.getInt(KEY_FILTER_TRANSACTION_DIRECTION, 0)
        when (radioButtonChecked) {
            0 -> rbTransactionAll.isChecked = true
            1 -> rbTransactionSent.isChecked = true
            2 -> rbTransactionReceived.isChecked = true
        }

        // Initialize Date range
        cbDateRange = view.findViewById(R.id.cbDateRange)
        llDateRange = view.findViewById(R.id.llDateRange)
        cbDateRange.setOnCheckedChangeListener { _, isChecked ->
            llDateRange.visibility = if(isChecked) View.GONE else View.VISIBLE }
        cbDateRange.isChecked = arguments!!.getBoolean(KEY_FILTER_DATE_RANGE_ALL, true)

        tvStartDate = view.findViewById(R.id.tvStartDate)
        tvEndDate = view.findViewById(R.id.tvEndDate)

        startDate = arguments!!.getLong(KEY_FILTER_START_DATE, 0)
        tvStartDate.setOnClickListener(mDateClickListener)

        endDate = arguments!!.getLong(KEY_FILTER_END_DATE, 0)
        tvEndDate.setOnClickListener(mDateClickListener)

        updateDateTextViews()

        // Initialize Asset
        cbAsset = view.findViewById(R.id.cbAsset)
        sAsset = view.findViewById(R.id.sAsset)
        cbAsset.setOnCheckedChangeListener { _, isChecked ->
            sAsset.visibility = if(isChecked) View.GONE else View.VISIBLE
        }
        cbAsset.isChecked = arguments!!.getBoolean(KEY_FILTER_ASSET_ALL, true)

        // Configure BalanceDetailViewModel to obtain the user's Balances
        mBalanceDetailViewModel = ViewModelProviders.of(this).get(BalanceDetailViewModel::class.java)

        mBalanceDetailViewModel.getAll().observe(this, Observer<List<BalanceDetail>> { balancesDetails ->
            mBalancesDetailsAdapter = BalancesDetailsAdapter(context!!, android.R.layout.simple_spinner_item, balancesDetails!!)
            sAsset.adapter = mBalancesDetailsAdapter

            val assetSelected = arguments!!.getString(KEY_FILTER_ASSET)

            // Try to select the selectedAssetSymbol
            for (i in 0 until mBalancesDetailsAdapter!!.count) {
                if (mBalancesDetailsAdapter!!.getItem(i)!!.symbol == assetSelected) {
                    sAsset.setSelection(i)
                    break
                }
            }
        })

        // Initialize Fiat amount
        cbFiatAmount = view.findViewById(R.id.cbFiatAmount)
//        llFiatAmount = view.findViewById(R.id.llFiatAmount)
//        cbFiatAmount.setOnCheckedChangeListener { _, isChecked ->
//            llFiatAmount.visibility = if(isChecked) View.GONE else View.VISIBLE }
        cbFiatAmount.isChecked = arguments!!.getBoolean(KEY_FILTER_FIAT_AMOUNT_ALL, true)

//        val locale = Resources.getSystem().configuration.locale
//
//        etFromFiatAmount = view.findViewById(R.id.etFromFiatAmount)
//        etFromFiatAmount.locale = locale
//        val fromFiatAmount = arguments!!.getLong(KEY_FILTER_FROM_FIAT_AMOUNT, 0)
//        etFromFiatAmount.setText("$fromFiatAmount", TextView.BufferType.EDITABLE)
//
//        etToFiatAmount = view.findViewById(R.id.etToFiatAmount)
//        etToFiatAmount.locale = locale
//        val toFiatAmount = arguments!!.getLong(KEY_FILTER_TO_FIAT_AMOUNT, 0)
//        etToFiatAmount.setText("$toFiatAmount", TextView.BufferType.EDITABLE)

        builder.setView(view)

        return builder.create()
    }

    /**
     * Attaches the current [DialogFragment] to its [Fragment] parent, to initialize the
     * [OnFilterOptionsSelectedListener] interface
     */
    private fun onAttachToParentFragment(fragment: Fragment) {
        try {
            mCallback = fragment as OnFilterOptionsSelectedListener
        } catch (e: ClassCastException) {
            throw ClassCastException(fragment.toString() + " must implement OnFilterOptionsSelectedListener")
        }
    }

    private val mDateClickListener = View.OnClickListener { v ->
        val calendar = Calendar.getInstance()

        // Variable used to select that date on the calendar
        var currentTime = calendar.timeInMillis
        var maxTime = currentTime

        var which = -1
        if (v.id == R.id.tvStartDate) {
            which = START_DATE_PICKER
            currentTime = startDate
            calendar.timeInMillis = endDate
            calendar.add(Calendar.MONTH, -1)
            maxTime = calendar.timeInMillis
        } else if (v.id == R.id.tvEndDate) {
            which = END_DATE_PICKER
            currentTime = endDate
        }

        val datePickerFragment = DatePickerFragment.newInstance(which, currentTime,
            maxTime, mDatePickerHandler)
        datePickerFragment.show(activity!!.supportFragmentManager, "date-picker")
    }

    private fun validateFields() {
        val filterTransactionsDirection =  when {
            rbTransactionAll.isChecked -> 0
            rbTransactionSent.isChecked -> 1
            rbTransactionReceived.isChecked -> 2
            else -> { 0 }
        }

        val filterDateRangeAll = cbDateRange.isChecked

        val filterAssetAll = cbAsset.isChecked

        val filterAsset = (sAsset.selectedItem as BalanceDetail).symbol

        val filterFiatAmountAll = cbFiatAmount.isChecked

        val filterFromFiatAmount = 0L//(etFromFiatAmount.currencyDouble *
//                Math.pow(10.0, mUserCurrency.defaultFractionDigits.toDouble())).toLong()

        var filterToFiatAmount = 1L//(etToFiatAmount.currencyDouble *
//                Math.pow(10.0, mUserCurrency.defaultFractionDigits.toDouble())).toLong()

        // Make sure ToFiatAmount is at least 50 units bigger than FromFiatAmount
//        if (!filterFiatAmountAll && filterToFiatAmount <= filterFromFiatAmount) {
//            filterToFiatAmount = filterFromFiatAmount + 50 *
//                    Math.pow(10.0, mUserCurrency.defaultFractionDigits.toDouble()).toLong()
//        }

        mCallback!!.onFilterOptionsSelected(filterTransactionsDirection, filterDateRangeAll,
            startDate, endDate, filterAssetAll, filterAsset, filterFiatAmountAll,
            filterFromFiatAmount, filterToFiatAmount)
    }
}