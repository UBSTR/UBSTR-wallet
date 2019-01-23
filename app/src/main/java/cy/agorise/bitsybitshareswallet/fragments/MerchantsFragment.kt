package cy.agorise.bitsybitshareswallet.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.gson.GsonBuilder

import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.network.MerchantsWebservice
import cy.agorise.bitsybitshareswallet.network.FeathersResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.preference.PreferenceManager
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.*
import com.google.maps.android.clustering.ClusterManager
import cy.agorise.bitsybitshareswallet.database.entities.Merchant
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.toast


class MerchantsFragment : Fragment(), OnMapReadyCallback, retrofit2.Callback<FeathersResponse<Merchant>> {

    companion object {
        private const val TAG = "MerchantsFragment"

        // Camera Permission
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    private lateinit var mMap: GoogleMap

    private var mClusterManager: ClusterManager<Merchant>? = null

    private lateinit var merchantIcon: BitmapDescriptor

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_merchants, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        merchantIcon = getMarkerIconFromDrawable(resources.getDrawable(R.drawable.ic_pin_merchants, null))
    }

    private fun verifyLocationPermission() {
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not already granted
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
        } else {
            // Permission is already granted
            mMap.isMyLocationEnabled = true
        }
    }

    /** Handles the result from the location permission request */
    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                mMap.isMyLocationEnabled = true
            } else {
                context?.toast(getString(R.string.msg__location_permission_necessary))
            }
            return
        }
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

        applyMapTheme()

        verifyLocationPermission()

        // Setup clusters to group markers when possible
        mClusterManager = ClusterManager(context, mMap)

        // Point the map's listeners at the listeners implemented by the cluster manager.
        mMap.setOnCameraIdleListener(mClusterManager)
        mMap.setOnMarkerClickListener(mClusterManager)

        val gson = GsonBuilder()
            .setLenient()
            .create()
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.MERCHANTS_WEBSERVICE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val ambassadorService = retrofit.create<MerchantsWebservice>(MerchantsWebservice::class.java)
        val call = ambassadorService.getMerchants(0)
        call.enqueue(this)
    }

    private fun applyMapTheme() {
        val nightMode = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, false)

        if (nightMode) {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context, R.raw.map_style_night
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        }
    }

    override fun onResponse(call: Call<FeathersResponse<Merchant>>, response: Response<FeathersResponse<Merchant>>) {
        if (response.isSuccessful) {
            val res: FeathersResponse<Merchant>? = response.body()
            val merchants = res?.data ?: return
            mClusterManager?.addItems(merchants)
        } else {
            Log.e("error_bitsy", response.errorBody()?.string())
        }
    }

    override fun onFailure(call: Call<FeathersResponse<Merchant>>, t: Throwable) { /* Do nothing */ }

    private fun getMarkerIconFromDrawable(drawable: Drawable): BitmapDescriptor {
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
