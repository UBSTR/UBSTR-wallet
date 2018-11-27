package cy.agorise.bitsybitshareswallet.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "symbol") val symbol: String,
    @ColumnInfo(name = "precision") val precision: Int,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "bit_asset_id") val bitAssetId: String
)
