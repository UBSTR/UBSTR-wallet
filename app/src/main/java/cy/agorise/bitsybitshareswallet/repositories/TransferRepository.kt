package cy.agorise.bitsybitshareswallet.repositories

import android.content.Context
import android.os.AsyncTask
import cy.agorise.bitsybitshareswallet.daos.BitsyDatabase
import cy.agorise.bitsybitshareswallet.daos.TransferDao
import cy.agorise.bitsybitshareswallet.entities.Transfer
import io.reactivex.Single

class TransferRepository internal constructor(context: Context) {

    private val mTransferDao: TransferDao

    init {
        val db = BitsyDatabase.getDatabase(context)
        mTransferDao = db!!.transferDao()
    }

    fun insertAll(transfers: List<Transfer>) {
        insertAllAsyncTask(mTransferDao).execute(transfers)
    }

    fun getCount(): Single<Int> {
        return mTransferDao.getCount()
    }

    fun deleteAll() {
        deleteAllAsyncTask(mTransferDao).execute()
    }

    private class insertAllAsyncTask internal constructor(private val mAsyncTaskDao: TransferDao) :
        AsyncTask<List<Transfer>, Void, Void>() {

        override fun doInBackground(vararg transfers: List<Transfer>): Void? {
            mAsyncTaskDao.insertAll(transfers[0])
            return null
        }
    }

    private class deleteAllAsyncTask internal constructor(private val mAsyncTaskDao: TransferDao) :
        AsyncTask<Void, Void, Void>() {

        override fun doInBackground(vararg params: Void?): Void? {
            mAsyncTaskDao.deleteAll()
            return null
        }
    }
}