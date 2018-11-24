package cy.agorise.bitsybitshareswallet.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflates the menu and places it in the toolbar
        menuInflater.inflate(R.menu.menu_main, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return if (item!!.itemId == R.id.menu_settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}
