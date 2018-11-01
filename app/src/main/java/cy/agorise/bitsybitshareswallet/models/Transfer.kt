package cy.agorise.bitsybitshareswallet.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "transfers",foreignKeys =
    [ForeignKey(
        entity = Operation::class,
        parentColumns = ["id"],
        childColumns = ["operation_id"]
    )]
)
data class Transfer (
    @PrimaryKey
    @ColumnInfo(name = "operation_id") val operationId: String,
    @ColumnInfo(name = "fee_amount") val feeAmount: Long,
    @ColumnInfo(name = "fee_asset_id") val feeAssetId: String, // TODO should be foreign key to Asset
    @ColumnInfo(name = "source") val source: String, // TODO should be foreign key to UserAccount
    @ColumnInfo(name = "destination") val destination: String, // TODO should be foreign key to UserAccount
    @ColumnInfo(name = "transfer_amount") val transferAmount: Long,
    @ColumnInfo(name = "transfer_asset_id") val transferAssetId: String, // TODO should be foreign key to Asset
    @ColumnInfo(name = "memo") val memo: String,
    @ColumnInfo(name = "memo_from_key") val memoFromKey: String,
    @ColumnInfo(name = "memo_to_key") val memoToKey: String
)