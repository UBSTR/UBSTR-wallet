package cy.agorise.bitsybitshareswallet.repositories

import android.content.Context
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.database.daos.TransferDao
import cy.agorise.bitsybitshareswallet.database.entities.Transfer
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

    fun setBlockTime(blockNumber: Long, timestamp: Long) {
        setBlockTimeAsyncTask(mTransferDao).execute(Pair(blockNumber, timestamp))
    }

    fun getAll(): LiveData<List<Transfer>> {
        return mTransferDao.getAll()
    }

    fun getCount(): Single<Int> {
        return mTransferDao.getCount()
    }

    fun getTransferBlockNumberWithMissingTime(): LiveData<Long> {
        return mTransferDao.getTransferBlockNumberWithMissingTime()
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

    private class setBlockTimeAsyncTask internal constructor(private val mAsyncTaskDao: TransferDao) :
        AsyncTask<Pair<Long, Long>, Void, Void>() {

        override fun doInBackground(vararg pair: Pair<Long, Long>): Void? {
            mAsyncTaskDao.setBlockTime(pair[0].first, pair[0].second)
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