package cy.agorise.bitsybitshareswallet.models

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "balances", primaryKeys = ["user_account_id", "asset_id"])
// TODO make userAccountId and assetId be ForeignKeys
data class Balance(
    @ColumnInfo(name = "user_account_id") val userAccountId: String,
    @ColumnInfo(name = "asset_id") val assetId: String,
    @ColumnInfo(name = "asset_amount") val assetAmount: Long,
    @ColumnInfo(name = "last_update") val lastUpdate: Long
)