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
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.MarkerManager
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import cy.agorise.bitsybitshareswallet.database.entities.Merchant
import cy.agorise.bitsybitshareswallet.database.entities.Teller
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.MerchantClusterRenderer
import cy.agorise.bitsybitshareswallet.utils.TellerClusterRenderer
import cy.agorise.bitsybitshareswallet.utils.toast
import cy.agorise.bitsybitshareswallet.viewmodels.MerchantViewModel
import java.lang.Exception


class MerchantsFragment : Fragment(), OnMapReadyCallback {

    companion object {
        private const val TAG = "MerchantsFragment"

        // Camera Permission
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    private lateinit var mMap: GoogleMap

    private lateinit var mMerchantViewModel: MerchantViewModel

    private var mMarkerManager: MarkerManager? = null

    private var mMerchantClusterManager: ClusterManager<Merchant>? = null
    private var mTellerClusterManager: ClusterManager<Teller>? = null

    private var selectedMerchant: Merchant? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_merchants, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mMerchantViewModel = ViewModelProviders.of(this).get(MerchantViewModel::class.java)
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

        mMarkerManager = MarkerManager(mMap)

        initMerchantsCluster()

        initTellersCluster()

        // Point the map's listeners at the listeners implemented by the cluster manager.
        mMap.setOnMarkerClickListener(mMarkerManager)

        mMap.setOnCameraIdleListener {
            mMerchantClusterManager?.onCameraIdle()
            mTellerClusterManager?.onCameraIdle()
        }

        mMap.setInfoWindowAdapter(mMarkerManager)
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

    private fun initMerchantsCluster() {
        // Setup clusters to group markers when possible
        mMerchantClusterManager = ClusterManager(context, mMap, mMarkerManager)
        val merchantRenderer = MerchantClusterRenderer(context, mMap, mMerchantClusterManager)
        mMerchantClusterManager?.renderer = merchantRenderer

        mMerchantClusterManager?.setOnClusterClickListener { onClusterClick(it as Cluster<ClusterItem>) }
        mMerchantClusterManager?.setOnClusterItemClickListener { merchant ->
            selectedMerchant = merchant
            false
        }

        mMerchantClusterManager?.markerCollection?.setOnInfoWindowAdapter(MerchantInfoWindowAdapter())

        mMerchantViewModel.getAllMerchants().observe(this, Observer<List<Merchant>> {merchants ->
            mMerchantClusterManager?.clearItems()
            mMerchantClusterManager?.addItems(merchants)
            mMerchantClusterManager?.cluster()
        })
    }

    private fun initTellersCluster() {
        // Setup clusters to group markers when possible
        mTellerClusterManager = ClusterManager(context, mMap, mMarkerManager)
        val tellerRenderer = TellerClusterRenderer(context, mMap, mTellerClusterManager)
        mTellerClusterManager?.renderer = tellerRenderer

        mTellerClusterManager?.setOnClusterClickListener { onClusterClick(it as Cluster<ClusterItem>) }
        mTellerClusterManager?.setOnClusterItemClickListener { false }

        mMerchantViewModel.getAllTellers().observe(this, Observer<List<Teller>> {tellers ->
            mTellerClusterManager?.clearItems()
            mTellerClusterManager?.addItems(tellers)
            mTellerClusterManager?.cluster()
        })
    }

    /** Animates the camera update to focus on an area that shows all the items from the cluster that was tapped. */
    private fun onClusterClick(cluster: Cluster<ClusterItem>?): Boolean {
        val builder = LatLngBounds.builder()
        val items = cluster?.items

        if (items != null) {
            for (item in items) {
                val position = item.position
                builder.include(position)
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

    inner class MerchantInfoWindowAdapter : GoogleMap.InfoWindowAdapter {

        override fun getInfoWindow(marker: Marker?): View {
            val infoWindowLayout: View = LayoutInflater.from(context).inflate(
                R.layout.map_pin_info_dialog, null)
            val tvName      = infoWindowLayout.findViewById<TextView>(R.id.tvName)
            val tvAddress   = infoWindowLayout.findViewById<TextView>(R.id.tvAddress)
            val tvPhone     = infoWindowLayout.findViewById<TextView>(R.id.tvPhone)
            val tvTelegram  = infoWindowLayout.findViewById<TextView>(R.id.tvTelegram)
            val tvWebsite   = infoWindowLayout.findViewById<TextView>(R.id.tvWebsite)

            if (selectedMerchant != null) {
                tvName.text = selectedMerchant?.name

                if (selectedMerchant?.name != null)
                    tvAddress.text = selectedMerchant?.address
                else
                    tvAddress.visibility = View.GONE

                if (selectedMerchant?.phone != null)
                    tvPhone.text = selectedMerchant?.phone
                else
                    tvPhone.visibility = View.GONE

                if (selectedMerchant?.telegram != null)
                    tvTelegram.text = selectedMerchant?.telegram
                else
                    tvTelegram.visibility = View.GONE

                if (selectedMerchant?.website != null)
                    tvWebsite.text = selectedMerchant?.website
                        ?.removePrefix("http://")?.removePrefix("https://")
                else
                    tvWebsite.visibility = View.GONE

            }

            return infoWindowLayout
        }

        override fun getInfoContents(marker: Marker?): View? {
            return null
        }
    }


}
