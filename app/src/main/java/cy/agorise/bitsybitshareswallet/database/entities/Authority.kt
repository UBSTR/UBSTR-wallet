package cy.agorise.bitsybitshareswallet.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "authorities")
data class Authority (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "authority_type") val authorityType: Int,
    @ColumnInfo(name = "encrypted_wif") val encryptedWIF: String,
    @ColumnInfo(name = "encrypted_brain_key") val encryptedBrainKey: String,
    @ColumnInfo(name = "encrypted_sequence_number") val encryptedSequenceNumber: String
)