package cy.agorise.bitsybitshareswallet.fragments

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import androidx.appcompat.widget.Toolbar
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

class HomeFragment : Fragment() {

    private lateinit var mUserAccountViewModel: UserAccountViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        val toolbar: Toolbar? = activity?.findViewById(R.id.toolbar)
        toolbar?.navigationIcon = resources.getDrawable(R.drawable.ic_bitsy_logo_2, null)

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

        // Configure ViewPager with PagerAdapter and TabLayout to display the Balances/NetWorth section
        val pagerAdapter = PagerAdapter(fragmentManager!!)
        viewPager.adapter = pagerAdapter
        tabLayout.setupWithViewPager(viewPager)
        // Set the pie chart icon for the third tab
        tabLayout.getTabAt(2)?.setIcon(R.drawable.ic_pie_chart)
    }

    /**
     * Pager adapter to create the placeholder fragments
     */
    private inner class PagerAdapter internal constructor(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            return if (position == 0)
                BalancesFragment()
            else
                NetWorthFragment()
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return listOf(getString(R.string.title_balances), getString(R.string.title_net_worth), "")[position]
        }

        override fun getCount(): Int {
            return 3
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
    }
}
