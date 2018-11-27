package cy.agorise.bitsybitshareswallet.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.entities.Balance

@Dao
interface BalanceDao {
    @Insert
    fun insert(balance: Balance)

    @Query("SELECT * FROM balances")
    fun getAllBalances(): LiveData<List<Balance>>
}