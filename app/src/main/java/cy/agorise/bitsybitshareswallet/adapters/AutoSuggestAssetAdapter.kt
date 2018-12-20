package cy.agorise.bitsybitshareswallet.adapters

import android.content.Context
import android.widget.ArrayAdapter
import cy.agorise.bitsybitshareswallet.database.entities.Asset

class AutoSuggestAssetAdapter(context: Context, resource: Int):
    ArrayAdapter<Asset>(context, resource) {

    private var mAssets = ArrayList<Asset>()

    fun setData(assets: List<Asset>) {
        mAssets.clear()
        mAssets.addAll(assets)
    }

    override fun getCount(): Int {
        return mAssets.size
    }

    override fun getItem(position: Int): Asset? {
        return mAssets[position]
    }
}