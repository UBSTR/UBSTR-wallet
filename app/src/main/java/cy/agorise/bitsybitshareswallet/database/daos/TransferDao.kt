package cy.agorise.bitsybitshareswallet.database.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import cy.agorise.bitsybitshareswallet.database.entities.Transfer
import io.reactivex.Observable
import io.reactivex.Single

@Dao
interface TransferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transfer: Transfer)

    @Update()
    fun update(transfer: Transfer)

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

    @Query("SELECT * FROM transfers WHERE timestamp != 0 AND bts_value = -1 AND transfer_asset_id != '1.3.0' LIMIT 1")
    fun getTransfersWithMissingBtsValue(): LiveData<Transfer>

    @Query("SELECT * FROM transfers WHERE id NOT IN (SELECT transfer_id FROM equivalent_values WHERE symbol = :symbol) AND bts_value >= 0 AND timestamp > 0 LIMIT 1")
    fun getTransfersWithMissingValueIn(symbol: String): Observable<Transfer>

    @Query("DELETE FROM transfers")
    fun deleteAll()
}