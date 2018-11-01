package cy.agorise.bitsybitshareswallet.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "equivalent_values",foreignKeys =
    [ForeignKey(
        entity = Transfer::class,
        parentColumns = ["operation_id"],
        childColumns = ["transfer_id"]
    ), ForeignKey(
        entity = Asset::class,
        parentColumns = ["id"],
        childColumns = ["asset_id"]
    )]
)
data class EquivalentValue (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "transfer_id") val transferId: String,
    @ColumnInfo(name = "value") val value: Long,
    @ColumnInfo(name = "asset_id") val assetId: String
)