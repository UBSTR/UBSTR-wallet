package cy.agorise.bitsybitshareswallet.activities

import android.content.DialogInterface
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.graphenej.Address
import cy.agorise.graphenej.BrainKey
import cy.agorise.graphenej.UserAccount
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.calls.GetAccounts
import cy.agorise.graphenej.api.calls.GetKeyReferences
import cy.agorise.graphenej.models.AccountProperties
import cy.agorise.graphenej.models.JsonRpcResponse
import kotlinx.android.synthetic.main.activity_import_brainkey.*
import org.bitcoinj.core.ECKey
import java.util.ArrayList

class ImportBrainkeyActivity : ConnectedActivity() {
    private val TAG = "ImportBrainkeyActivity"

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

        // Custom event to activate import account from the keyboard
        tietBrainKey.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                importAccount()
                true
            } else
            false
        }

        btnImport.setOnClickListener { importAccount() }
    }

    private fun importAccount() {
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
            val brainKey = tietBrainKey.text.toString()
            if (brainKey.split(" ").size in 12..16)
                verifyBrainKey(false)
            else
                tilBrainKey.error = getString(R.string.error__enter_correct_brainkey)
        }

    }

    /**
     * Method that will verify the provided brain key, and if valid will retrieve the account information
     * associated to the user id.
     *
     * This method will perform a network lookup to look which accounts use the public key associated
     * with the user provided brainkey.
     *
     * Some sources use brainkeys in capital letters, while others use lowercase. The activity should
     * initially call this method with the 'switchCase' parameter as false, in order to try the
     * brainkey as it was provided by the user.
     *
     * But in case this lookup fails, it is expected that the activity makes another attempt. This time
     * with the 'switchCase' argument set to true.
     *
     * If both attempts fail, then we can be certain that the provided brainkey is not currently
     * associated with any account.
     *
     * @param switchCase Whether to switch the case used in the brainkey or not.
     */
    private fun verifyBrainKey(switchCase: Boolean) {
        //showDialog("", getString(R.string.importing_your_wallet))
        val brainKey = tietBrainKey.text.toString()
        // Should we switch the brainkey case?
        if (switchCase) {
            if (Character.isUpperCase(brainKey.toCharArray()[brainKey.length - 1])) {
                // If the last character is an uppercase, we assume the whole brainkey
                // was given in capital letters and turn it to lowercase
                getAccountFromBrainkey(brainKey.toLowerCase())
            } else {
                // Otherwise we turn the whole brainkey to capital letters
                getAccountFromBrainkey(brainKey.toUpperCase())
            }
        } else {
            // If no case switching should take place, we perform the network call with
            // the brainkey as it was provided to us.
            getAccountFromBrainkey(brainKey)
        }
    }

    /**
     * Method that will send a network request asking for all the accounts that make use of the
     * key derived from a give brain key.
     *
     * @param brainKey The brain key the user has just typed
     */
    private fun getAccountFromBrainkey(brainKey: String) {
        mBrainKey = BrainKey(brainKey, 0)
        val address = Address(ECKey.fromPublicOnly(mBrainKey!!.privateKey.pubKey))
        Log.d(TAG, String.format("Brainkey would generate address: %s", address.toString()))
        keyReferencesRequestId = mNetworkService!!.sendMessage(GetKeyReferences(address), GetKeyReferences.REQUIRED_API)
    }

    override fun handleJsonRpcResponse(response: JsonRpcResponse<*>) {
        Log.d(TAG, "handleResponse.Thread: " + Thread.currentThread().name)
        if (response.id == keyReferencesRequestId) {
            val resp = response.result as List<List<UserAccount>>
            val accountList: List<UserAccount> = resp[0].distinct()
            if (accountList.isEmpty() && mKeyReferencesAttempts == 0) {
                mKeyReferencesAttempts++
                verifyBrainKey(true)
            } else {
                if (accountList.isEmpty()) {
                    //hideDialog()
                    Toast.makeText(applicationContext, R.string.error__invalid_brainkey, Toast.LENGTH_SHORT).show()
                } else {
                    if (accountList.size == 1) {
                        // If we only found one account linked to this key, then we just proceed
                        // trying to find out the account name
                        mUserAccount = accountList[0]
                        getAccountsRequestId =
                                mNetworkService!!.sendMessage(GetAccounts(mUserAccount), GetAccounts.REQUIRED_API)
                    } else {
                        // If we found more than one account linked to this key, we must also
                        // find out the account names, but the procedure is a bit different in
                        // that after having those, we must still ask the user to decide which
                        // account should be imported.
                        mUserAccountCandidates = accountList
                        getAccountsRequestId = mNetworkService!!.sendMessage(
                            GetAccounts(mUserAccountCandidates),
                            GetAccounts.REQUIRED_API
                        )
                    }
                }
            }
        } else if (response.id == getAccountsRequestId) {
            val accountPropertiesList = response.result as List<AccountProperties>
            if (accountPropertiesList.size > 1) {
                val candidates = ArrayList<String>()
                for (accountProperties in accountPropertiesList) {
                    candidates.add(accountProperties.name)
                }
//                hideDialog()
                MaterialDialog(this)
                    .title(R.string.dialog__account_candidates_title)
                    .message(R.string.dialog__account_candidates_content)
                    .listItems(items = candidates) { dialog, index, _ ->
                        if (index >= 0) {
                            // If one account was selected, we keep a reference to it and
                            // store the account properties
                            // TODO make sure this is reached
                            mUserAccount = mUserAccountCandidates!![index]
                            onAccountSelected(accountPropertiesList[index])
                            dialog.dismiss()
                        }
                    }
                    .negativeButton(android.R.string.cancel) { mKeyReferencesAttempts = 0 }
                    .show()
            } else if (accountPropertiesList.size == 1) {
                onAccountSelected(accountPropertiesList[0])
            } else {
                Toast.makeText(applicationContext, R.string.error__try_again, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun handleConnectionStatusUpdate(connectionStatusUpdate: ConnectionStatusUpdate) {
        Log.d(TAG, "handleConnectionStatusUpdate. code: " + connectionStatusUpdate.updateCode)
    }

    /**
     * Method called internally once an account has been detected. This method will store internally
     * the following details:
     *
     * - Account name in the database
     * - Account authorities in the database
     * - The current account id in the shared preferences
     *
     * @param accountProperties Account properties object
     */
    private fun onAccountSelected(accountProperties: AccountProperties) {
        mUserAccount!!.name = accountProperties.name

        Toast.makeText(this, "Account: "+accountProperties.name, Toast.LENGTH_SHORT).show()

        val password = tietPin.text!!.toString()

        // Stores the accounts this key refers to
//        database.putOwnedUserAccounts(applicationContext, mUserAccount, password)

        // Stores the id of the currently active user account
//        PreferenceManager.getDefaultSharedPreferences(applicationContext)
//            .edit()
//            .putString(Constants.KEY_CURRENT_ACCOUNT_ID, mUserAccount!!.objectId)
//            .apply()

        // Trying to store all possible authorities (owner, active and memo)
//        for (i in 0..2) {
//            mBrainKey.setSequenceNumber(i)
//            saveAccountAuthorities(mBrainKey, accountProperties)
//        }

        // TODO move to MainActivity
    }
}