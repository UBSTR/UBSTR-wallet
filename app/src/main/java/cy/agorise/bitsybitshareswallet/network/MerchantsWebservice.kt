package cy.agorise.bitsybitshareswallet.network

import cy.agorise.bitsybitshareswallet.database.entities.Merchant
import cy.agorise.bitsybitshareswallet.database.entities.Teller
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MerchantsWebservice {

    @GET("/api/v1/merchants")
    fun getMerchants(@Query(value = "\$skip") skip: Int): Call<FeathersResponse<Merchant>>

    @GET("api/v2/tellers")
    fun getTellers(@Query(value = "\$skip") skip: Int): Call<FeathersResponse<Teller>>
}