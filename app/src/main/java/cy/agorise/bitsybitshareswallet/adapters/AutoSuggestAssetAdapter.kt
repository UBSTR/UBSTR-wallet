package cy.agorise.bitsybitshareswallet.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import cy.agorise.graphenej.Asset

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

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var cv = convertView

        if (cv == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            cv = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)
        }

        val text: TextView = cv!!.findViewById(android.R.id.text1)

        val asset = getItem(position)
        text.text = asset!!.symbol

        return cv
    }
}