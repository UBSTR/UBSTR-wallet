package cy.agorise.bitsybitshareswallet.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(tableName = "equivalent_values",
    primaryKeys = arrayOf("transfer_id", "symbol"),
    foreignKeys = [ForeignKey(
        entity = Transfer::class,
        parentColumns = ["id"],
        childColumns = ["transfer_id"]
    )]
)
data class EquivalentValue (
    @ColumnInfo(name = "transfer_id") val transferId: String,
    @ColumnInfo(name = "value") val value: Long,
    @ColumnInfo(name = "symbol") val symbol: String
)