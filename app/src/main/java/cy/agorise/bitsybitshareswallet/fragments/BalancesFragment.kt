package cy.agorise.bitsybitshareswallet.fragments

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager

import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.adapters.BalancesAdapter
import cy.agorise.bitsybitshareswallet.adapters.TransfersAdapter
import cy.agorise.bitsybitshareswallet.database.entities.UserAccount
import cy.agorise.bitsybitshareswallet.database.joins.BalanceDetail
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetail
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.viewmodels.BalanceDetailViewModel
import cy.agorise.bitsybitshareswallet.viewmodels.TransferDetailViewModel
import cy.agorise.bitsybitshareswallet.viewmodels.UserAccountViewModel
import kotlinx.android.synthetic.main.fragment_balances.*

class BalancesFragment : Fragment() {

    private lateinit var mUserAccountViewModel: UserAccountViewModel
    private lateinit var mBalanceDetailViewModel: BalanceDetailViewModel
    private lateinit var mTransferDetailViewModel: TransferDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_balances, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configure UserAccountViewModel to show the current account
        mUserAccountViewModel = ViewModelProviders.of(this).get(UserAccountViewModel::class.java)

        val userId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "")

        mUserAccountViewModel.getUserAccount(userId!!).observe(this, Observer<UserAccount>{ user ->
            tvAccountName.text = user.name
        })

        // Configure BalanceDetailViewModel to show the current balances
        mBalanceDetailViewModel = ViewModelProviders.of(this).get(BalanceDetailViewModel::class.java)

        val balancesAdapter = BalancesAdapter(context!!)
        rvBalances.adapter = balancesAdapter
        rvBalances.layoutManager = GridLayoutManager(context, 2)

        mBalanceDetailViewModel.getAll().observe(this, Observer<List<BalanceDetail>> { balancesDetails ->
            balancesAdapter.replaceAll(balancesDetails)
        })

        // Configure TransferDetailViewModel to show the transaction history
        mTransferDetailViewModel = ViewModelProviders.of(this).get(TransferDetailViewModel::class.java)

        val transfersAdapter = TransfersAdapter(context!!)
        rvTransactions.adapter = transfersAdapter
        rvTransactions.layoutManager = LinearLayoutManager(context)

        mTransferDetailViewModel.getAll().observe(this, Observer<List<TransferDetail>> { transfersDetails ->
            transfersAdapter.replaceAll(transfersDetails)
        })
    }
}
