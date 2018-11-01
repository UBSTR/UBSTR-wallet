package cy.agorise.bitsybitshareswallet.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cy.agorise.bitsybitshareswallet.models.Operation

@Dao
interface OperationDao {
    @Insert
    fun insert(operation: Operation)

    @Query("SELECT * FROM operations")
    fun getAllOperations(): LiveData<List<Operation>>
}