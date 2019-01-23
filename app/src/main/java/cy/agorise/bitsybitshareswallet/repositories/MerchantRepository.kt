package cy.agorise.bitsybitshareswallet.repositories

import android.content.Context
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import cy.agorise.bitsybitshareswallet.database.BitsyDatabase
import cy.agorise.bitsybitshareswallet.database.daos.MerchantDao
import cy.agorise.bitsybitshareswallet.database.entities.Merchant
import cy.agorise.bitsybitshareswallet.network.FeathersResponse
import cy.agorise.bitsybitshareswallet.network.MerchantsWebservice
import cy.agorise.bitsybitshareswallet.utils.Constants
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MerchantRepository internal constructor(context: Context) : retrofit2.Callback<FeathersResponse<Merchant>> {

    private val mMerchantDao: MerchantDao

    init {
        val db = BitsyDatabase.getDatabase(context)
        mMerchantDao = db!!.merchantDao()
    }

    /**
     * Returns a LiveData object directly from the database while the response from the WebService is obtained.
     */
    fun getAll(): LiveData<List<Merchant>> {
        refreshMerchants()
        return mMerchantDao.getAll()
    }

    private fun refreshMerchants() {
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.MERCHANTS_WEBSERVICE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val ambassadorService = retrofit.create<MerchantsWebservice>(MerchantsWebservice::class.java)
        val call = ambassadorService.getMerchants(0)
        call.enqueue(this)
    }

    override fun onResponse(call: Call<FeathersResponse<Merchant>>, response: Response<FeathersResponse<Merchant>>) {
        if (response.isSuccessful) {
            val res: FeathersResponse<Merchant>? = response.body()
            val merchants = res?.data ?: return
            insertAllAsyncTask(mMerchantDao).execute(merchants)
        }
    }

    override fun onFailure(call: Call<FeathersResponse<Merchant>>, t: Throwable) { /* Do nothing */ }

    private class insertAllAsyncTask internal constructor(private val mAsyncTaskDao: MerchantDao) :
        AsyncTask<List<Merchant>, Void, Void>() {

        override fun doInBackground(vararg merchants: List<Merchant>): Void? {
            // TODO Delete all first
            mAsyncTaskDao.insertAll(merchants[0])
            return null
        }
    }
}