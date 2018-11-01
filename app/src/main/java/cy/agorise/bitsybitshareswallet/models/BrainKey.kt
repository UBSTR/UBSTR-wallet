package cy.agorise.bitsybitshareswallet.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="brain_keys")
data class BrainKey(
    @PrimaryKey
    @ColumnInfo(name = "public_key") val publicKey: String,
    @ColumnInfo(name = "encrypted_brain_key") val encryptedBrainKey: String,
    @ColumnInfo(name = "sequence_number") val sequenceNumber: Long
)