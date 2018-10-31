package cy.agorise.bitsybitshareswallet.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "symbol") val symbol: String,
    @ColumnInfo(name = "precision") val precision: Int,
    @ColumnInfo(name = "issuer") val issuer: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "max_supply") val maxSupply: Long, // TODO verify type
    @ColumnInfo(name = "market_fee_percent") val marketFeePercent: Float, // TODO verify type
    @ColumnInfo(name = "max_market_fee") val maxMarketFee: Float, // TODO verify type
    @ColumnInfo(name = "issuer_permissions") val issuerPermissions: Int,
    @ColumnInfo(name = "flags") val flags: Int,
    @ColumnInfo(name = "asset_type") val assetType: Int,
    @ColumnInfo(name = "bit_asset_id") val bitAssetId: String,
    @ColumnInfo(name = "holders_count") val holdersCount: Int
)
