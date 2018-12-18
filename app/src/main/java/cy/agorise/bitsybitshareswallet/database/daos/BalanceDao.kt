package cy.agorise.bitsybitshareswallet.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.database.entities.Balance

@Dao
interface BalanceDao {
    @Insert
    fun insert(balance: Balance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(balances: List<Balance>)

    @Query("SELECT * FROM balances")
    fun getAll(): LiveData<List<Balance>>

    // TODO not sure if this is the best place for this query as it involves two entities
    @Query("SELECT DISTINCT asset_id FROM balances WHERE asset_id NOT IN (SELECT id FROM assets)")
    fun getMissingAssetIds(): LiveData<List<String>>
}