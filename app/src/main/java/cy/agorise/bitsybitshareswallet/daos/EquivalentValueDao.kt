package cy.agorise.bitsybitshareswallet.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.EquivalentValue

@Dao
interface EquivalentValueDao {
    @Insert
    fun insert(equivalentValue: EquivalentValue)

    @Query("SELECT * FROM equivalent_values")
    fun getAllEquivalentValues(): LiveData<List<EquivalentValue>>
}