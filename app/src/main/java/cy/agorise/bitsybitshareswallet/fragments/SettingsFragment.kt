package cy.agorise.bitsybitshareswallet.fragments

import android.content.*
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.list.customListAdapter
import cy.agorise.bitsybitshareswallet.BuildConfig
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.adapters.FullNodesAdapter
import cy.agorise.bitsybitshareswallet.repositories.AuthorityRepository
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.CryptoUtils
import cy.agorise.graphenej.BrainKey
import cy.agorise.graphenej.api.android.NetworkService
import cy.agorise.graphenej.api.android.RxBus
import cy.agorise.graphenej.api.calls.GetDynamicGlobalProperties
import cy.agorise.graphenej.models.DynamicGlobalProperties
import cy.agorise.graphenej.models.JsonRpcResponse
import cy.agorise.graphenej.network.FullNode
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_settings.*
import java.text.NumberFormat

class SettingsFragment : Fragment(), ServiceConnection {
    private val TAG = this.javaClass.simpleName

    private var mDisposables = CompositeDisposable()

    /* Network service connection */
    private var mNetworkService: NetworkService? = null

    /** Flag used to keep track of the NetworkService binding state */
    private var mShouldUnbindNetwork: Boolean = false

    // Dialog displaying the list of nodes and their latencies
    private var mNodesDialog: MaterialDialog? = null

    /** Adapter that holds the FullNode list used in the Bitshares nodes modal */
    private var nodesAdapter: FullNodesAdapter? = null

    private val mHandler = Handler()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAutoCloseSwitch()

        initNightModeSwitch()

        btnViewBrainKey.setOnClickListener { getBrainkey(it) }

        tvNetworkStatus.setOnClickListener { v ->
            if (mNetworkService != null) {
                // PublishSubject used to announce full node latencies updates
                val fullNodePublishSubject = mNetworkService!!.nodeLatencyObservable
                fullNodePublishSubject?.observeOn(AndroidSchedulers.mainThread())?.subscribe(nodeLatencyObserver)

                val fullNodes = mNetworkService!!.nodes

                nodesAdapter = FullNodesAdapter(v.context)
                nodesAdapter!!.add(fullNodes)

                mNodesDialog = MaterialDialog(v.context)
                    .title(text = String.format("%s v%s", getString(R.string.app_name), BuildConfig.VERSION_NAME))
                    .message(text = getString(R.string.title__bitshares_nodes_dialog, "-------"))
                    .customListAdapter(nodesAdapter as FullNodesAdapter)
                    .negativeButton(android.R.string.ok) {
                        mHandler.removeCallbacks(mRequestDynamicGlobalPropertiesTask)
                    }

                mNodesDialog?.show()

                // Registering a recurrent task used to poll for dynamic global properties requests
                mHandler.post(mRequestDynamicGlobalPropertiesTask)
            }
        }

