package cy.agorise.bitsybitshareswallet.adapters

import android.content.Context
import android.preference.PreferenceManager
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
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetail
import cy.agorise.bitsybitshareswallet.utils.Constants

class TransfersAdapter(private val context: Context) :
    RecyclerView.Adapter<TransfersAdapter.ViewHolder>() {

    val userId = PreferenceManager.getDefaultSharedPreferences(context)
        .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "")!!

    private val mComparator =
        Comparator<TransferDetail> { a, b -> a.id.compareTo(b.id) }

    private val mSortedList =
        SortedList<TransferDetail>(TransferDetail::class.java, object : SortedList.Callback<TransferDetail>() {
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

            override fun compare(a: TransferDetail, b: TransferDetail): Int {
                return mComparator.compare(a, b)
            }

            override fun areContentsTheSame(oldItem: TransferDetail, newItem: TransferDetail): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(item1: TransferDetail, item2: TransferDetail): Boolean {
                return item1.id == item2.id
            }
        })

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rootView: ConstraintLayout  = itemView.findViewById(R.id.rootView)
        val vPaymentDirection: View     = itemView.findViewById(R.id.vPaymentDirection)
        val tvFrom: TextView            = itemView.findViewById(R.id.tvFrom)
        val ivDirectionArrow: ImageView = itemView.findViewById(R.id.ivDirectionArrow)
        val tvTo: TextView              = itemView.findViewById(R.id.tvTo)
        val llMemo: LinearLayout        = itemView.findViewById(R.id.llMemo)
        val tvMemo: TextView            = itemView.findViewById(R.id.tvMemo)
        val tvDate: TextView            = itemView.findViewById(R.id.tvDate)
        val tvTime: TextView            = itemView.findViewById(R.id.tvTime)
        val tvCryptoAmount: TextView    = itemView.findViewById(R.id.tvCryptoAmount)
        val tvFiatEquivalent: TextView  = itemView.findViewById(R.id.tvFiatEquivalent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransfersAdapter.ViewHolder {
        val inflater = LayoutInflater.from(context)

        val transactionView = inflater.inflate(R.layout.item_transaction, parent, false)

        return ViewHolder(transactionView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val transferDetail = mSortedList.get(position)

        viewHolder.vPaymentDirection.setBackgroundColor(context.resources.getColor(
            if(transferDetail.direction) R.color.colorReceive else R.color.colorSend
        ))

        viewHolder.tvFrom.text = transferDetail.from
        viewHolder.tvTo.text = transferDetail.to

        viewHolder.ivDirectionArrow.setImageDrawable(context.getDrawable(
            if(transferDetail.direction) R.drawable.ic_arrow_receive else R.drawable.ic_arrow_send
        ))
    }

    fun add(transferDetail: TransferDetail) {
        mSortedList.add(transferDetail)
    }

    fun remove(transferDetail: TransferDetail) {
        mSortedList.remove(transferDetail)
    }

    fun add(transfersDetails: List<TransferDetail>) {
        mSortedList.addAll(transfersDetails)
    }

    fun remove(transfersDetails: List<TransferDetail>) {
        mSortedList.beginBatchedUpdates()
        for (transferDetail in transfersDetails) {
            mSortedList.remove(transferDetail)
        }
        mSortedList.endBatchedUpdates()
    }

    fun replaceAll(transfersDetails: List<TransferDetail>) {
        mSortedList.beginBatchedUpdates()
        for (i in mSortedList.size() - 1 downTo 0) {
            val transferDetail = mSortedList.get(i)
            if (!transfersDetails.contains(transferDetail)) {
                mSortedList.remove(transferDetail)
            }
        }
        mSortedList.addAll(transfersDetails)
        mSortedList.endBatchedUpdates()
    }

    override fun getItemCount(): Int {
        return mSortedList.size()
    }
}
