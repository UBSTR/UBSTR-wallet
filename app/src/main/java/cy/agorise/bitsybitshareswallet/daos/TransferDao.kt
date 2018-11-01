package cy.agorise.bitsybitshareswallet.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.Transfer

@Dao
interface TransferDao {
    @Insert
    fun insert(transfer: Transfer)

    @Query("SELECT * FROM transfers")
    fun getAllTransfers(): LiveData<List<Transfer>>
}