        // Connect to the RxBus, which receives events from the NetworkService
        mDisposables.add(
            RxBus.getBusInstance()
                .asFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { handleIncomingMessage(it) }
        )
    }

    /**
     * Observer used to be notified about node latency measurement updates.
     */
    private val nodeLatencyObserver = object : Observer<FullNode> {
        override fun onSubscribe(d: Disposable) {
            mDisposables.add(d)
        }

        override fun onNext(fullNode: FullNode) {
            if (!fullNode.isRemoved)
                nodesAdapter?.add(fullNode)
            else
                nodesAdapter?.remove(fullNode)
        }

        override fun onError(e: Throwable) {
            Log.e(TAG, "nodeLatencyObserver.onError.Msg: " + e.message)
        }

        override fun onComplete() {}
    }

    private fun handleIncomingMessage(message: Any?) {
        if (message is JsonRpcResponse<*>) {
            if (message.result is DynamicGlobalProperties) {
                val dynamicGlobalProperties = message.result as DynamicGlobalProperties
                if (mNodesDialog != null && mNodesDialog?.isShowing == true) {
                    val blockNumber = NumberFormat.getInstance().format(dynamicGlobalProperties.head_block_number)
                    mNodesDialog?.message(text = getString(R.string.title__bitshares_nodes_dialog, blockNumber))
                }
            }
        }
    }

    /**
     * Task used to obtain frequent updates on the global dynamic properties object
     */
    private val mRequestDynamicGlobalPropertiesTask = object : Runnable {
        override fun run() {
            if (mNetworkService != null) {
                if (mNetworkService?.isConnected == true) {
                    mNetworkService?.sendMessage(GetDynamicGlobalProperties(), GetDynamicGlobalProperties.REQUIRED_API)
                } else {
                    Log.d(TAG, "NetworkService exists but is not connected")
                }
            } else {
                Log.d(TAG, "NetworkService reference is null")
            }
            mHandler.postDelayed(this, Constants.BLOCK_PERIOD)
        }
    }

    /**
     * Fetches the relevant preference from the SharedPreferences and configures the corresponding switch accordingly,
     * and adds a listener to the said switch to store the preference in case the user changes it.
     */
    private fun initAutoCloseSwitch() {
        val autoCloseOn = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Constants.KEY_AUTO_CLOSE_ACTIVATED, true)

        switchAutoClose.isChecked = autoCloseOn

        switchAutoClose.setOnCheckedChangeListener { buttonView, isChecked ->
            PreferenceManager.getDefaultSharedPreferences(buttonView.context).edit()
                .putBoolean(Constants.KEY_AUTO_CLOSE_ACTIVATED, isChecked).apply()
        }
    }

    /**
     * Fetches the relevant preference from the SharedPreferences and configures the corresponding switch accordingly,
     * and adds a listener to the said switch to store the preference in case the user changes it. Also makes a call to
     * recreate the activity and apply the selected theme.
     */
    private fun initNightModeSwitch() {
        val nightModeOn = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, false)

        switchNightMode.isChecked = nightModeOn

        switchNightMode.setOnCheckedChangeListener { buttonView, isChecked ->

            PreferenceManager.getDefaultSharedPreferences(buttonView.context).edit()
                .putBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, isChecked).apply()

            // Recreates the activity to apply the selected theme
            activity?.recreate()
        }
    }

    /**
     * Obtains the brainKey from the authorities db table for the current user account and if it is not null it passes
     * the brainKey to a method to show it in a nice MaterialDialog
     */
    private fun getBrainkey(view: View) {
        val userId = PreferenceManager.getDefaultSharedPreferences(view.context)
            .getString(Constants.KEY_CURRENT_ACCOUNT_ID, "") ?: ""

        val authorityRepository = AuthorityRepository(view.context)

        mDisposables.add(authorityRepository.get(userId)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { authority ->
                if (authority != null) {
                    val plainBrainKey = CryptoUtils.decrypt(view.context, authority.encryptedBrainKey)
                    val plainSequenceNumber = CryptoUtils.decrypt(view.context, authority.encryptedSequenceNumber)
                    val sequenceNumber = Integer.parseInt(plainSequenceNumber)
                    val brainKey = BrainKey(plainBrainKey, sequenceNumber)
                    showBrainKeyDialog(view, brainKey)
                }
            }
        )
    }

    /**
     * Shows the plain brainkey in a dialog so that the user can view and Copy it.
     */
    private fun showBrainKeyDialog(view: View, brainKey: BrainKey) {
        MaterialDialog(view.context).show {
            title(text = "BrainKey")
            message(text = brainKey.brainKey)
            customView(R.layout.dialog_copy_brainkey)
            cancelable(false)
            positiveButton(android.R.string.copy) {
                Toast.makeText(it.context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                val clipboard = it.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("label", brainKey.brainKey)
                clipboard.primaryClip = clip
                it.dismiss()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val intent = Intent(context, NetworkService::class.java)
        if (context?.bindService(intent, this, Context.BIND_AUTO_CREATE) == true) {
            mShouldUnbindNetwork = true
        } else {
            Log.e(TAG, "Binding to the network service failed.")
        }
    }

    override fun onPause() {
        super.onPause()

        // Unbinding from network service
        if (mShouldUnbindNetwork) {
            context?.unbindService(this)
            mShouldUnbindNetwork = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!mDisposables.isDisposed) mDisposables.dispose()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        tvNetworkStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null,
            resources.getDrawable(R.drawable.ic_disconnected, null), null)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        // We've bound to LocalService, cast the IBinder and get LocalService instance
        val binder = service as NetworkService.LocalBinder
        mNetworkService = binder.service

        tvNetworkStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null,
            resources.getDrawable(R.drawable.ic_connected, null), null)
    }
}

