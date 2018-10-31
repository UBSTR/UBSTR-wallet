package cy.agorise.bitsybitshareswallet.models

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "balances", primaryKeys = ["user_id", "asset_id"])
data class Balance(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "asset_id") val assetId: String,
    @ColumnInfo(name = "asset_amount") val assetAmount: Long,
    @ColumnInfo(name = "last_update") val lastUpdate: Long
)