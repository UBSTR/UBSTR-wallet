package cy.agorise.bitsybitshareswallet.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.database.entities.Authority
import io.reactivex.Single

@Dao
interface AuthorityDao {
    @Insert
    fun insert(authority: Authority)

    @Query("SELECT * FROM authorities")
    fun getAll(): LiveData<List<Authority>>

    @Query("SELECT encrypted_wif FROM authorities WHERE user_id=:userId AND authority_type=:authorityType")
    fun getWIF(userId: String, authorityType: Int): Single<String>
}