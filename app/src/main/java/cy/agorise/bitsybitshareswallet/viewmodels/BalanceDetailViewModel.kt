package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.joins.BalanceDetail
import cy.agorise.bitsybitshareswallet.repositories.BalanceDetailRepository


class BalanceDetailViewModel(application: Application) : AndroidViewModel(application) {
    private var mRepository = BalanceDetailRepository(application)

    internal fun getAll(): LiveData<List<BalanceDetail>> {
        return mRepository.getAll()
    }
}