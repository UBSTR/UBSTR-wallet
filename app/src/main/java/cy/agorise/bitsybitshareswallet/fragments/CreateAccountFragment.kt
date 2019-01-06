package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.jakewharton.rxbinding3.widget.textChanges
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.toast
import cy.agorise.graphenej.Address
import cy.agorise.graphenej.BrainKey
import cy.agorise.graphenej.api.ConnectionStatusUpdate
import cy.agorise.graphenej.models.JsonRpcResponse
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_create_account.*
import org.bitcoinj.core.ECKey
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class CreateAccountFragment : ConnectedFragment() {

    companion object {
        private const val TAG = "CreateAccountFragment"

        private const val BRAINKEY_FILE = "brainkeydict.txt"
    }

    private lateinit var mBrainKey: BrainKey
    private lateinit var mAddress: String

    /** Variables used to store the validation status of the form fields */
    private var isPINValid = false
    private var isPINConfirmationValid = false
    private var isAccountValid = true // TODO make false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.fragment_create_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use RxJava Debounce to update the PIN error only after the user stops writing for > 500 ms
        mDisposables.add(
            tietPin.textChanges()
                .skipInitialValue()
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { validatePIN() }
        )

        // Use RxJava Debounce to update the PIN Confirmation error only after the user stops writing for > 500 ms
        mDisposables.add(
            tietPinConfirmation.textChanges()
                .skipInitialValue()
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { validatePINConfirmation() }
        )

        btnCancel.setOnClickListener { findNavController().navigateUp() }

        btnCreate.isEnabled = false

        // Generating BrainKey
        generateKeys()
    }

    private fun validatePIN() {
        val pin = tietPin.text.toString()

        if (pin.length < Constants.MIN_PIN_LENGTH) {
            tilPin.error = getString(R.string.error__pin_too_short)
            isPINValid = false
        } else {
            tilPin.isErrorEnabled = false
            isPINValid = true
        }

        validatePINConfirmation()
    }

    private fun validatePINConfirmation() {
        val pinConfirmation = tietPinConfirmation.text.toString()

        if (pinConfirmation != tietPin.text.toString()) {
            tilPinConfirmation.error = getString(R.string.error__pin_mismatch)
            isPINConfirmationValid = false
        } else {
            tilPinConfirmation.isErrorEnabled = false
            isPINConfirmationValid = true
        }

        enableDisableCreateButton()
    }

    private fun enableDisableCreateButton() {
        btnCreate.isEnabled =  (isPINValid && isPINConfirmationValid && isAccountValid)
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