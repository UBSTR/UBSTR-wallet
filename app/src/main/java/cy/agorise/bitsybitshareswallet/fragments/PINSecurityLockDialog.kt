package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.jakewharton.rxbinding3.widget.textChanges
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.utils.CryptoUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.dialog_pin_security_lock.*

/**
 * Contains all the specific logic to create and confirm a new PIN or verifying the validity of the current one.
 */
class PINSecurityLockDialog : BaseSecurityLockDialog() {

    companion object {
        const val TAG = "PINSecurityLockDialog"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.dialog_pin_security_lock, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Listens to the event when the user clicks the 'Enter' button in the keyboard and acts accordingly
        tietPIN.setOnEditorActionListener { v, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val encryptedPIN = CryptoUtils.encrypt(v.context, v.text.toString()).trim()

                if (currentStep == STEP_SECURITY_LOCK_VERIFY) {
                    // The user just wants to verify the current encrypted PIN/Pattern
                    if (encryptedPIN == currentEncryptedPINPattern) {
                        // PIN is correct, proceed
                        dismiss()
                        mCallback?.onPINPatternEntered(actionIdentifier)
                    } else {
                        tilPIN.error = "Wrong PIN"
                    }
                }

                handled = true
            }
            handled
        }

        // Use RxBindings to clear the error when the user edits the PIN
        mDisposables.add(
            tietPIN.textChanges()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { tilPIN.isErrorEnabled = false }
        )
    }
}