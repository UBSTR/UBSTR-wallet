package cy.agorise.bitsybitshareswallet.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.Authority
import cy.agorise.bitsybitshareswallet.models.UserAccountAuthority

@Dao
interface UserAccountAuthorityDao {
    @Insert
    fun insert(userAccountAuthority: UserAccountAuthority)

//    @Query("SELECT * FROM authorities INNER JOIN user_accounts__authorities ON user_accounts.id=user_accounts__authorities.user_account_id WHERE user_accounts__authorities.user_account_id=:userAccountId")
//    fun getAuthoritiesForUserAccount(userAccountId: String): LiveData<List<Authority>>
}