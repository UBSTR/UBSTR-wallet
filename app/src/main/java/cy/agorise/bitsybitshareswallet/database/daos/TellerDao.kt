package cy.agorise.bitsybitshareswallet.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.database.entities.Teller
import io.reactivex.Single

@Dao
interface TellerDao {
    @Insert
    fun insert(teller: Teller)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(tellers: List<Teller>)

    @Query("SELECT * FROM tellers")
    fun getAll(): LiveData<List<Teller>>

    @Query("SELECT * FROM tellers WHERE (name LIKE :query) OR (address LIKE :query) OR (phone LIKE :query) OR (telegram LIKE :query) OR (website LIKE :query)")
    fun findTellersByWord(query: String): Single<List<Teller>>

    @Query("DELETE FROM tellers")
    fun deleteAll()
}