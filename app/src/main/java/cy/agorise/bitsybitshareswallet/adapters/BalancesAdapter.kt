package cy.agorise.bitsybitshareswallet.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.database.entities.Balance

class BalancesAdapter(private val context: Context) :
    RecyclerView.Adapter<BalancesAdapter.ViewHolder>() {

    private val mComparator =
        Comparator<Balance> { a, b -> a.assetId.compareTo(b.assetId) }

    private val mSortedList =
        SortedList<Balance>(Balance::class.java, object : SortedList.Callback<Balance>() {
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

            override fun compare(a: Balance, b: Balance): Int {
                return mComparator.compare(a, b)
            }

            override fun areContentsTheSame(oldItem: Balance, newItem: Balance): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(item1: Balance, item2: Balance): Boolean {
                return item1.assetId == item2.assetId
            }
        })

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBalance: TextView = itemView.findViewById(R.id.tvBalance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BalancesAdapter.ViewHolder {
        val inflater = LayoutInflater.from(context)

        val balanceView = inflater.inflate(R.layout.item_balance, parent, false)

        return ViewHolder(balanceView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val balance = mSortedList.get(position)

        val amount = "${balance.assetAmount} ${balance.assetId}"
        viewHolder.tvBalance.text = amount
    }

    fun add(balance: Balance) {
        mSortedList.add(balance)
    }

    fun remove(balance: Balance) {
        mSortedList.remove(balance)
    }

    fun add(balances: List<Balance>) {
        mSortedList.addAll(balances)
    }

    fun remove(balances: List<Balance>) {
        mSortedList.beginBatchedUpdates()
        for (balance in balances) {
            mSortedList.remove(balance)
        }
        mSortedList.endBatchedUpdates()
    }

    fun replaceAll(balances: List<Balance>) {
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
