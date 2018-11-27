package cy.agorise.bitsybitshareswallet.fragments

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer

import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.entities.UserAccount
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.viewmodels.BalancesViewModel
import cy.agorise.bitsybitshareswallet.viewmodels.UserAccountViewModel
import kotlinx.android.synthetic.main.fragment_balances.*

class BalancesFragment : Fragment() {

    private lateinit var mUserAccountViewModel: UserAccountViewModel
    private lateinit var mBalancesViewModel: BalancesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_balances, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mUserAccountViewModel = ViewModelProviders.of(this).get(UserAccountViewModel::class.java)

        val userId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "")

        mUserAccountViewModel.getUserAccount(userId!!).observe(this, Observer<UserAccount>{ user ->
            tvAccountName.text = user.name
        })


        mBalancesViewModel = ViewModelProviders.of(this).get(BalancesViewModel::class.java)
        // TODO: Use the ViewModel

    }
}
