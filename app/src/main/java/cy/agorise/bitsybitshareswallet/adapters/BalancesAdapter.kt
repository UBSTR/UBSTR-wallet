package cy.agorise.bitsybitshareswallet.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.database.joins.BalanceDetail

class BalancesAdapter(private val context: Context) :
    RecyclerView.Adapter<BalancesAdapter.ViewHolder>() {

    private val mComparator =
        Comparator<BalanceDetail> { a, b -> a.symbol.compareTo(b.symbol) }

    private val mSortedList =
        SortedList<BalanceDetail>(BalanceDetail::class.java, object : SortedList.Callback<BalanceDetail>() {
            override fun onInserted(position: Int, count: Int) {
                notifyItemRangeInserted(position, count)
            }

            override fun onRemoved(position: Int, count: Int) {
                notifyItemRangeRemoved(position, count)
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                notifyItemMoved(fromPosition, toPosition)
            }

            override fun onChanged(position: Int, count: Int) {
                notifyItemRangeChanged(position, count)
            }

            override fun compare(a: BalanceDetail, b: BalanceDetail): Int {
                return mComparator.compare(a, b)
            }

            override fun areContentsTheSame(oldItem: BalanceDetail, newItem: BalanceDetail): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(item1: BalanceDetail, item2: BalanceDetail): Boolean {
                return item1.id == item2.id
            }
        })

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSymbol: TextView = itemView.findViewById(R.id.tvSymbol)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BalancesAdapter.ViewHolder {
        val inflater = LayoutInflater.from(context)

        val balanceView = inflater.inflate(R.layout.item_balance, parent, false)

        return ViewHolder(balanceView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val balance = mSortedList.get(position)

        viewHolder.tvSymbol.text = balance.symbol

        val amount = balance.amount.toDouble() / Math.pow(10.0, balance.precision.toDouble())
        viewHolder.tvAmount.text = String.format("%." + Math.min(balance.precision, 8) + "f", amount)
    }

    fun add(balance: BalanceDetail) {
        mSortedList.add(balance)
    }

    fun remove(balance: BalanceDetail) {
        mSortedList.remove(balance)
    }

    fun add(balances: List<BalanceDetail>) {
        mSortedList.addAll(balances)
    }

    fun remove(balances: List<BalanceDetail>) {
        mSortedList.beginBatchedUpdates()
        for (balance in balances) {
            mSortedList.remove(balance)
        }
        mSortedList.endBatchedUpdates()
    }

    fun replaceAll(balances: List<BalanceDetail>) {
        mSortedList.beginBatchedUpdates()
        for (i in mSortedList.size() - 1 downTo 0) {
            val balance = mSortedList.get(i)
            if (!balances.contains(balance)) {
                mSortedList.remove(balance)
            }
        }
        mSortedList.addAll(balances)
        mSortedList.endBatchedUpdates()
    }

    override fun getItemCount(): Int {
        return mSortedList.size()
    }
}
