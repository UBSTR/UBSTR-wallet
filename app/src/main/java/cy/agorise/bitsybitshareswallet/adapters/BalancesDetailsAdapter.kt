package cy.agorise.bitsybitshareswallet.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import cy.agorise.bitsybitshareswallet.database.joins.BalanceDetail



class BalancesDetailsAdapter(context: Context, resource: Int, data: List<BalanceDetail>) :
    ArrayAdapter<BalanceDetail>(context, resource, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var cv = convertView

        if (cv == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            cv = inflater.inflate(android.R.layout.simple_spinner_item, parent, false)
        }

        val text: TextView = cv!!.findViewById(android.R.id.text1)

        val balanceDetail = getItem(position)
        text.text = balanceDetail!!.symbol

        return cv
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val v = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)
        val text: TextView = v.findViewById(android.R.id.text1)

        val balanceDetail = getItem(position)
        text.text = balanceDetail!!.symbol

        return v
    }
}