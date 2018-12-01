package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.entities.Balance
import cy.agorise.bitsybitshareswallet.repositories.BalanceRepository


class BalanceViewModel(application: Application) : AndroidViewModel(application) {
    private var mRepository = BalanceRepository(application)

    internal fun getAll(): LiveData<List<Balance>> {
        return mRepository.getAll()
    }
}