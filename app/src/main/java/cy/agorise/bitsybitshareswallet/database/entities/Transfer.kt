package cy.agorise.bitsybitshareswallet.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfers")
data class Transfer (
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "block_number") val blockNumber: Long,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "fee_amount") val feeAmount: Long,
    @ColumnInfo(name = "fee_asset_id") val feeAssetId: String, // TODO should be foreign key to Asset
    @ColumnInfo(name = "source") val source: String, // TODO should be foreign key to UserAccount
    @ColumnInfo(name = "destination") val destination: String, // TODO should be foreign key to UserAccount
    @ColumnInfo(name = "transfer_amount") val transferAmount: Long,
    @ColumnInfo(name = "transfer_asset_id") val transferAssetId: String, // TODO should be foreign key to Asset
    @ColumnInfo(name = "memo") val memo: String
)