package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.GsonBuilder

import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.models.Merchant
import cy.agorise.bitsybitshareswallet.network.AmbassadorService
import cy.agorise.bitsybitshareswallet.network.FeathersResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class MerchantsFragment : Fragment(), OnMapReadyCallback, retrofit2.Callback<FeathersResponse<Merchant>> {

    private lateinit var mMap: GoogleMap

    private var location: LatLng? = null
    private var merchants: List<Merchant>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_merchants, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // TODO https://github.com/Agorise/bitsy-wallet/blob/feat_merchants/app/src/main/java/cy/agorise/bitsybitshareswallet/fragments/MapFragment.kt
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

//        val gson = GsonBuilder()
//            .setLenient()
//            .create()
//        val retrofit = Retrofit.Builder()
//            .baseUrl("https://intranet.palmpay.io/")
//            .addConverterFactory(GsonConverterFactory.create(gson))
//            .build()
//
//        val ambassadorService = retrofit.create<AmbassadorService>(AmbassadorService::class.java)
//        val call = ambassadorService.allMerchants
//        call.enqueue(this)
    }

    override fun onResponse(call: Call<FeathersResponse<Merchant>>, response: Response<FeathersResponse<Merchant>>) {
        if (response.isSuccessful) {
            val res: FeathersResponse<Merchant>? = response.body()
            merchants = res!!.data
            for (mer in merchants!!) {
                location = LatLng(mer.lat.toDouble(), mer.lon.toDouble())
                mMap.addMarker(
                    MarkerOptions().position(location!!).title(mer.name).snippet(mer.address).icon(
                        BitmapDescriptorFactory.fromResource(R.drawable.ic_merchants)
                    )
                )
            }
        } else {
            try {
                Log.e("error_bitsy", response.errorBody()?.string())
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    override fun onFailure(call: Call<FeathersResponse<Merchant>>, t: Throwable) {

    }

}
