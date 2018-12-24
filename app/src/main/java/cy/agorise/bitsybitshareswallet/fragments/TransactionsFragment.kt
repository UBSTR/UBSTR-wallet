package cy.agorise.bitsybitshareswallet.fragments

import android.graphics.Point
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.appcompat.queryTextChanges
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.adapters.TransfersDetailsAdapter
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetail
import cy.agorise.bitsybitshareswallet.utils.BounceTouchListener
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.viewmodels.TransferDetailViewModel
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_transactions.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class TransactionsFragment : Fragment(), SearchView.OnQueryTextListener {

    private lateinit var mTransferDetailViewModel: TransferDetailViewModel

    private lateinit var transfersDetailsAdapter: TransfersDetailsAdapter

    private val transfersDetails = ArrayList<TransferDetail>()
    private val filteredTransfersDetails = ArrayList<TransferDetail>()

    /** Variables used to filter the transaction items  */
    private var filterQuery = ""
    private var filterTransactionsDirection = 0
    private var filterDateRangeAll = true
    private var filterStartDate = 0L
    private var filterEndDate = 0L
    private var filterCryptocurrencyAll = true
    private var filterCryptocurrency = "BTS"
    private var filterFiatAmountAll = true
    private var filterFromFiatAmount = 0L
    private var filterToFiatAmount = 500L

    private var mDisposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "") ?: ""

        transfersDetailsAdapter = TransfersDetailsAdapter(context!!)
        rvTransactions.adapter = transfersDetailsAdapter
        rvTransactions.layoutManager = LinearLayoutManager(context)

        // Configure TransferDetailViewModel to fetch the transaction history
        mTransferDetailViewModel = ViewModelProviders.of(this).get(TransferDetailViewModel::class.java)

        mTransferDetailViewModel.getAll(userId).observe(this, Observer<List<TransferDetail>> { transfersDetails ->
            this.transfersDetails.clear()
            this.transfersDetails.addAll(transfersDetails)
            applyFilterOptions(false)
        })

        // Set custom touch listener to handle bounce/stretch effect
        val bounceTouchListener = BounceTouchListener(rvTransactions)
        rvTransactions.setOnTouchListener(bounceTouchListener)

        // Initialize filter options
        val calendar = Calendar.getInstance()
        filterEndDate = calendar.timeInMillis / 1000
        calendar.add(Calendar.MONTH, -2)
        filterStartDate = calendar.timeInMillis / 1000
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_transactions, menu)

        // Adds listener for the SearchView
        val searchItem = menu.findItem(R.id.menuSearch)
        val searchView = searchItem.actionView as SearchView
        mDisposables.add(
            searchView.queryTextChanges()
            .skipInitialValue()
            .debounce(500, TimeUnit.MILLISECONDS)
            .map { it.toString().toLowerCase() }
            .subscribe {
                filterQuery = it
                applyFilterOptions()
            }
        )

        // Adjust SearchView width to avoid pushing other menu items out of the screen
        searchView.maxWidth = getScreenWidth(activity) * 3 / 5
    }

    /**
     * Returns the screen width in pixels for the given [FragmentActivity]
     */
    private fun getScreenWidth(activity: FragmentActivity?): Int {
        if (activity == null)
            return 200

        val size = Point()
        activity.windowManager.defaultDisplay.getSize(size)
        return size.x
    }

    override fun onQueryTextChange(query: String): Boolean {
        filterQuery = query.toLowerCase()
        applyFilterOptions()
        return true
    }

    override fun onQueryTextSubmit(query: String?) = false // Not needed since we are filtering as the user types

    /**
     * Filters the TransferDetail list given the user selected filter options.
     * TODO move this to a background thread
     */
    private fun applyFilterOptions(scrollToTop: Boolean = true) {
        // Clean the filtered list
        filteredTransfersDetails.clear()

        for (transferDetail in transfersDetails) {
            // Filter by transfer direction
            if (transferDetail.direction) { // Transfer sent
                if (filterTransactionsDirection == 2)
                // Looking for received transfers only
                    continue
            } else { // Transfer received
                if (filterTransactionsDirection == 1)
                // Looking for sent transactions only
                    continue
            }

            // Filter by date range
            if (!filterDateRangeAll && (transferDetail.date < filterStartDate || transferDetail.date > filterEndDate))
                continue

            // Filter by cryptocurrency
            if (!filterCryptocurrencyAll && transferDetail.cryptoSymbol != filterCryptocurrency)
                continue

//            // Filter by fiat amount
//            if (!filterFiatAmountAll && (transferDetail.fiatAmount < filterFromFiatAmount || transferDetail.fiatAmount > filterToFiatAmount))
//                continue

            // Filter by search query
            val text = (transferDetail.from ?: "").toLowerCase() + (transferDetail.to ?: "").toLowerCase()
            if (text.contains(filterQuery, ignoreCase = true)) {
                filteredTransfersDetails.add(transferDetail)
            }
        }

        // Replaces the list of TransferDetail items with the new filtered list
        transfersDetailsAdapter.replaceAll(filteredTransfersDetails)

        if (scrollToTop)
            rvTransactions.scrollToPosition(0)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!mDisposables.isDisposed) mDisposables.dispose()
    }
}
