package cy.agorise.bitsybitshareswallet.fragments

import android.preference.PreferenceManager
import androidx.navigation.fragment.findNavController
import cy.agorise.bitsybitshareswallet.database.entities.Authority
import cy.agorise.bitsybitshareswallet.repositories.AuthorityRepository
import cy.agorise.bitsybitshareswallet.repositories.UserAccountRepository
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.CryptoUtils
import cy.agorise.graphenej.AuthorityType
import cy.agorise.graphenej.BrainKey
import cy.agorise.graphenej.PublicKey
import cy.agorise.graphenej.models.AccountProperties
import org.bitcoinj.core.ECKey

abstract class BaseAccountFragment : ConnectedFragment() {

    /** Private variable that will hold an instance of the [BrainKey] class */
    protected var mBrainKey: BrainKey? = null

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
    protected fun onAccountSelected(accountProperties: AccountProperties, pin: String) {
        val encryptedPIN = CryptoUtils.encrypt(context!!, pin)

        // Stores the user selected PIN encrypted
        PreferenceManager.getDefaultSharedPreferences(context!!)
            .edit()
            .putString(Constants.KEY_ENCRYPTED_PIN, encryptedPIN)
            .apply()

        // Stores the accounts this key refers to
        val id = accountProperties.id
        val name = accountProperties.name
        val isLTM = accountProperties.membership_expiration_date == Constants.LIFETIME_EXPIRATION_DATE

        val userAccount = cy.agorise.bitsybitshareswallet.database.entities.UserAccount(id, name, isLTM)

        val userAccountRepository = UserAccountRepository(context!!.applicationContext)
        userAccountRepository.insert(userAccount)

        // Stores the id of the currently active user account
        PreferenceManager.getDefaultSharedPreferences(context!!).edit()
            .putString(Constants.KEY_CURRENT_ACCOUNT_ID, accountProperties.id).apply()

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

        // Send the user back to HomeFragment
        findNavController().popBackStack()
    }

    /**
     * Adds the given BrainKey encrypted as AuthorityType of userId.
     */
    private fun addAuthorityToDatabase(userId: String, authorityType: Int, brainKey: BrainKey) {
        val brainKeyWords = brainKey.brainKey
        val wif = brainKey.walletImportFormat
        val sequenceNumber = brainKey.sequenceNumber

        val encryptedBrainKey = CryptoUtils.encrypt(context!!, brainKeyWords)
        val encryptedSequenceNumber = CryptoUtils.encrypt(context!!, sequenceNumber.toString())
        val encryptedWIF = CryptoUtils.encrypt(context!!, wif)

        val authority = Authority(0, userId, authorityType, encryptedWIF, encryptedBrainKey, encryptedSequenceNumber)

        val authorityRepository = AuthorityRepository(context!!)
        authorityRepository.insert(authority)
    }
}