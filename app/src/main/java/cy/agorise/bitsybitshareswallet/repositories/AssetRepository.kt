package cy.agorise.bitsybitshareswallet.repositories

import android.content.Context
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.daos.AssetDao
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.database.entities.Asset

class AssetRepository internal constructor(context: Context) {

    private val mAssetDao: AssetDao

    init {
        val db = BitsyDatabase.getDatabase(context)
        mAssetDao = db!!.assetDao()
    }

    fun getAll(): LiveData<List<Asset>> {
        return mAssetDao.getAll()
    }

    fun insertAll(assets: List<Asset>) {
        insertAllAsyncTask(mAssetDao).execute(assets)
    }

    private class insertAllAsyncTask internal constructor(private val mAsyncTaskDao: AssetDao) :
        AsyncTask<List<Asset>, Void, Void>() {

        override fun doInBackground(vararg assets: List<Asset>): Void? {
            mAsyncTaskDao.insertAll(assets[0])
            return null
        }
    }
}