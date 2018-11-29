package cy.agorise.bitsybitshareswallet.repositories

import android.content.Context
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.daos.BalanceDao
import cy.agorise.bitsybitshareswallet.daos.BitsyDatabase
import cy.agorise.bitsybitshareswallet.entities.Balance

class BalanceRepository internal constructor(context: Context) {

    private val mBalanceDao: BalanceDao

    init {
        val db = BitsyDatabase.getDatabase(context)
        mBalanceDao = db!!.balanceDao()
    }

    fun insertAll(balances: List<Balance>) {
        insertAllAsyncTask(mBalanceDao).execute(balances)
    }

    fun getAll(): LiveData<List<Balance>> {
        return mBalanceDao.getAll()
    }

    private class insertAllAsyncTask internal constructor(private val mAsyncTaskDao: BalanceDao) :
        AsyncTask<List<Balance>, Void, Void>() {

        override fun doInBackground(vararg transfers: List<Balance>): Void? {
            mAsyncTaskDao.insertAll(transfers[0])
            return null
        }
    }
}