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
import androidx.core.os.ConfigurationCompat
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
import kotlin.collections.ArrayList


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
        const val KEY_FILTER_EQUIVALENT_VALUE_ALL = "key_filter_equivalent_value_all"
        const val KEY_FILTER_FROM_EQUIVALENT_VALUE = "key_filter_from_equivalent_value"
        const val KEY_FILTER_TO_EQUIVALENT_VALUE = "key_filter_to_equivalent_value"
        const val KEY_FILTER_AGORISE_FEES = "key_filter_agorise_fees"

        const val KEY_TIMESTAMP = "key_timestamp"

        const val START_DATE_PICKER = 0
        const val END_DATE_PICKER = 1

        fun newInstance(filterTransactionsDirection: Int, filterDateRangeAll: Boolean,
                        filterStartDate: Long, filterEndDate: Long, filterAssetAll: Boolean,
                        filterAsset: String, filterEquivalentValueAll: Boolean, filterFromEquivalentValue: Long,
                        filterToEquivalentValue: Long, filterAgoriseFees: Boolean): FilterOptionsDialog {
            val frag = FilterOptionsDialog()
            val args = Bundle()
            args.putInt(KEY_FILTER_TRANSACTION_DIRECTION, filterTransactionsDirection)
            args.putBoolean(KEY_FILTER_DATE_RANGE_ALL, filterDateRangeAll)
            args.putLong(KEY_FILTER_START_DATE, filterStartDate)
            args.putLong(KEY_FILTER_END_DATE, filterEndDate)
            args.putBoolean(KEY_FILTER_ASSET_ALL, filterAssetAll)
            args.putString(KEY_FILTER_ASSET, filterAsset)
            args.putBoolean(KEY_FILTER_EQUIVALENT_VALUE_ALL, filterEquivalentValueAll)
            args.putLong(KEY_FILTER_FROM_EQUIVALENT_VALUE, filterFromEquivalentValue)
            args.putLong(KEY_FILTER_TO_EQUIVALENT_VALUE, filterToEquivalentValue)
            args.putBoolean(KEY_FILTER_AGORISE_FEES, filterAgoriseFees)
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
    private lateinit var cbEquivalentValue: CheckBox
    private lateinit var llEquivalentValue: LinearLayout
    private lateinit var etFromEquivalentValue: EditText
    private lateinit var etToEquivalentValue: EditText
    private lateinit var tvEquivalentValueSymbol: TextView
    private lateinit var switchAgoriseFees: Switch

    private var mCallback: OnFilterOptionsSelectedListener? = null

    private lateinit var mDatePickerHandler: DatePickerHandler

    private var dateFormat: SimpleDateFormat = SimpleDateFormat("d/MMM/yyyy",
        ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0])

    private var startDate: Long = 0
    private var endDate: Long = 0

    private var mBalanceDetails = ArrayList<BalanceDetail>()

    private lateinit var mBalanceDetailViewModel: BalanceDetailViewModel

    private var mBalancesDetailsAdapter: BalancesDetailsAdapter? = null

    private lateinit var mCurrency: Currency

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

    // Container Fragment must implement this interface
    interface OnFilterOptionsSelectedListener {
        fun onFilterOptionsSelected(filterTransactionsDirection: Int,
                                    filterDateRangeAll: Boolean,
                                    filterStartDate: Long,
                                    filterEndDate: Long,
                                    filterAssetAll: Boolean,
                                    filterAsset: String,
                                    filterEquivalentValueAll: Boolean,
                                    filterFromEquivalentValue: Long,
                                    filterToEquivalentValue: Long,
                                    filterAgoriseFees: Boolean)
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
            mBalanceDetails.clear()
            mBalanceDetails.addAll(balancesDetails)
            mBalanceDetails.sortWith(
                Comparator { a, b -> a.toString().compareTo(b.toString(), true) }
            )
            mBalancesDetailsAdapter = BalancesDetailsAdapter(context!!, android.R.layout.simple_spinner_item, mBalanceDetails)
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

        // Initialize Equivalent Value
        cbEquivalentValue = view.findViewById(R.id.cbEquivalentValue)
        llEquivalentValue = view.findViewById(R.id.llEquivalentValue)
        cbEquivalentValue.setOnCheckedChangeListener { _, isChecked ->
            llEquivalentValue.visibility = if(isChecked) View.GONE else View.VISIBLE }
        cbEquivalentValue.isChecked = arguments!!.getBoolean(KEY_FILTER_EQUIVALENT_VALUE_ALL, true)

        // TODO obtain user selected currency
        val currencySymbol = "usd"
        mCurrency = Currency.getInstance(currencySymbol)

        etFromEquivalentValue = view.findViewById(R.id.etFromEquivalentValue)
        val fromEquivalentValue = arguments!!.getLong(KEY_FILTER_FROM_EQUIVALENT_VALUE, 0) /
                Math.pow(10.0, mCurrency.defaultFractionDigits.toDouble()).toLong()
        etFromEquivalentValue.setText("$fromEquivalentValue", TextView.BufferType.EDITABLE)

        etToEquivalentValue = view.findViewById(R.id.etToEquivalentValue)
        val toEquivalentValue = arguments!!.getLong(KEY_FILTER_TO_EQUIVALENT_VALUE, 0) /
                Math.pow(10.0, mCurrency.defaultFractionDigits.toDouble()).toLong()
        etToEquivalentValue.setText("$toEquivalentValue", TextView.BufferType.EDITABLE)

        tvEquivalentValueSymbol = view.findViewById(R.id.tvEquivalentValueSymbol)
        tvEquivalentValueSymbol.text = currencySymbol.toUpperCase()

        // Initialize transaction network fees
        switchAgoriseFees = view.findViewById(R.id.switchAgoriseFees)
        switchAgoriseFees.isChecked = arguments!!.getBoolean(KEY_FILTER_AGORISE_FEES, true)

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
            throw ClassCastException("$fragment must implement OnFilterOptionsSelectedListener")
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

        val filterEquivalentValueAll = cbEquivalentValue.isChecked

        val filterFromEquivalentValue = etFromEquivalentValue.text.toString().toLong() *
                Math.pow(10.0, mCurrency.defaultFractionDigits.toDouble()).toLong()

        var filterToEquivalentValue = etToEquivalentValue.text.toString().toLong() *
                Math.pow(10.0, mCurrency.defaultFractionDigits.toDouble()).toLong()

        // Make sure ToEquivalentValue is at least 50 units bigger than FromEquivalentValue
        if (!filterEquivalentValueAll && filterToEquivalentValue < filterFromEquivalentValue + 50) {
            filterToEquivalentValue = filterFromEquivalentValue + 50
        }

        val filterAgoriseFees = switchAgoriseFees.isChecked

        mCallback!!.onFilterOptionsSelected(filterTransactionsDirection, filterDateRangeAll,
            startDate, endDate, filterAssetAll, filterAsset, filterEquivalentValueAll,
            filterFromEquivalentValue, filterToEquivalentValue, filterAgoriseFees)
    }
}