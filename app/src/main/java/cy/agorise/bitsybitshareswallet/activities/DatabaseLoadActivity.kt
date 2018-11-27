package cy.agorise.bitsybitshareswallet.activities

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.repositories.AssetRepository
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.graphenej.Asset
import cy.agorise.graphenej.api.ApiAccess
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.api.calls.ListAssets
import cy.agorise.graphenej.models.JsonRpcResponse
import kotlinx.android.synthetic.main.activity_database_load.*

class DatabaseLoadActivity: ConnectedActivity() {
    private val TAG = "DatabaseLoadActivity"

    /** Time in milliseconds to wait before re-trying to access the full node */
    private val NETWORK_RETRY_PERIOD: Long = 1000

    /** Handler instance used to schedule tasks back to the main thread */
    private var mHandler: Handler? = null

    /** Timer used to avoid multiple instances of the following activity */
    private var countDownTimer: CountDownTimer? = null

    /** Repository used as the single point of truth for Assets */
    private var mAssetRepository: AssetRepository? = null

    // Variable used to keep track of the last lower bound used int asset batch loading
    private var lastLowerBound: String? = null

    // Variable used to keep track of the possession state of the database api id
    private var hasDatabaseApiId: Boolean = false

    // Variable used to count the number of assets already loaded
    private var loadedAssetsCounter: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database_load)

        mHandler = Handler()

        mAssetRepository = AssetRepository(application)

        btnNext.setOnClickListener { onNext() }
    }
    override fun handleJsonRpcResponse(response: JsonRpcResponse<*>) {
        if (response.result is List<*> &&
            (response.result as List<*>).size > 0 &&
            (response.result as List<*>)[0] is Asset) {
            handlePlatformAssetBatch(response.result as List<Asset>)
        }
    }

    override fun handleConnectionStatusUpdate(connectionStatusUpdate: ConnectionStatusUpdate) {
        if (connectionStatusUpdate.updateCode == ConnectionStatusUpdate.API_UPDATE &&
            connectionStatusUpdate.api and ApiAccess.API_DATABASE == ApiAccess.API_DATABASE) {
            hasDatabaseApiId = true
            this.lastLowerBound = ""
            sendAssetBatchRequest()
        }
    }

    /**
     * Method that issues a request for the next 100 known assets, starting from the last known
     * lower bound for the asset symbol.
     */
    private fun sendAssetBatchRequest() {
        if (mNetworkService != null && mNetworkService!!.isConnected) {
            mNetworkService!!.sendMessage(ListAssets(lastLowerBound, ListAssets.LIST_ALL), ListAssets.REQUIRED_API)
        } else {
            Handler().postDelayed({ sendAssetBatchRequest() }, NETWORK_RETRY_PERIOD)
        }
    }

    /**
     * Method that loads a new batch of platform assets into the database and decides whether to finish
     * the procedure or to keep requesting for more assets.
     *
     * @param assetList The list of assets obtained in the last 'list_assets' API call.
     */
    private fun handlePlatformAssetBatch(assetList: List<Asset>) {
        val assets = mutableListOf<cy.agorise.bitsybitshareswallet.entities.Asset>()

        // TODO find if there is a better way to convert to Bitsy Asset instances
        for (_asset in assetList) {
            val asset = cy.agorise.bitsybitshareswallet.entities.Asset(
                _asset.objectId,
                _asset.symbol,
                _asset.precision,
                _asset.description ?: "",
                _asset.bitassetId ?: ""
            )

            assets.add(asset)
        }

        mAssetRepository!!.insertAll(assets)

        loadedAssetsCounter += assetList.size

        tvLoadMessage.text = getString(R.string.text__loading_assets, loadedAssetsCounter)

        if (assetList.size < ListAssets.MAX_BATCH_SIZE) {
            // We might have reached the end of the asset list
            Log.d(TAG, "We might have reached the end!")

            // Storing the last asset update time and setting the database as loaded
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .edit()
                .putLong(Constants.KEY_LAST_ASSET_LIST_UPDATE, System.currentTimeMillis())
                .apply()

            onAssetsReady()
        } else {
            // Using the last asset symbol in the list as the new lower bound.
            lastLowerBound = assetList[assetList.size - 1].symbol
            sendAssetBatchRequest()
        }
    }

    private fun onAssetsReady() {
        // Storing the last asset update time and setting the database as loaded
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
            .edit()
            .putBoolean(Constants.KEY_DATABASE_LOADED, true)
            .apply()


        mHandler!!.post {
            progressBar.visibility = View.INVISIBLE
            btnNext.isEnabled = true
            tvLoadTitle.setText(R.string.title__assets_loaded)
            tvLoadMessage.setText(R.string.text__assets_loaded)

            // Timer to automatically take user to the next activity
            countDownTimer = object : CountDownTimer(5000, 1000) {

                override fun onTick(millisUntilFinished: Long) {}

                override fun onFinish() {
                    onNext()
                }
            }.start()
        }
    }

    /**
     * Called whenever the user clicks on the 'next' button_light. This button_light will only be visible when
     * the database loading procedure is done, OR if there was an error in it.
     */
    fun onNext() {
        // Cancel timer to avoid starting InitialSetupActivity twice
        countDownTimer!!.cancel()

        val intent = Intent(applicationContext, ImportBrainkeyActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
//        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        finish()
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        super.onServiceConnected(name, service)

        hasDatabaseApiId = mNetworkService!!.hasApiId(ApiAccess.API_DATABASE)
        if (hasDatabaseApiId) {
            this.lastLowerBound = ""
            sendAssetBatchRequest()
        }
    }
}