package cy.agorise.bitsybitshareswallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.entities.Asset
import cy.agorise.bitsybitshareswallet.repositories.AssetRepository

class AssetViewModel(application: Application) : AndroidViewModel(application) {
    private var mRepository = AssetRepository(application)

    internal fun getAllNonZero(): LiveData<List<Asset>> {
        return mRepository.getAllNonZero()
    }
}