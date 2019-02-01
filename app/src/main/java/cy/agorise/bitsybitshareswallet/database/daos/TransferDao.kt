package cy.agorise.bitsybitshareswallet.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.database.entities.Transfer
import io.reactivex.Single

@Dao
interface TransferDao {
    @Insert
    fun insert(transfer: Transfer)

    // TODO find a way to return number of added rows
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(transfers: List<Transfer>)

    @Query("UPDATE transfers SET timestamp=:timestamp WHERE block_number=:blockNumber")
    fun setBlockTime(blockNumber: Long, timestamp: Long)

    @Query("SELECT * FROM transfers")
    fun getAll(): LiveData<List<Transfer>>

    @Query("SELECT COUNT(*) FROM transfers")
    fun getCount(): Single<Int>

    @Query("SELECT block_number FROM transfers WHERE timestamp='0' LIMIT 1")
    fun getTransferBlockNumberWithMissingTime(): LiveData<Long>

    @Query("SELECT * FROM transfers WHERE id NOT IN (SELECT transfer_id FROM equivalent_values WHERE symbol = :symbol)")
    fun getTransfersWithMissingValueIn(symbol: String): LiveData<List<Transfer>>

    @Query("DELETE FROM transfers")
    fun deleteAll()
}