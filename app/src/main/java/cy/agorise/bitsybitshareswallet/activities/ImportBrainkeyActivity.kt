package cy.agorise.bitsybitshareswallet.activities

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.jakewharton.rxbinding2.widget.RxTextView
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.database.entities.Authority
import cy.agorise.bitsybitshareswallet.repositories.AuthorityRepository
import cy.agorise.bitsybitshareswallet.repositories.UserAccountRepository
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.CryptoUtils
import cy.agorise.graphenej.*
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.calls.GetAccounts
import cy.agorise.graphenej.api.calls.GetKeyReferences
import cy.agorise.graphenej.models.AccountProperties
import cy.agorise.graphenej.models.JsonRpcResponse
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_import_brainkey.*
import org.bitcoinj.core.ECKey
import java.util.ArrayList
import java.util.concurrent.TimeUnit

// TODO Add method to load the 20? most important assets
// TODO add progress bar or something while the user waits for the import response from the node

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

    private var mDisposables = CompositeDisposable()

    private var isPINValid = false
    private var isPINConfirmationValid = false
    private var isBrainKeyValid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_brainkey)

        // Use RxJava Debounce to update the PIN error only after the user stops writing for > 500 ms
        mDisposables.add(
            RxTextView.textChanges(tietPin)
                .skipInitialValue()
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { validatePIN() }
        )

        // Use RxJava Debounce to update the PIN Confirmation error only after the user stops writing for > 500 ms
        mDisposables.add(
            RxTextView.textChanges(tietPinConfirmation)
                .skipInitialValue()
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { validatePINConfirmation() }
        )

        // Use RxJava Debounce to update the BrainKey error only after the user stops writing for > 500 ms
        mDisposables.add(
            RxTextView.textChanges(tietBrainKey)
                .skipInitialValue()
                .debounce(500, TimeUnit.MILLISECONDS)
                .map { it.toString().trim() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { validateBrainKey(it) }
        )

        btnImport.isEnabled = false
        btnImport.setOnClickListener { verifyBrainKey(false) }
    }

    private fun validatePIN() {
        val pin = tietPin.text.toString()

        if (pin.length < Constants.MIN_PIN_LENGTH) {
            tilPin.error = getString(R.string.error__pin_too_short)
            isPINValid = false
        } else {
            tilPin.isErrorEnabled = false
            isPINValid = true
        }

        validatePINConfirmation()
    }

    private fun validatePINConfirmation() {
        val pinConfirmation = tietPinConfirmation.text.toString()

        if (pinConfirmation != tietPin.text.toString()) {
            tilPinConfirmation.error = getString(R.string.error__pin_mismatch)
            isPINConfirmationValid = false
        } else {
            tilPinConfirmation.isErrorEnabled = false
            isPINConfirmationValid = true
        }

        enableDisableImportButton()
    }

    private fun validateBrainKey(brainKey: String) {
        if (brainKey.isEmpty() || !brainKey.contains(" ") || brainKey.split(" ").size !in 12..16) {
            tilBrainKey.error = getString(R.string.error__enter_correct_brainkey)
            isBrainKeyValid = false
        } else {
            tilBrainKey.isErrorEnabled = false
            isBrainKeyValid = true
        }

        enableDisableImportButton()
    }

    private fun enableDisableImportButton() {
        btnImport.isEnabled =  (isPINValid && isPINConfirmationValid && isBrainKeyValid)
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
                    .listItemsSingleChoice (items = candidates, initialSelection = -1) { _, index, _ ->
                        if (index >= 0) {
                            // If one account was selected, we keep a reference to it and
                            // store the account properties
                            mUserAccount = mUserAccountCandidates!![index]
                            onAccountSelected(accountPropertiesList[index])
                        }
                    }
                    .positiveButton(android.R.string.ok)
                    .negativeButton(android.R.string.cancel) {
                        mKeyReferencesAttempts = 0
                    }
                    .cancelable(false)
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

        val encryptedPIN = CryptoUtils.encrypt(this, tietPin.text!!.toString())

        // Stores the user selected PIN encrypted
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putString(Constants.KEY_ENCRYPTED_PIN, encryptedPIN)
            .apply()

        // Stores the accounts this key refers to
        val id = accountProperties.id
        val name = accountProperties.name
        val isLTM = accountProperties.membership_expiration_date == Constants.LIFETIME_EXPIRATION_DATE

        val userAccount = cy.agorise.bitsybitshareswallet.database.entities.UserAccount(id, name, isLTM)

        val userAccountRepository = UserAccountRepository(application)
        userAccountRepository.insert(userAccount)

        // Stores the id of the currently active user account
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putString(Constants.KEY_CURRENT_ACCOUNT_ID, mUserAccount!!.objectId)
            .apply()

        // Trying to store all possible authorities (owner, active and memo) into the database
        val ownerAuthority = accountProperties.owner
        val activeAuthority = accountProperties.active
        val options = accountProperties.options

        for (i in 0..2) {
            mBrainKey!!.sequenceNumber = i
            val publicKey = PublicKey(ECKey.fromPublicOnly(mBrainKey!!.privateKey.pubKey))

            if (ownerAuthority.keyAuths.keys.contains(publicKey)) {
                addAuthorityToDatabase(accountProperties.id, AuthorityType.OWNER.ordinal, mBrainKey!!)
            }
            if (activeAuthority.keyAuths.keys.contains(publicKey)) {
                addAuthorityToDatabase(accountProperties.id, AuthorityType.ACTIVE.ordinal, mBrainKey!!)
            }
            if (options.memoKey == publicKey) {
                addAuthorityToDatabase(accountProperties.id, AuthorityType.MEMO.ordinal, mBrainKey!!)
            }
        }

        // Stores a flag into the SharedPreferences to tell the app there is an active account and there is no need
        // to show this activity again, until the account is removed.
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putBoolean(Constants.KEY_INITIAL_SETUP_DONE, true)
            .apply()

        // Send the user to the MainActivity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * Adds the given BrainKey encrypted as AuthorityType of userId.
     */
    private fun addAuthorityToDatabase(userId: String, authorityType: Int, brainKey: BrainKey) {
        val brainKeyWords = brainKey.brainKey
        val wif = brainKey.walletImportFormat
        val sequenceNumber = brainKey.sequenceNumber

        val encryptedBrainKey = CryptoUtils.encrypt(this, brainKeyWords)
        val encryptedSequenceNumber = CryptoUtils.encrypt(this, sequenceNumber.toString())
        val encryptedWIF = CryptoUtils.encrypt(this, wif)

        val authority = Authority(0, userId, authorityType, encryptedWIF, encryptedBrainKey, encryptedSequenceNumber)

        val authorityRepository = AuthorityRepository(this)
        authorityRepository.insert(authority)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!mDisposables.isDisposed) mDisposables.dispose()
    }
}