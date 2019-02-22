package cy.agorise.bitsybitshareswallet.repositories

import android.content.Context
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.daos.AuthorityDao
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.database.entities.Authority
import io.reactivex.Single

class AuthorityRepository internal constructor(context: Context) {

    private val mAuthorityDao: AuthorityDao

    init {
        val db = BitsyDatabase.getDatabase(context)
        mAuthorityDao = db!!.authorityDao()
    }

    fun insert(authority: Authority) {
        insertAsyncTask(mAuthorityDao).execute(authority)
    }

    fun get(userId: String): Single<Authority> {
        return mAuthorityDao.get(userId)
    }

    fun getWIFOld(userId: String, authorityType: Int): Single<String> {
        return mAuthorityDao.getWIFOld(userId, authorityType)
    }

    fun getWIF(userId: String, authorityType: Int): LiveData<String> {
        return mAuthorityDao.getWIF(userId, authorityType)
    }

    private class insertAsyncTask internal constructor(private val mAsyncTaskDao: AuthorityDao) :
        AsyncTask<Authority, Void, Void>() {

        override fun doInBackground(vararg authorities: Authority): Void? {
            mAsyncTaskDao.insert(authorities[0])
            return null
        }
    }
}