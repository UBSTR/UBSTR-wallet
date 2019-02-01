package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.repositories.TransferRepository
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers

class TransferViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "TransferViewModel"
    private var mRepository = TransferRepository(application)

    internal fun setBlockTime(blockNumber: Long, timestamp: Long) {
        mRepository.setBlockTime(blockNumber, timestamp)
    }

    internal fun getTransferBlockNumberWithMissingTime(): LiveData<Long> {
        return mRepository.getTransferBlockNumberWithMissingTime()
    }

    fun getTransfersWithMissingValueIn(symbol: String) {
        mRepository.getTransfersWithMissingValueIn(symbol)
    }
}
