package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.repositories.AuthorityRepository

class SendTransactionViewModel(application: Application) : AndroidViewModel(application) {
    private var mAuthorityRepository = AuthorityRepository(application)

    internal fun getWIF(userId: String, authorityType: Int): LiveData<String> {
        return mAuthorityRepository.getWIF(userId, authorityType)
    }
}