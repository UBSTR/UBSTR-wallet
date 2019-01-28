package cy.agorise.bitsybitshareswallet.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.database.entities.Merchant
import io.reactivex.Single

@Dao
interface MerchantDao {
    @Insert
    fun insert(merchant: Merchant)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(merchants: List<Merchant>)

    @Query("SELECT * FROM merchants")
    fun getAll(): LiveData<List<Merchant>>

    @Query("SELECT * FROM merchants")
    fun getAllSync(): List<Merchant>

    @Query("SELECT * FROM merchants WHERE (name LIKE :query) OR (address LIKE :query) OR (phone LIKE :query) OR (telegram LIKE :query) OR (website LIKE :query)")
    fun findMerchantsByWord(query: String): Single<List<Merchant>>

    @Query("SELECT * FROM merchants WHERE (name LIKE :query) OR (address LIKE :query) OR (phone LIKE :query) OR (telegram LIKE :query) OR (website LIKE :query)")
    fun findMerchantsByWordSync(query: String): List<Merchant>

    @Query("DELETE FROM merchants")
    fun deleteAll()
}