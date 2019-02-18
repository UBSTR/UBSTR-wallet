package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import cy.agorise.bitsybitshareswallet.utils.Constants
import io.reactivex.disposables.CompositeDisposable

/**
 * Encapsulates the shared logic required for the PIN and Pattern Security Lock Fragments.
 */
abstract class BaseSecurityLockDialog : DialogFragment() {

    companion object {
        /** Used to denote that the user is in the step of creating the preferred security lock option */
        const val STEP_SECURITY_LOCK_CREATE = 1

        /** Used to denote that the user is in the step of confirming the newly created security lock option,
         * this option should only be used internally */
        const val STEP_SECURITY_LOCK_CONFIRM = 2

        /** Used to denote that the user is in the step of verifying the current security lock option, to give
         * permission to do a security constrained action like sending a transaction or trying to change the
         * current security lock option */
        const val STEP_SECURITY_LOCK_VERIFY = 3

        /** Used to let the dialog know if the user wants to create a new PIN/Pattern or just verify its correctness
         * to get access to security constrained actions */
        const val KEY_STEP_SECURITY_LOCK = "key_step_security_lock"

        /** The calling fragment can be calling this dialog to unlock many different things, this variable helps to
         * keep track of what action the calling fragment wants to achieve */
        const val KEY_ACTION_IDENTIFIER = "key_action_identifier"
    }

    // Container Fragment must implement this interface
    interface OnPINPatternEnteredListener {
        fun onPINPatternEntered(actionIdentifier: Int)
        fun onPINPatternChanged()
    }

    /** Callback used to notify the parent that a PIN/Pattern has been entered successfully */
    protected var mCallback: OnPINPatternEnteredListener? = null

    /** Keeps track of the current step, can be create, confirm of verify */
    protected var currentStep: Int = -1

    /** Used so the calling object knows what was the intention to ask for the Security Lock */
    protected var actionIdentifier: Int = -1

    /** Keeps track of all RxJava disposables, to make sure they are all disposed when the fragment is destroyed */
    protected var mDisposables = CompositeDisposable()

    /** Current hashed version of the salt + PIN/Pattern */
    protected var currentHashedPINPattern: String? = null

    /** Salt used to hash the current PIN/Pattern */
    protected var currentPINPatternSalt: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onAttachToParentFragment(parentFragment)

        currentHashedPINPattern = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_HASHED_PIN_PATTERN, "")

        currentPINPatternSalt = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.KEY_PIN_PATTERN_SALT, "")

        currentStep = arguments?.getInt(KEY_STEP_SECURITY_LOCK) ?: -1

        actionIdentifier = arguments?.getInt(KEY_ACTION_IDENTIFIER) ?: -1
    }

    /**
     * Attaches the current [DialogFragment] to its [Fragment] parent, to initialize the
     * [OnPINPatternEnteredListener] interface
     */
    private fun onAttachToParentFragment(fragment: Fragment?) {
        try {
            mCallback = fragment as OnPINPatternEnteredListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$fragment must implement OnFilterOptionsSelectedListener")
        }
    }

    override fun onResume() {
        super.onResume()

        // Force dialog fragment to use the full width of the screen
        val dialogWindow = dialog.window
        dialogWindow?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!mDisposables.isDisposed) mDisposables.dispose()
    }
}