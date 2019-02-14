package cy.agorise.bitsybitshareswallet.fragments

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

/**
 * Encapsulates the shared logic required for the PIN and Pattern Security Lock Fragments.
 */
abstract class BaseSecurityLockDialog : DialogFragment() {

    companion object {
        /** Used to denote that the user is in the step of creating the preferred security lock option */
        const val SECURITY_LOG_STEP_CREATE = 0x01

        /** Used to denote that the user is in the step of confirming the just created security lock option */
        const val SECURITY_LOG_STEP_CONFIRM = 0x02

        /** Used to denote that the user is in the step of verifying the current security lock option, to give
         * permission to do a security constrained action like sending a transaction or trying to change the
         * current security lock option */
        const val SECURITY_LOG_STEP_VERIFY = 0x04
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }
}