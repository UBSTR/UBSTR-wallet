package cy.agorise.bitsybitshareswallet.activities

import android.os.Bundle
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.graphenej.BrainKey
import cy.agorise.graphenej.UserAccount
import kotlinx.android.synthetic.main.activity_import_brainkey.*

class ImportBrainkeyActivity : ConnectedActivity() {

    /**
     * Private variable that will hold an instance of the [BrainKey] class
     */
    private var mBrainKey: BrainKey? = null

    /**
     * User account associated with the key derived from the brainkey that the user just typed in
     */
    private var mUserAccount: UserAccount? = null

    /**
     * List of user account candidates, this is required in order to allow the user to select a single
     * user account in case one key (derived from the brainkey) controls more than one account.
     */
    private var mUserAccountCandidates: List<UserAccount>? = null

    private var mKeyReferencesAttempts = 0

    private var keyReferencesRequestId: Long = 0
    private var getAccountsRequestId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_brainkey)

        btnImport.setOnClickListener { importAccount() }
    }

    private fun importAccount() {
        val trimmedBrainKey = tietBrainKey.text!!.toString().trim { it <= ' ' }
        tietBrainKey.setText(trimmedBrainKey)

        tilPin.isErrorEnabled = false
        tilPinConfirmation.isErrorEnabled = false
        tilBrainKey.isErrorEnabled = false

        if (tietPin.text!!.length < Constants.MIN_PIN_LENGTH)
            tilPin.error = getString(R.string.error__pin_too_short)
        else if (tietPin.text.toString() != tietPinConfirmation.text.toString())
            tilPinConfirmation.error = getString(R.string.error__pin_mismatch)
        else if (tietBrainKey.text!!.isEmpty() || !tietBrainKey.text.toString().contains(" "))
            tilBrainKey.error = getString(R.string.error__enter_correct_brainkey)
        else {
            val brainKey = tietBrainKey.text.toString().split(" ")
            if (brainKey.size in 12..16) {
                // TODO verify brainkey
            } else
                tilBrainKey.error = getString(R.string.error__enter_correct_brainkey)
        }

    }
}