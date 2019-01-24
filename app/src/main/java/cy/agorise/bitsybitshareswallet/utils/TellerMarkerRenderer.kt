package cy.agorise.bitsybitshareswallet.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.SparseArray
import android.view.ViewGroup
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.ui.IconGenerator
import com.google.maps.android.ui.SquareTextView
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.database.entities.Teller

/**
 * This class is used to create custom merchant and merchant cluster icons to show on the map.
 */
class TellerMarkerRenderer(val context: Context?, map: GoogleMap?, clusterManager: ClusterManager<Teller>?) :
    DefaultClusterRenderer<Teller>(context, map, clusterManager) {

    // Icons used to display merchants and merchants' clusters on the map
    private var tellerIcon: BitmapDescriptor

    private val mIcons = SparseArray<BitmapDescriptor>()

    private val mDensity: Float
    private val mIconGenerator = IconGenerator(context)

    init {
        tellerIcon = getMarkerIconFromDrawable(
            context?.resources?.getDrawable(R.drawable.ic_teller_pin, null))

        mDensity = context?.resources?.displayMetrics?.density ?: 2.0F

        this.mIconGenerator.setContentView(this.makeSquareTextView(context))
        this.mIconGenerator.setTextAppearance(com.google.maps.android.R.style.amu_ClusterIcon_TextAppearance)
        this.mIconGenerator.setBackground(context?.resources?.getDrawable(R.drawable.ic_teller_cluster, null))
    }

    override fun onBeforeClusterItemRendered(item: Teller?, markerOptions: MarkerOptions?) {
        markerOptions?.icon(tellerIcon)
    }

    override fun onBeforeClusterRendered(cluster: Cluster<Teller>?, markerOptions: MarkerOptions?) {
        val bucket = getBucket(cluster)
        var descriptor: BitmapDescriptor? = mIcons.get(bucket)
        if (descriptor == null) {
            descriptor = BitmapDescriptorFactory.fromBitmap(mIconGenerator.makeIcon(getClusterText(bucket)))
            mIcons.put(bucket, descriptor)
        }
        markerOptions?.icon(descriptor)
    }

    override fun shouldRenderAsCluster(cluster: Cluster<Teller>?): Boolean {
        return (cluster?.size ?: 0) > 1
    }

    private fun makeSquareTextView(context: Context?): SquareTextView {
        val squareTextView = SquareTextView(context)
        val layoutParams = ViewGroup.LayoutParams(-2, -2)
        squareTextView.layoutParams = layoutParams
        squareTextView.id = com.google.maps.android.R.id.amu_text
        val padding = (24.0f * this.mDensity).toInt()
        squareTextView.setPadding(padding, padding, padding, padding)
        return squareTextView
    }

    private fun getMarkerIconFromDrawable(drawable: Drawable?): BitmapDescriptor {
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(drawable?.intrinsicWidth ?: 24,
            drawable?.intrinsicHeight ?: 24, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(bitmap)
        drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable?.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}