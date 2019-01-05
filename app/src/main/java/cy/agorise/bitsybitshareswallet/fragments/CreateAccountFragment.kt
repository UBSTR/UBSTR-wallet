package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.utils.toast
import cy.agorise.graphenej.Address
import cy.agorise.graphenej.BrainKey
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.models.JsonRpcResponse
import kotlinx.android.synthetic.main.fragment_create_account.*
import org.bitcoinj.core.ECKey
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class CreateAccountFragment : ConnectedFragment() {

    companion object {
        private const val TAG = "CreateAccountFragment"

        private const val BRAINKEY_FILE = "brainkeydict.txt"
    }

    private lateinit var mBrainKey: BrainKey
    private lateinit var mAddress: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.fragment_create_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Generating BrainKey
        generateKeys()
    }

    override fun handleJsonRpcResponse(response: JsonRpcResponse<*>) {
        // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun handleConnectionStatusUpdate(connectionStatusUpdate: ConnectionStatusUpdate) {
        // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Method that generates a fresh key that will be controlling the newly created account.
     */
    private fun generateKeys() {
        var reader: BufferedReader? = null
        val dictionary: String
        try {
            reader = BufferedReader(InputStreamReader(context!!.assets.open(BRAINKEY_FILE), "UTF-8"))
            dictionary = reader.readLine()

            val brainKeySuggestion = BrainKey.suggest(dictionary)
            mBrainKey = BrainKey(brainKeySuggestion, 0)
            val address = Address(ECKey.fromPublicOnly(mBrainKey.privateKey.pubKey))
            Log.d(TAG, "brain key: $brainKeySuggestion")
            Log.d(TAG, "address would be: " + address.toString())
            mAddress = address.toString()
            tvBrainKey.text = mBrainKey.brainKey

        } catch (e: IOException) {
            Log.e(TAG, "IOException while trying to generate key. Msg: " + e.message)
            context?.toast(getString(R.string.error__read_dict_file))
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    Log.e(TAG, "IOException while trying to close BufferedReader. Msg: " + e.message)
                }

            }
        }
    }
}