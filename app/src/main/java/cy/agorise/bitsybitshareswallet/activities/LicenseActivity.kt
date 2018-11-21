package cy.agorise.bitsybitshareswallet.activities

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.utils.Constants
import kotlinx.android.synthetic.main.activity_license.*

class LicenseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)

        // Get version number of the last agreed license version
        val agreedLicenseVersion = PreferenceManager.getDefaultSharedPreferences(this)
            .getInt(Constants.KEY_LAST_AGREED_LICENSE_VERSION, 0)

        // If the last agreed license version is the actual one then proceed to the following Activities
        if (agreedLicenseVersion == 1) {
            agree()
        } else {
            wbLA.loadData(getString(R.string.licence_html), "text/html", "UTF-8")

            btnDisagree.setOnClickListener { finish() }

            btnAgree.setOnClickListener { agree() }
        }
    }

    private fun agree() {

    }
}