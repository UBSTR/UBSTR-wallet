package cy.agorise.bitsybitshareswallet.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.BrainKey

@Dao
interface BrainKeyDao {
    @Insert
    fun insert(brainKey: BrainKey)

    @Query("SELECT * FROM brain_keys")
    fun getAllBrainKeys(): LiveData<List<BrainKey>>
}