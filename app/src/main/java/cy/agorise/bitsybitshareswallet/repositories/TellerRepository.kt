package cy.agorise.bitsybitshareswallet.repositories

import android.content.Context
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.util.Log
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.database.daos.TellerDao
import cy.agorise.bitsybitshareswallet.database.entities.Teller
import cy.agorise.bitsybitshareswallet.network.FeathersResponse
import cy.agorise.bitsybitshareswallet.network.MerchantsWebservice
import cy.agorise.bitsybitshareswallet.utils.Constants
import io.reactivex.Single
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TellerRepository internal constructor(val context: Context) : retrofit2.Callback<FeathersResponse<Teller>> {

    companion object {
        private const val TAG = "TellerRepository"
    }

    private val mTellerDao: TellerDao

    init {
        val db = BitsyDatabase.getDatabase(context)
        mTellerDao = db!!.tellerDao()
    }

    /** Returns a LiveData object directly from the database while the response from the WebService is obtained. */
    fun getAll(): LiveData<List<Teller>> {
        refreshTellers()
        return mTellerDao.getAll()
    }

    /** Refreshes the tellers information only if the MERCHANT_UPDATE_PERIOD has passed, otherwise it does nothing */
    private fun refreshTellers() {
        val lastTellerUpdate = PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(Constants.KEY_TELLERS_LAST_UPDATE, 0)

        val now = System.currentTimeMillis()

        if (lastTellerUpdate + Constants.MERCHANTS_UPDATE_PERIOD < now) {
            Log.d(TAG, "Updating tellers from webservice")
            // TODO make sure it works when there are more tellers than those sent back in the first response
            val retrofit = Retrofit.Builder()
                .baseUrl(Constants.MERCHANTS_WEBSERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val ambassadorService = retrofit.create<MerchantsWebservice>(MerchantsWebservice::class.java)
            val call = ambassadorService.getTellers(0)
            call.enqueue(this)
        }
    }

    override fun onResponse(call: Call<FeathersResponse<Teller>>, response: Response<FeathersResponse<Teller>>) {
        if (response.isSuccessful) {
            val res: FeathersResponse<Teller>? = response.body()
            val tellers = res?.data ?: return
            insertAllAsyncTask(mTellerDao).execute(tellers)

            val now = System.currentTimeMillis()
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putLong(Constants.KEY_TELLERS_LAST_UPDATE, now).apply()
        }
    }

    override fun onFailure(call: Call<FeathersResponse<Teller>>, t: Throwable) { /* Do nothing */ }

    private class insertAllAsyncTask internal constructor(private val mAsyncTaskDao: TellerDao) :
        AsyncTask<List<Teller>, Void, Void>() {

        override fun doInBackground(vararg tellers: List<Teller>): Void? {
            mAsyncTaskDao.deleteAll()
            mAsyncTaskDao.insertAll(tellers[0])
            return null
        }
    }

    fun findTellerByWord(word: String): Single<List<Teller>> {
        return mTellerDao.findTellersByWord("%$word%")
    }
}