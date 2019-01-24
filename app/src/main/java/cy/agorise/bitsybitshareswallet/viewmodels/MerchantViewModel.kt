package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.entities.Merchant
import cy.agorise.bitsybitshareswallet.database.entities.Teller
import cy.agorise.bitsybitshareswallet.repositories.MerchantRepository
import cy.agorise.bitsybitshareswallet.repositories.TellerRepository

class MerchantViewModel(application: Application) : AndroidViewModel(application) {
    private var mMerchantRepository = MerchantRepository(application)
    private var mTellerRepository = TellerRepository(application)

    internal fun getAllMerchants(): LiveData<List<Merchant>> {
        return mMerchantRepository.getAll()
    }

    internal fun getAllTellers(): LiveData<List<Teller>> {
        return mTellerRepository.getAll()
    }
}