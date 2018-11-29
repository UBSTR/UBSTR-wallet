package cy.agorise.bitsybitshareswallet.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.entities.Transfer

class TransactionsAdapter(private val context: Context, private val mComparator: Comparator<Transfer>) :
    RecyclerView.Adapter<TransactionsAdapter.ViewHolder>() {

    private val mSortedList =
        SortedList<Transfer>(Transfer::class.java, object : SortedList.Callback<Transfer>() {
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

            override fun compare(a: Transfer, b: Transfer): Int {
                return mComparator.compare(a, b)
            }

            override fun areContentsTheSame(oldItem: Transfer, newItem: Transfer): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(item1: Transfer, item2: Transfer): Boolean {
                return item1.id == item2.id
            }
        })

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rootView            = itemView.findViewById<ConstraintLayout>(R.id.rootView)
        val vPaymentDirection   = itemView.findViewById<View>(R.id.vPaymentDirection)
        val tvFrom              = itemView.findViewById<TextView>(R.id.tvFrom)
        val ivDirectionArrow    = itemView.findViewById<ImageView>(R.id.ivDirectionArrow)
        val tvTo                = itemView.findViewById<TextView>(R.id.tvTo)
        val llMemo              = itemView.findViewById<LinearLayout>(R.id.llMemo)
        val tvMemo              = itemView.findViewById<TextView>(R.id.tvMemo)
        val tvDate              = itemView.findViewById<TextView>(R.id.tvDate)
        val tvTime              = itemView.findViewById<TextView>(R.id.tvTime)
        val tvCryptoAmount      = itemView.findViewById<TextView>(R.id.tvCryptoAmount)
        val tvFiatEquivalent    = itemView.findViewById<TextView>(R.id.tvFiatEquivalent)
    }

    init {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionsAdapter.ViewHolder {
        val inflater = LayoutInflater.from(context)

        val transactionView = inflater.inflate(R.layout.item_transaction, parent, false)

        return ViewHolder(transactionView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val transaction = mSortedList.get(position)

        viewHolder.tvFrom.text = transaction.source
        viewHolder.tvTo.text = transaction.destination
    }

    fun add(transaction: Transfer) {
        mSortedList.add(transaction)
    }

    fun remove(transaction: Transfer) {
        mSortedList.remove(transaction)
    }

    fun add(transactions: List<Transfer>) {
        mSortedList.addAll(transactions)
    }

    fun remove(transactions: List<Transfer>) {
        mSortedList.beginBatchedUpdates()
        for (transaction in transactions) {
            mSortedList.remove(transaction)
        }
        mSortedList.endBatchedUpdates()
    }

    fun replaceAll(transactions: List<Transfer>) {
        mSortedList.beginBatchedUpdates()
        for (i in mSortedList.size() - 1 downTo 0) {
            val transaction = mSortedList.get(i)
            if (!transactions.contains(transaction)) {
                mSortedList.remove(transaction)
            }
        }
        mSortedList.addAll(transactions)
        mSortedList.endBatchedUpdates()
    }

    override fun getItemCount(): Int {
        return mSortedList.size()
    }
}
