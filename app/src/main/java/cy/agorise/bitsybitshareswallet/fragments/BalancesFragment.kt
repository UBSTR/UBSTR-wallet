package cy.agorise.bitsybitshareswallet.fragments

import androidx.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.activities.ReceiveTransactionActivity
import cy.agorise.bitsybitshareswallet.activities.SendTransactionActivity
import cy.agorise.bitsybitshareswallet.viewmodels.BalancesViewModel
import kotlinx.android.synthetic.main.fragment_balances.*

class BalancesFragment : Fragment() {

    companion object {
        fun newInstance() = BalancesFragment()
    }

    private lateinit var viewModel: BalancesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_balances, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(BalancesViewModel::class.java)
        // TODO: Use the ViewModel
    }
}
