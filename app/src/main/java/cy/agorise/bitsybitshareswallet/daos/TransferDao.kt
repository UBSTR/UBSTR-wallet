package cy.agorise.bitsybitshareswallet.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.entities.Transfer

@Dao
interface TransferDao {
    @Insert
    fun insert(transfer: Transfer)

    // TODO find a way to return number of added rows
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(transfers: List<Transfer>)

    @Query("SELECT COUNT(*) FROM transfers")
    fun getCount(): Int

    @Query("SELECT * FROM transfers")
    fun getAllTransfers(): LiveData<List<Transfer>>

    @Query("DELETE FROM transfers")
    fun deleteAll()
}