package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.entities.Merchant
import cy.agorise.bitsybitshareswallet.repositories.MerchantRepository

class MerchantViewModel(application: Application) : AndroidViewModel(application) {
    private var mMerchantRepository = MerchantRepository(application)

    internal fun getAllMerchants(): LiveData<List<Merchant>> {
        return mMerchantRepository.getAll()
    }
}