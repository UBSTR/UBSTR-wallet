package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cy.agorise.bitsybitshareswallet.repositories.TransferRepository

class ConnectedActivityViewModel(application: Application) : AndroidViewModel(application) {
    private var mTransfersRepository = TransferRepository(application)

    fun observeMissingEquivalentValuesIn(symbol: String) {
        mTransfersRepository.observeMissingEquivalentValuesIn(symbol)
    }

    override fun onCleared() {
        super.onCleared()
        mTransfersRepository.onCleared()
    }
}