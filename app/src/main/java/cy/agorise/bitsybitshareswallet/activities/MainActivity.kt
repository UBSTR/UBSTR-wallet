package cy.agorise.bitsybitshareswallet.activities

import android.os.Bundle
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.fragments.BalancesFragment
import cy.agorise.bitsybitshareswallet.fragments.MerchantsFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_receive -> {
                Toast.makeText(this, "Receive Fragment", Toast.LENGTH_SHORT).show()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_balances -> {
                loadBalancesFragment()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_merchants -> {
                loadMerchantsFragment()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_send -> {
                Toast.makeText(this, "Send Fragment", Toast.LENGTH_SHORT).show()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        navigation.selectedItemId = R.id.navigation_balances
    }

    private fun loadBalancesFragment() {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_frame, BalancesFragment())
        ft.commit()
    }

    private fun loadMerchantsFragment() {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_frame, MerchantsFragment())
        ft.commit()
    }
}
