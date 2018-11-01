package cy.agorise.bitsybitshareswallet.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.UserAccount

@Dao
interface UserAccountDao {
    @Insert
    fun insert(userAccount: UserAccount)

    @Query("SELECT * FROM user_accounts")
    fun getAllUserAccounts(): LiveData<List<UserAccount>>
}