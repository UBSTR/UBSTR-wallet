package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.entities.UserAccount
import cy.agorise.bitsybitshareswallet.repositories.UserAccountRepository

class UserAccountViewModel(application: Application) : AndroidViewModel(application) {
    private var mRepository = UserAccountRepository(application)

    internal fun getUserAccount(id: String): LiveData<UserAccount> {
        return mRepository.getUserAccount(id)
    }

//    fun insert(userAccount: UserAccount) {
//        mRepository.insert(userAccount)
//    }
}