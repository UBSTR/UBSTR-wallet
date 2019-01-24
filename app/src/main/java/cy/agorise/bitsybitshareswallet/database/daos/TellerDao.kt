package cy.agorise.bitsybitshareswallet.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.database.entities.Teller

@Dao
interface TellerDao {
    @Insert
    fun insert(teller: Teller)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(tellers: List<Teller>)

    @Query("SELECT * FROM tellers")
    fun getAll(): LiveData<List<Teller>>

    @Query("DELETE FROM tellers")
    fun deleteAll()
}