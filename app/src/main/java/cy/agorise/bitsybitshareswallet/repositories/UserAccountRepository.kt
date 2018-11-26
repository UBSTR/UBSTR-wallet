package cy.agorise.bitsybitshareswallet.repositories

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.daos.BitsyDatabase
import cy.agorise.bitsybitshareswallet.daos.UserAccountDao
import cy.agorise.bitsybitshareswallet.models.UserAccount

class UserAccountRepository internal constructor(application: Application) {

    private val mUserAccountDao: UserAccountDao

    init {
        val db = BitsyDatabase.getDatabase(application)
        mUserAccountDao = db!!.userAccountDao()
    }

    fun insert(userAccount: UserAccount) {
        insertAsyncTask(mUserAccountDao).execute(userAccount)
    }

    fun getUserAccount(id: String): LiveData<UserAccount> {
        return mUserAccountDao.getUserAccount(id)
    }

    private class insertAsyncTask internal constructor(private val mAsyncTaskDao: UserAccountDao) :
        AsyncTask<UserAccount, Void, Void>() {

        override fun doInBackground(vararg userAccounts: UserAccount): Void? {
            mAsyncTaskDao.insert(userAccounts[0])
            return null
        }
    }
}