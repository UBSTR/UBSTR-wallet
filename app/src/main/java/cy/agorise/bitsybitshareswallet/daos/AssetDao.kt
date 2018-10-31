package cy.agorise.bitsybitshareswallet.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.Asset

@Dao
interface AssetDao {

    @Query("SELECT * FROM assets")
    fun getAllAssets(): LiveData<List<Asset>>

    @Insert
    fun insert(asset: Asset)
}
