package cy.agorise.bitsybitshareswallet.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.database.entities.UserAccount

@Dao
interface UserAccountDao {
    @Insert
    fun insert(userAccount: UserAccount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(userAccounts: List<UserAccount>)

    @Query("SELECT * FROM user_accounts WHERE user_accounts.id = :id")
    fun getUserAccount(id: String): LiveData<UserAccount>

    @Query("SELECT * FROM user_accounts")
    fun getAll(): LiveData<List<UserAccount>>

    // TODO not sure if this is the best place for this query as it involves two entities
    @Query("SELECT DISTINCT destination FROM transfers WHERE destination NOT IN (SELECT id FROM user_accounts) UNION SELECT DISTINCT source FROM transfers WHERE source NOT IN (SELECT id FROM user_accounts)")
    fun getMissingAccountIds(): LiveData<List<String>>
}