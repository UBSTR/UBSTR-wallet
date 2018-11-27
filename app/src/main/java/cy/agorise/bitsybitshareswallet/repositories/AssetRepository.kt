package cy.agorise.bitsybitshareswallet.repositories

import android.app.Application
import android.os.AsyncTask
import cy.agorise.bitsybitshareswallet.daos.AssetDao
import cy.agorise.bitsybitshareswallet.daos.BitsyDatabase
import cy.agorise.bitsybitshareswallet.entities.Asset

class AssetRepository internal constructor(application: Application) {

    private val mAssetDao: AssetDao

    init {
        val db = BitsyDatabase.getDatabase(application)
        mAssetDao = db!!.assetDao()
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