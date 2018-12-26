package cy.agorise.bitsybitshareswallet.fragments


import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.fragment.app.DialogFragment
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import cy.agorise.bitsybitshareswallet.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.ClassCastException


/**
 * Creates a Dialog that communicates with {@link TransactionsActivity} to give it parameters about
 * how to filter the list of Transactions
 */
class FilterOptionsDialog : DialogFragment() {

    // Widgets TODO use android-kotlin-extensions {onViewCreated}
    lateinit var rbTransactionAll: RadioButton
    lateinit var rbTransactionSent: RadioButton
    lateinit var rbTransactionReceived: RadioButton
    lateinit var cbDateRange: CheckBox
    lateinit var llDateRange: LinearLayout
    lateinit var tvStartDate: TextView
    lateinit var tvEndDate: TextView
    lateinit var cbCryptocurrency: CheckBox
    lateinit var sCryptocurrency: Spinner
    lateinit var cbFiatAmount: CheckBox
    lateinit var llFiatAmount: LinearLayout
//    lateinit var etFromFiatAmount: CurrencyEditText
//    lateinit var etToFiatAmount: CurrencyEditText

    private var mCallback: OnFilterOptionsSelectedListener? = null

    private var mDatePickerHandler: DatePickerHandler? = null

    private var dateFormat: SimpleDateFormat = SimpleDateFormat("d/MMM/yyyy",
        Resources.getSystem().configuration.locale)

    private var startDate: Long = 0
    private var endDate: Long = 0

//    /**
//     * Variable used to keep track of the current user's currency
//     */
//    private val mUserCurrency = RuntimeData.EXTERNAL_CURRENCY

    companion object {

        const val KEY_FILTER_TRANSACTION_DIRECTION = "key_filter_transaction_direction"
        const val KEY_FILTER_DATE_RANGE_ALL = "key_filter_date_range_all"
        const val KEY_FILTER_START_DATE = "key_filter_start_date"
        const val KEY_FILTER_END_DATE = "key_filter_end_date"
        const val KEY_FILTER_CRYPTOCURRENCY_ALL = "key_filter_cryptocurrency_all"
        const val KEY_FILTER_CRYPTOCURRENCY = "key_filter_cryptocurrency"
        const val KEY_FILTER_FIAT_AMOUNT_ALL = "key_filter_fiat_amount_all"
        const val KEY_FILTER_FROM_FIAT_AMOUNT = "filter_from_fiat_amount"
        const val KEY_FILTER_TO_FIAT_AMOUNT = "filter_to_fiat_amount"

        const val KEY_TIMESTAMP = "key_timestamp"

        const val START_DATE_PICKER = 0
        const val END_DATE_PICKER = 1

        fun newInstance(filterTransactionsDirection: Int, filterDateRangeAll: Boolean,
                        filterStartDate: Long, filterEndDate: Long, filterCryptocurrencyAll: Boolean,
                        filterCryptocurrency: String, filterFiatAmountAll: Boolean,
                        filterFromFiatAmount: Long, filterToFiatAmount: Long): FilterOptionsDialog {
            val frag = FilterOptionsDialog()
            val args = Bundle()
            args.putInt(KEY_FILTER_TRANSACTION_DIRECTION, filterTransactionsDirection)
            args.putBoolean(KEY_FILTER_DATE_RANGE_ALL, filterDateRangeAll)
            args.putLong(KEY_FILTER_START_DATE, filterStartDate)
            args.putLong(KEY_FILTER_END_DATE, filterEndDate)
            args.putBoolean(KEY_FILTER_CRYPTOCURRENCY_ALL, filterCryptocurrencyAll)
            args.putString(KEY_FILTER_CRYPTOCURRENCY, filterCryptocurrency)
            args.putBoolean(KEY_FILTER_FIAT_AMOUNT_ALL, filterFiatAmountAll)
            args.putLong(KEY_FILTER_FROM_FIAT_AMOUNT, filterFromFiatAmount)
            args.putLong(KEY_FILTER_TO_FIAT_AMOUNT, filterToFiatAmount)
            frag.arguments = args
            return frag
        }

    }

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
                                    filterCryptocurrencyAll: Boolean,
                                    filterCryptocurrency: String,
                                    filterFiatAmountAll: Boolean,
                                    filterFromFiatAmount: Long,
                                    filterToFiatAmount: Long)
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        onAttachToParentFragment(parentFragment!!)

        // Initialize handler for communication with the DatePicker
        mDatePickerHandler = DatePickerHandler()

        val builder = AlertDialog.Builder(context!!)
            .setTitle("Filter options")
            .setPositiveButton("Filter") { _, _ ->  validateFields() }
            .setNegativeButton("Cancel") { _, _ ->  dismiss() }

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
//        llDateRange = view.findViewById(R.id.llDateRange)
//        cbDateRange.setOnCheckedChangeListener { _, isChecked ->
//            llDateRange.visibility = if(isChecked) View.GONE else View.VISIBLE }
        cbDateRange.isChecked = arguments!!.getBoolean(KEY_FILTER_DATE_RANGE_ALL, true)
