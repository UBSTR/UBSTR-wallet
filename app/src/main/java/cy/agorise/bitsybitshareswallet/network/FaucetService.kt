package cy.agorise.bitsybitshareswallet.network

import cy.agorise.bitsybitshareswallet.models.FaucetRequest
import cy.agorise.bitsybitshareswallet.models.FaucetResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Interface to the faucet service. The faucet is used in order to register new BitShares accounts.
 */
interface FaucetService {

    @GET("/")
    fun checkStatus(): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("/api/v1/accounts")
    fun registerPrivateAccount(@Body faucetRequest: FaucetRequest): Call<FaucetResponse>
}