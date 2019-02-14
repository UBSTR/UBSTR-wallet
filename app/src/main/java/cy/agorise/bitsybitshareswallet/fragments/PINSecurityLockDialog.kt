package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cy.agorise.bitsybitshareswallet.R

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


    }

    override fun onResume() {
        super.onResume()

        // Force dialog fragment to use the full width of the screen
        val dialogWindow = dialog.window
        dialogWindow?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
}