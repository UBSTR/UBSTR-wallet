package cy.agorise.bitsybitshareswallet.repositories

import android.content.Context
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.database.daos.UserAccountDao
import cy.agorise.bitsybitshareswallet.database.entities.UserAccount

class UserAccountRepository internal constructor(context: Context) {

    private val mUserAccountDao: UserAccountDao

    init {
        val db = BitsyDatabase.getDatabase(context)
        mUserAccountDao = db!!.userAccountDao()
    }

    fun insert(userAccount: UserAccount) {
        insertAsyncTask(mUserAccountDao).execute(userAccount)
    }

    fun insertAll(userAccounts: List<UserAccount>) {
        insertAllAsyncTask(mUserAccountDao).execute(userAccounts)
    }

    fun getUserAccount(id: String): LiveData<UserAccount> {
        return mUserAccountDao.getUserAccount(id)
    }

    fun getMissingUserAccountIds(): LiveData<List<String>> {
        return mUserAccountDao.getMissingAccountIds()
    }

    private class insertAsyncTask internal constructor(private val mAsyncTaskDao: UserAccountDao) :
        AsyncTask<UserAccount, Void, Void>() {

        override fun doInBackground(vararg userAccounts: UserAccount): Void? {
            mAsyncTaskDao.insert(userAccounts[0])
            return null
        }
    }

    private class insertAllAsyncTask internal constructor(private val mAsyncTaskDao: UserAccountDao) :
        AsyncTask<List<UserAccount>, Void, Void>() {

        override fun doInBackground(vararg userAccounts: List<UserAccount>): Void? {
            mAsyncTaskDao.insertAll(userAccounts[0])
            return null
        }
    }
}