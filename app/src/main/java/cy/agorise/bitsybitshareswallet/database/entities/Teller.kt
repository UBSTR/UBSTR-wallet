package cy.agorise.bitsybitshareswallet.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tellers")
data class Teller(
    @PrimaryKey
    @ColumnInfo(name = "id") val _id: String,
    @ColumnInfo(name = "name") val gt_name: String,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "lat") val lat: Float,
    @ColumnInfo(name = "lon") val lon: Float,
    @ColumnInfo(name = "phone") val phone: String,
    @ColumnInfo(name = "telegram") val telegram: String,
    @ColumnInfo(name = "website") val url: String
)