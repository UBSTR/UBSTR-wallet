package cy.agorise.bitsybitshareswallet.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

@Entity(tableName = "tellers")
data class Teller(
    @PrimaryKey
    @ColumnInfo(name = "id") val _id: String,
    @ColumnInfo(name = "name") val gt_name: String,
    @ColumnInfo(name = "address") val address: String?,
    @ColumnInfo(name = "lat") val lat: Double,
    @ColumnInfo(name = "lon") val lon: Double,
    @ColumnInfo(name = "phone") val phone: String?,
    @ColumnInfo(name = "telegram") val telegram: String?,
    @ColumnInfo(name = "website") val url: String?
) : ClusterItem {
    override fun getSnippet(): String {
        return address ?: ""
    }

    override fun getTitle(): String {
        return gt_name
    }

    override fun getPosition(): LatLng {
        return LatLng(lat, lon)
    }

}