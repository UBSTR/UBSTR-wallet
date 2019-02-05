package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetail
import cy.agorise.bitsybitshareswallet.repositories.TransferDetailRepository

class EReceiptViewModel(application: Application) : AndroidViewModel(application) {
    private var mTransferDetailRepository = TransferDetailRepository(application)

    internal fun get(userId: String, transferId: String): LiveData<TransferDetail> {
        return mTransferDetailRepository.get(userId, transferId)
    }
}