//
//        tvStartDate = view.findViewById(R.id.tvStartDate)
//        tvEndDate = view.findViewById(R.id.tvEndDate)
//
//        startDate = arguments!!.getLong(KEY_FILTER_START_DATE, 0)
//        tvStartDate.setOnClickListener(mDateClickListener)
//
//        endDate = arguments!!.getLong(KEY_FILTER_END_DATE, 0)
//        tvEndDate.setOnClickListener(mDateClickListener)
//
//        updateDateTextViews()

        // Initialize Cryptocurrency
        cbCryptocurrency = view.findViewById(R.id.cbCryptocurrency)
//        sCryptocurrency = view.findViewById(R.id.sCryptocurrency)
//        cbCryptocurrency.setOnCheckedChangeListener { _, isChecked ->
//            sCryptocurrency.visibility = if(isChecked) View.GONE else View.VISIBLE }
        cbCryptocurrency.isChecked = arguments!!.getBoolean(KEY_FILTER_CRYPTOCURRENCY_ALL, true)

//        sCryptocurrency = view.findViewById(R.id.sCryptocurrency)
//        initializeCryptocurrencySpinner()


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

//    private fun initializeCryptocurrencySpinner() {
//        val cryptoCurrencyList = database!!.getSortedCryptoCurrencies(false,
//            SortType.DESCENDING, true)
//
//        val cryptocurrencySpinnerAdapter = CryptocurrencySpinnerAdapter(context!!,
//            R.layout.item_cryptocurrency,
//            R.id.tvCryptocurrencyName,
//            cryptoCurrencyList)
//
//        sCryptocurrency.adapter = cryptocurrencySpinnerAdapter
//
//        val cryptocurrencySelected = arguments!!.getString(KEY_FILTER_CRYPTOCURRENCY)
//
//        val index = Math.max(cryptocurrencySpinnerAdapter.getPosition(database!!.getCryptocurrencyBySymbol(
//            cryptocurrencySelected)), 0)
//
//        sCryptocurrency.setSelection(index)
//    }

//    private val mDateClickListener = View.OnClickListener { v ->
//        val calendar = Calendar.getInstance()
//
//        // Variable used to select that date on the calendar
//        var currentTime = calendar.timeInMillis
//        var maxTime = currentTime
//
//        var which = -1
//        if (v.id == R.id.tvStartDate) {
//            which = START_DATE_PICKER
//            currentTime = startDate
//            calendar.timeInMillis = endDate
//            calendar.add(Calendar.MONTH, -1)
//            maxTime = calendar.timeInMillis
//        } else if (v.id == R.id.tvEndDate) {
//            which = END_DATE_PICKER
//            currentTime = endDate
//        }
//
//        val datePickerFragment = DatePickerFragment.newInstance(which, currentTime,
//            maxTime, mDatePickerHandler)
//        datePickerFragment.show(activity!!.supportFragmentManager, "date-picker")
//    }

    private fun validateFields() {
        val filterTransactionsDirection =  0 //when {
//            rbTransactionAll.isChecked -> 0
//            rbTransactionSent.isChecked -> 1
//            rbTransactionReceived.isChecked -> 2
//            else -> { 0 }
//        }

        val filterDateRangeAll = cbDateRange.isChecked

        val filterCryptocurrencyAll = cbCryptocurrency.isChecked

        val filterCryptocurrency = "" //(sCryptocurrency.selectedItem as CryptoCurrency).symbol

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
            startDate, endDate, filterCryptocurrencyAll, filterCryptocurrency, filterFiatAmountAll,
            filterFromFiatAmount, filterToFiatAmount)
    }
}