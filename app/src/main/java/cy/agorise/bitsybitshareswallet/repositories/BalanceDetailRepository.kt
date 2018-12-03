package cy.agorise.bitsybitshareswallet.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.database.joins.BalanceDetail
import cy.agorise.bitsybitshareswallet.database.joins.BalanceDetailDao

class BalanceDetailRepository internal constructor(context: Context) {

    private val mBalanceDetailDao: BalanceDetailDao

    init {
        val db = BitsyDatabase.getDatabase(context)
        mBalanceDetailDao = db!!.balanceDetailDao()
    }

    fun getAll(): LiveData<List<BalanceDetail>> {
        return mBalanceDetailDao.getAll()
    }

}