package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.entities.UserAccount
import cy.agorise.bitsybitshareswallet.repositories.AuthorityRepository
import cy.agorise.bitsybitshareswallet.repositories.UserAccountRepository

class SettingsFragmentViewModel(application: Application) : AndroidViewModel(application) {
    private var mUserAccountRepository = UserAccountRepository(application)
    private var mAuthorityRepository = AuthorityRepository(application)

    internal fun getUserAccount(id: String): LiveData<UserAccount> {
        return mUserAccountRepository.getUserAccount(id)
    }

    internal fun getWIF(userId: String, authorityType: Int): LiveData<String> {
        return mAuthorityRepository.getWIF(userId, authorityType)
    }
}