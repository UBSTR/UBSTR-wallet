package cy.agorise.bitsybitshareswallet.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import cy.agorise.bitsybitshareswallet.database.entities.Merchant

@Dao
interface MerchantDao {
    @Insert
    fun insert(merchant: Merchant)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(merchants: List<Merchant>)
}