package cy.agorise.bitsybitshareswallet.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.database.entities.UserAccount

@Dao
interface UserAccountDao {
    @Insert
    fun insert(userAccount: UserAccount)

    @Query("SELECT * FROM user_accounts WHERE user_accounts.id = :id")
    fun getUserAccount(id: String): LiveData<UserAccount>

    @Query("SELECT * FROM user_accounts")
    fun getAllUserAccounts(): LiveData<List<UserAccount>>
}