package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.adapters.TransfersDetailsAdapter
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetail
import cy.agorise.bitsybitshareswallet.utils.BounceTouchListener
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.viewmodels.TransferDetailViewModel
import kotlinx.android.synthetic.main.fragment_transactions.*

class TransactionsFragment : Fragment() {

    private lateinit var mTransferDetailViewModel: TransferDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "") ?: ""

        // Configure TransferDetailViewModel to show the transaction history
        mTransferDetailViewModel = ViewModelProviders.of(this).get(TransferDetailViewModel::class.java)

        val transfersDetailsAdapter = TransfersDetailsAdapter(context!!)
        rvTransactions.adapter = transfersDetailsAdapter
        rvTransactions.layoutManager = LinearLayoutManager(context)

        mTransferDetailViewModel.getAll(userId).observe(this, Observer<List<TransferDetail>> { transfersDetails ->
            transfersDetailsAdapter.replaceAll(transfersDetails)
        })

        // Sets custom touch listener to handle bounce/stretch effect
        val bounceTouchListener = BounceTouchListener(rvTransactions)
        rvTransactions.setOnTouchListener(bounceTouchListener)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_transactions, menu)
    }
}
