package cy.agorise.bitsybitshareswallet.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "authorities")
data class Authority (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "encrypted_private_key") val encryptedBrainkey: String,
    @ColumnInfo(name = "user_id") val userId: String
)