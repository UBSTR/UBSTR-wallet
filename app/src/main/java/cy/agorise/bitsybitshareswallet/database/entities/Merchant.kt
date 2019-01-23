package cy.agorise.bitsybitshareswallet.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

@Entity(tableName = "merchants")
data class Merchant(
    @PrimaryKey
    @ColumnInfo(name = "id") val _id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "address") val address: String?,
    @ColumnInfo(name = "lat") val lat: Double,
    @ColumnInfo(name = "lon") val lon: Double,
    @ColumnInfo(name = "phone") val phone: String?,
    @ColumnInfo(name = "telegram") val telegram: String?,
    @ColumnInfo(name = "website") val website: String?
) : ClusterItem {
    override fun getSnippet(): String {
        return address ?: ""
    }

    override fun getTitle(): String {
        return name
    }

    override fun getPosition(): LatLng {
        return LatLng(lat, lon)
    }

}