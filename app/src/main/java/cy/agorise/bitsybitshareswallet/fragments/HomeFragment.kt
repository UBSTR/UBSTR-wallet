package cy.agorise.bitsybitshareswallet.fragments

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController

import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.database.entities.UserAccount
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.viewmodels.UserAccountViewModel
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.item_balance.view.*

class HomeFragment : Fragment() {

    private lateinit var mUserAccountViewModel: UserAccountViewModel
//    private lateinit var mBalanceDetailViewModel: BalanceDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.fragment_home, container, false)
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

//        // Configure BalanceDetailViewModel to show the current balances
//        mBalanceDetailViewModel = ViewModelProviders.of(this).get(BalanceDetailViewModel::class.java)
//
//        val balancesAdapter = BalancesAdapter(context!!)
//        rvBalances.adapter = balancesAdapter
//        rvBalances.layoutManager = LinearLayoutManager(context!!)
//        rvBalances.addItemDecoration(DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL))
//
//        mBalanceDetailViewModel.getAll().observe(this, Observer<List<BalanceDetail>> { balancesDetails ->
//            balancesAdapter.replaceAll(balancesDetails)
//        })

        // Navigate to the Receive Transaction Fragment
        fabReceiveTransaction.setOnClickListener (
            Navigation.createNavigateOnClickListener(R.id.receive_action)
        )

        // Navigate to the Send Transaction Fragment without activating the camera
        fabSendTransaction.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.send_action)
        )

        // Navigate to the Send Transaction Fragment using Navigation's SafeArgs to activate the camera
        fabSendTransactionCamera.setOnClickListener {
            val action = HomeFragmentDirections.sendActionCamera()
            action.setOpenCamera(true)
            findNavController().navigate(action)
        }

        val pagerAdapter = PagerAdapter(fragmentManager!!)
        viewPager.adapter = pagerAdapter
        tabLayout.setupWithViewPager(viewPager)
    }

    /**
     * Pager adapter to create the placeholder fragments
     */
    private inner class PagerAdapter internal constructor(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return listOf(getString(R.string.title_balances), "Net Worth")[position]
        }

        override fun getCount(): Int {
            return 2
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    class PlaceholderFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val rootView = inflater.inflate(R.layout.item_balance, container, false)
            val text = "Hello World from section ${arguments?.getInt(ARG_SECTION_NUMBER)}"
            rootView.tvBalance.text = text
            return rootView
        }

        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            private const val ARG_SECTION_NUMBER = "section_number"

            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */
            fun newInstance(sectionNumber: Int): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                fragment.arguments = args
                return fragment
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_balances, menu)
    }
}
