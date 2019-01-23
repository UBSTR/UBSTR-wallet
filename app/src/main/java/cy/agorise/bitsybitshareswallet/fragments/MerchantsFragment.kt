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

import cy.agorise.bitsybitshareswallet.R
import android.preference.PreferenceManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import cy.agorise.bitsybitshareswallet.database.entities.Merchant
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.MerchantMarkerRenderer
import cy.agorise.bitsybitshareswallet.utils.toast
import cy.agorise.bitsybitshareswallet.viewmodels.MerchantViewModel
import java.lang.Exception


class MerchantsFragment : Fragment(), OnMapReadyCallback,
            ClusterManager.OnClusterClickListener<Merchant>,
            ClusterManager.OnClusterItemClickListener<Merchant>,
            ClusterManager.OnClusterItemInfoWindowClickListener<Merchant>{

    companion object {
        private const val TAG = "MerchantsFragment"

        // Camera Permission
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    private lateinit var mMap: GoogleMap

    private lateinit var mMerchantViewModel: MerchantViewModel

    private var mClusterManager: ClusterManager<Merchant>? = null

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

        mMerchantViewModel = ViewModelProviders.of(this).get(MerchantViewModel::class.java)
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
        val renderer = MerchantMarkerRenderer(context, mMap, mClusterManager)
        mClusterManager?.renderer = renderer

        // Point the map's listeners at the listeners implemented by the cluster manager.
        mMap.setOnCameraIdleListener(mClusterManager)
        mMap.setOnMarkerClickListener(mClusterManager)

        mMap.setOnMarkerClickListener(mClusterManager)
        mMap.setInfoWindowAdapter(mClusterManager?.markerManager)
        mMap.setOnInfoWindowClickListener(mClusterManager)
        mClusterManager?.setOnClusterClickListener(this)
        mClusterManager?.setOnClusterItemClickListener(this)
        mClusterManager?.setOnClusterItemInfoWindowClickListener(this)

        mMerchantViewModel.getAllMerchants().observe(this, Observer<List<Merchant>> {merchants ->
            mClusterManager?.clearItems()
            mClusterManager?.addItems(merchants)
            mClusterManager?.cluster()
        })
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

    /**
     * Animates the camera update to focus on an area that shows all the items from the cluster that was tapped.
     */
    override fun onClusterClick(cluster: Cluster<Merchant>?): Boolean {
        val builder = LatLngBounds.builder()
        val merchantMarkers = cluster?.items

        if (merchantMarkers != null) {
            for (item in merchantMarkers) {
                val merchantPosition = item.position
                builder.include(merchantPosition)
            }

            val bounds = builder.build()

            try {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (e: Exception) {
                Log.d(TAG, e.message)
            }
        }

        return true
    }

    override fun onClusterItemClick(p0: Merchant?): Boolean {
        return false
    }

    override fun onClusterItemInfoWindowClick(p0: Merchant?) {
    }
}
