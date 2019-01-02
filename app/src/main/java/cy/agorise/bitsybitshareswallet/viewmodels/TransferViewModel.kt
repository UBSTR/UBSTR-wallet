package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.repositories.TransferRepository

class TransferViewModel(application: Application) : AndroidViewModel(application) {
    private var mRepository = TransferRepository(application)

    internal fun setBlockTime(blockNumber: Long, timestamp: Long) {
        mRepository.setBlockTime(blockNumber, timestamp)
    }

    internal fun getTransferBlockNumberWithMissingTime(): LiveData<Long> {
        return mRepository.getTransferBlockNumberWithMissingTime()
    }
}