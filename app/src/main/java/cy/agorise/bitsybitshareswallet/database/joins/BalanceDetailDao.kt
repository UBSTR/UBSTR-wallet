package cy.agorise.bitsybitshareswallet.database.joins

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query

@Dao
interface BalanceDetailDao {
    @Query("SELECT assets.id AS id, balances.asset_amount AS amount, assets.precision, assets.symbol " +
            "FROM balances INNER JOIN assets on balances.asset_id = assets.id WHERE balances.asset_amount > 0")
    fun getAll(): LiveData<List<BalanceDetail>>
}