package cy.agorise.bitsybitshareswallet.models

import androidx.annotation.NonNull

class AmbassadorLocation(
    var id: String?,
    var name: String?,
    var country: String?
) : Comparable<AmbassadorLocation> {

    override fun toString(): String {
        return this.name!!
    }

    override fun equals(other: Any?): Boolean {
        return other is AmbassadorLocation && id == other.id
    }

    override fun compareTo(@NonNull other: AmbassadorLocation): Int {
        return name!!.compareTo(other.name!!)
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (country?.hashCode() ?: 0)
        return result
    }
}