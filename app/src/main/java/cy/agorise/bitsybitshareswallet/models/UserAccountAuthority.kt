package cy.agorise.bitsybitshareswallet.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * Table to create a N:N relationship between [UserAccount] and [Authority]
 */
@Entity(tableName = "user_accounts__authorities",
    primaryKeys = ["user_account_id", "authority_id"],
    foreignKeys = [ForeignKey(
        entity = UserAccount::class,
        parentColumns = ["id"],
        childColumns = ["user_account_id"]
    ), ForeignKey(
        entity = Authority::class,
        parentColumns = ["id"],
        childColumns = ["authority_id"]
    )])
data class UserAccountAuthority (
    @ColumnInfo(name = "user_account_id") val userAccountId: String,
    @ColumnInfo(name = "authority_id") val authorityId: Long,
    @ColumnInfo(name = "weight") val weight: Int
)