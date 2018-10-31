package cy.agorise.bitsybitshareswallet.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.Authority

@Dao
interface AuthorityDao {

    @Query("SELECT * FROM authorities")
    fun getAllAuthorities(): LiveData<List<Authority>>

    @Insert
    fun insert(authority: Authority)
}