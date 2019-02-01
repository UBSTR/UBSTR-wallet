package cy.agorise.bitsybitshareswallet.database.joins

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query

@Dao
interface TransferDetailDao {

    @Query("SELECT transfers.id, (SELECT name FROM user_accounts WHERE user_accounts.id=transfers.source) AS `from`, (SELECT name FROM user_accounts WHERE user_accounts.id=transfers.destination) AS `to`, (CASE WHEN destination=:userId THEN 1 ELSE 0 END) AS `direction`, transfers.memo AS `memo`, transfers.timestamp AS `date`, transfers.transfer_amount AS `assetAmount`, assets.precision AS `assetPrecision`, assets.symbol AS `assetSymbol`, assets.issuer as `assetIssuer` FROM transfers INNER JOIN assets WHERE transfers.transfer_asset_id = assets.id")
    fun getAll(userId: String): LiveData<List<TransferDetail>>
}