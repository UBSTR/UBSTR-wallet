package cy.agorise.bitsybitshareswallet.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.Asset

@Dao
interface AssetDao {
    @Insert
    fun insert(asset: Asset)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(assets: List<Asset>)

    @Query("SELECT * FROM assets")
    fun getAllAssets(): LiveData<List<Asset>>
}
