package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.entities.Transfer
import cy.agorise.bitsybitshareswallet.repositories.TransferRepository

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private var mRepository = TransferRepository(application)

    internal fun getAll(): LiveData<List<Transfer>> {
        return mRepository.getAll()
    }
}