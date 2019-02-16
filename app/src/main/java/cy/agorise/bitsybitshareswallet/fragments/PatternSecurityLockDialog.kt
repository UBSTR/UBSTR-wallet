package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cy.agorise.bitsybitshareswallet.R
import kotlinx.android.synthetic.main.dialog_pattern_security_lock.*
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.listener.PatternLockViewListener
import cy.agorise.bitsybitshareswallet.utils.Constants
import cy.agorise.bitsybitshareswallet.utils.CryptoUtils


/**
 * Contains all the specific logic to create and confirm a new Pattern or verifying the validity of the current one.
 */
class PatternSecurityLockDialog : BaseSecurityLockDialog() {

    companion object {
        const val TAG = "PatternSecurityLockDialog"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.dialog_pattern_security_lock, container, false)
    }

    private var newPattern = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupScreen()

        patternLockView.addPatternLockListener(mPatternLockViewListener)

        btnClear.setOnClickListener { setupScreen() }
    }

    private val mPatternLockViewListener = object : PatternLockViewListener {
        override fun onStarted() {
            // Make sure the button is hidden when the user starts a new pattern when it was incorrect
            btnClear.visibility = View.INVISIBLE
            when (currentStep) {
                STEP_SECURITY_LOCK_VERIFY -> {
                    // Do something
                }
                STEP_SECURITY_LOCK_CREATE -> {
                    tvMessage.text = getString(R.string.msg__release_finger)
                }
                STEP_SECURITY_LOCK_CONFIRM -> {
                    tvMessage.text = getString(R.string.msg__release_finger)
                }
            }
        }

        override fun onProgress(progressPattern: List<PatternLockView.Dot>) {

        }

        override fun onComplete(pattern: List<PatternLockView.Dot>) {
            if (currentStep == STEP_SECURITY_LOCK_VERIFY) {
                // Do something
            } else if (currentStep == STEP_SECURITY_LOCK_CREATE) {
                btnClear.visibility = View.VISIBLE
                if (pattern.size < 4) {
                    tvMessage.text = getString(R.string.error__connect_at_least_4_dots)
                    patternLockView.setViewMode(PatternLockView.PatternViewMode.WRONG)
                } else {
                    tvMessage.text = getString(R.string.text__pattern_recorded)
                    patternLockView.setViewMode(PatternLockView.PatternViewMode.CORRECT)
                    patternLockView.isInputEnabled = false
                    btnNext.isEnabled = true
                    newPattern = getStringPattern(pattern)
                    btnNext.setOnClickListener {
                        currentStep = STEP_SECURITY_LOCK_CONFIRM
                        setupScreen()
                    }
                }
            } else if (currentStep == STEP_SECURITY_LOCK_CONFIRM) {
                val patternConfirm = getStringPattern(pattern)
                if (patternConfirm != newPattern) {
                    tvMessage.text = getString(R.string.error__wront_pattern)
                    btnNext.isEnabled = false
                    patternLockView.setViewMode(PatternLockView.PatternViewMode.WRONG)
                } else {
                    tvMessage.text = getString(R.string.msg__your_new_unlock_pattern)
                    patternLockView.isEnabled = false
                    patternLockView.setViewMode(PatternLockView.PatternViewMode.CORRECT)
                    btnNext.isEnabled = true
                    btnNext.setOnClickListener {
                        context?.let {
                            val encryptedPattern = CryptoUtils.encrypt(it, patternConfirm).trim()

                            // Stores the newly selected Pattern, encrypted
                            PreferenceManager.getDefaultSharedPreferences(it).edit()
                                .putString(Constants.KEY_ENCRYPTED_PIN, encryptedPattern)
                                .putInt(Constants.KEY_SECURITY_LOCK_SELECTED, 1).apply() // 1 -> Pattern

                            dismiss()
                            mCallback?.onPINPatternChanged()
                        }
                    }
                }
            }
        }

        override fun onCleared() {

        }
    }

    /**
     * Converts the given pattern into a string representation of it.
     */
    private fun getStringPattern(pattern: List<PatternLockView.Dot>): String {
        val sb = StringBuilder()
        for (dot in pattern)
            sb.append(dot.id)

        return sb.toString()
    }

    private fun setupScreen() {
        when (currentStep) {
            STEP_SECURITY_LOCK_VERIFY -> {
                tvTitle.text = getString(R.string.title__re_enter_your_pattern)
                tvSubTitle.text = getString(R.string.msg__enter_your_pattern)
                btnClear.visibility = View.INVISIBLE
            }
            STEP_SECURITY_LOCK_CREATE -> {
                tvTitle.text = getString(R.string.title__set_bitsy_screen_lock)
                tvSubTitle.text = getString(R.string.msg__set_bitsy_pattern)
                tvMessage.text = getString(R.string.text__draw_an_unlock_pattern)
                patternLockView.clearPattern()
                patternLockView.isInputEnabled = true
                btnClear.visibility = View.INVISIBLE
                btnNext.isEnabled = false
            }
            STEP_SECURITY_LOCK_CONFIRM -> {
                tvTitle.text = getString(R.string.title__re_enter_your_pattern)
                tvSubTitle.text = ""
                tvMessage.text = getString(R.string.msg__draw_pattern_confirm)
                tvSubTitle.visibility = View.GONE
                patternLockView.clearPattern()
                patternLockView.isInputEnabled = true
                btnClear.visibility = View.INVISIBLE
                btnNext.isEnabled = false
                btnNext.text = getString(R.string.btn__confirm)
            }
        }
    }
}