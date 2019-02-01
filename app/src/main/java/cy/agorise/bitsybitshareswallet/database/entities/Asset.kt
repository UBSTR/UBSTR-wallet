package cy.agorise.bitsybitshareswallet.database.entities

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
    @ColumnInfo(name = "issuer") val issuer: String
) {
    // Add the bit prefix to smartcoins, ie bitUSD, bitEUR, bitMXN, etc.
    override fun toString(): String {
        if (issuer == "1.2.0")
            return "bit$symbol"
        return symbol
    }
}
