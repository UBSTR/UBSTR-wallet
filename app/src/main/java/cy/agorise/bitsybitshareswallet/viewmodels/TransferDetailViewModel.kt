package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetail
import cy.agorise.bitsybitshareswallet.repositories.TransferDetailRepository

class TransferDetailViewModel(application: Application) : AndroidViewModel(application) {
    private var mRepository = TransferDetailRepository(application)

    internal fun getAll(): LiveData<List<TransferDetail>> {
        return mRepository.getAll()
    }
}