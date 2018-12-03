package cy.agorise.bitsybitshareswallet.database.joins

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query

@Dao
interface TransferDetailDao {

    @Query("SELECT id, IFNULL((SELECT name FROM user_accounts WHERE user_accounts.id=transfers.source), '') AS `from`, IFNULL((SELECT name FROM user_accounts WHERE user_accounts.id=transfers.destination), '') AS `to` FROM transfers")
    fun getAll(): LiveData<List<TransferDetail>>
}