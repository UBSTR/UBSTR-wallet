package cy.agorise.bitsybitshareswallet.repositories

import android.app.Application
import android.os.AsyncTask
import cy.agorise.bitsybitshareswallet.daos.AuthorityDao
import cy.agorise.bitsybitshareswallet.daos.BitsyDatabase
import cy.agorise.bitsybitshareswallet.models.Authority

class AuthorityRepository internal constructor(application: Application) {

    private val mAuthorityDao: AuthorityDao

    init {
        val db = BitsyDatabase.getDatabase(application)
        mAuthorityDao = db!!.authorityDao()
    }

    fun insert(authority: Authority) {
        insertAsyncTask(mAuthorityDao).execute(authority)
    }

    private class insertAsyncTask internal constructor(private val mAsyncTaskDao: AuthorityDao) :
        AsyncTask<Authority, Void, Void>() {

        override fun doInBackground(vararg authorities: Authority): Void? {
            mAsyncTaskDao.insert(authorities[0])
            return null
        }
    }
}