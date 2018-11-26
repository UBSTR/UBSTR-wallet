package cy.agorise.bitsybitshareswallet.activities

import android.content.Intent
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
        if (agreedLicenseVersion == Constants.CURRENT_LICENSE_VERSION) {
            agree()
        } else {
            wbLA.loadData(getString(R.string.licence_html), "text/html", "UTF-8")

            btnDisagree.setOnClickListener { finish() }

            btnAgree.setOnClickListener { agree() }
        }
    }

    /**
     * This function stores the version of the current accepted license version into the Shared Preferences and
     * sends the user to load the assets database if they have not been loaded, import/create account if there is no
     * active account or to the MainActivity otherwise.
     */
    private fun agree() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putInt(Constants.KEY_LAST_AGREED_LICENSE_VERSION, Constants.CURRENT_LICENSE_VERSION).apply()

        val intent : Intent?

        val isDatabaseLoaded = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(Constants.KEY_DATABASE_LOADED, false)

        val initialSetupDone = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(Constants.KEY_INITIAL_SETUP_DONE, false)

        intent = if (!isDatabaseLoaded)
            Intent(this, DatabaseLoadActivity::class.java)
        else if (!initialSetupDone)
            Intent(this, ImportBrainkeyActivity::class.java)
        else
            Intent(this, MainActivity::class.java)


        startActivity(intent)
        finish()
    }
}