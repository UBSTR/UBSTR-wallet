package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cy.agorise.bitsybitshareswallet.R
import kotlinx.android.synthetic.main.dialog_pattern_security_lock.*
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.listener.PatternLockViewListener
import cy.agorise.bitsybitshareswallet.utils.toast


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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupScreen()

        patternLockView.addPatternLockListener(mPatternLockViewListener)
    }

    private val mPatternLockViewListener = object : PatternLockViewListener {
        override fun onStarted() {
            context?.toast("Pattern started")
        }

        override fun onProgress(progressPattern: List<PatternLockView.Dot>) {
            tvMessage.text = getString(R.string.msg__release_finger)
        }

        override fun onComplete(pattern: List<PatternLockView.Dot>) {
            if (pattern.size < 4) {
                patternLockView.setViewMode(PatternLockView.PatternViewMode.WRONG)
            }
        }

        override fun onCleared() {
            context?.toast("Pattern has been cleared")
        }
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
                btnClear.visibility = View.INVISIBLE
            }
            STEP_SECURITY_LOCK_CONFIRM -> {
                tvTitle.text = getString(R.string.title__re_enter_your_pin)
                tvSubTitle.text = ""
                tvSubTitle.visibility = View.GONE
            }
        }
    }
}