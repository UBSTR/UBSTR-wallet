package cy.agorise.bitsybitshareswallet.fragments

import android.graphics.Point
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.adapters.TransfersDetailsAdapter
import cy.agorise.bitsybitshareswallet.database.joins.TransferDetail
import cy.agorise.bitsybitshareswallet.utils.BounceTouchListener
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.viewmodels.TransferDetailViewModel
import kotlinx.android.synthetic.main.fragment_transactions.*

class TransactionsFragment : Fragment() {

    private lateinit var mTransferDetailViewModel: TransferDetailViewModel

    /** Variables used for the RecyclerView pull springy animation  */
    private var bounceTouchListener: BounceTouchListener? = null
    private var pivotY1: Float = 0.toFloat()
    private var pivotY2:Float = 0.toFloat()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "") ?: ""

        // Configure TransferDetailViewModel to show the transaction history
        mTransferDetailViewModel = ViewModelProviders.of(this).get(TransferDetailViewModel::class.java)

        val transfersDetailsAdapter = TransfersDetailsAdapter(context!!)
        rvTransactions.adapter = transfersDetailsAdapter
        rvTransactions.layoutManager = LinearLayoutManager(context)

        mTransferDetailViewModel.getAll(userId).observe(this, Observer<List<TransferDetail>> { transfersDetails ->
            transfersDetailsAdapter.replaceAll(transfersDetails)
        })

        rvTransactions.pivotX = getScreenWidth(activity) * 0.5f

        pivotY1 = 0f
        pivotY2 = getScreenHeight(activity) * .5f

        bounceTouchListener =
                BounceTouchListener.create(rvTransactions, object : BounceTouchListener.OnTranslateListener {
                    override fun onTranslate(translation: Float) {
                        if (translation > 0) {
                            bounceTouchListener?.setMaxAbsTranslation(-99)
                            rvTransactions.pivotY = pivotY1
                            val scale = 2 * translation / rvTransactions.measuredHeight + 1
                            rvTransactions.scaleY = Math.pow(scale.toDouble(), .6).toFloat()
                        } else {
                            bounceTouchListener?.setMaxAbsTranslation((pivotY2 * .33f).toInt())
                            rvTransactions.pivotY = pivotY2
                            val scale = 2 * translation / rvTransactions.measuredHeight + 1
                            rvTransactions.scaleY = Math.pow(scale.toDouble(), .5).toFloat()
                        }
                    }
                })

        // Sets custom touch listener to handle bounce/stretch effect
        rvTransactions.setOnTouchListener(bounceTouchListener)
    }

    private fun getScreenWidth(activity: FragmentActivity?): Int {
        val display = activity?.windowManager?.defaultDisplay
        val size = Point()
        display?.getSize(size)
        return size.x
    }

    private fun getScreenHeight(activity: FragmentActivity?): Int {
        val display = activity?.windowManager?.defaultDisplay
        val size = Point()
        display?.getSize(size)
        return size.y
    }

//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        inflater.inflate(R.menu.menu_home, menu)
//    }
}
