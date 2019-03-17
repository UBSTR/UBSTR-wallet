package cy.agorise.bitsybitshareswallet.fragments

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.Fragment
import com.crashlytics.android.Crashlytics
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.android.NetworkService
import cy.agorise.graphenej.api.android.RxBus
import cy.agorise.graphenej.models.JsonRpcResponse
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

/**
 * Base fragment that defines the methods and variables commonly used in all fragments that directly connect and
 * talk to the BitShares nodes through graphenej's NetworkService
 */
abstract class ConnectedFragment : Fragment(), ServiceConnection {

    companion object {
        private const val TAG = "ConnectedFragment"
    }

    /** Network service connection */
    protected var mNetworkService: NetworkService? = null

    /** Flag used to keep track of the NetworkService binding state */
    private var mShouldUnbindNetwork: Boolean = false

    /** Keeps track of all RxJava disposables, to make sure they are all disposed when the fragment is destroyed */
    protected var mDisposables = CompositeDisposable()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val locale = ConfigurationCompat.getLocales(resources.configuration)[0]
        Crashlytics.setString(Constants.CRASHLYTICS_KEY_LANGUAGE, locale.displayName)

        // Connect to the RxBus, which receives events from the NetworkService
        mDisposables.add(
            RxBus.getBusInstance()
                .asFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { handleIncomingMessage(it) } ,
                        {t -> Crashlytics.log(Log.DEBUG, TAG, t.message) }
                )
        )
    }

    private fun handleIncomingMessage(message: Any?) {
        if (message is JsonRpcResponse<*>) {
            // Generic processing taken care by subclasses
            handleJsonRpcResponse(message)
        } else if (message is ConnectionStatusUpdate) {
            // Generic processing taken care by subclasses
            handleConnectionStatusUpdate(message)
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

    override fun onServiceDisconnected(name: ComponentName?) { }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        // We've bound to LocalService, cast the IBinder and get LocalService instance
        val binder = service as NetworkService.LocalBinder
        mNetworkService = binder.service
    }

    /**
     * Method to be implemented by all subclasses in order to be notified of JSON-RPC responses.
     * @param response
     */
    abstract fun handleJsonRpcResponse(response: JsonRpcResponse<*>)

    /**
     * Method to be implemented by all subclasses in order to be notified of connection status updates
     * @param connectionStatusUpdate
     */
    abstract fun handleConnectionStatusUpdate(connectionStatusUpdate: ConnectionStatusUpdate)
}