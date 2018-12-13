package cy.agorise.bitsybitshareswallet.fragments

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cy.agorise.bitsybitshareswallet.R
import cy.agorise.bitsybitshareswallet.utils.Constants
import kotlinx.android.synthetic.main.fragment_settings.*

class SettingsFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAutoCloseSwitch()

        initNightModeSwitch()
    }

    private fun initAutoCloseSwitch() {
        val autoCloseOn = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Constants.KEY_AUTO_CLOSE_ACTIVATED, false)

        switchAutoClose.isChecked = autoCloseOn

        switchAutoClose.setOnCheckedChangeListener { buttonView, isChecked ->
            PreferenceManager.getDefaultSharedPreferences(buttonView.context).edit()
                .putBoolean(Constants.KEY_AUTO_CLOSE_ACTIVATED, isChecked).apply()
        }
    }

    private fun initNightModeSwitch() {
        val nightModeOn = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, false)

        switchNightMode.isChecked = nightModeOn

        switchNightMode.setOnCheckedChangeListener { buttonView, isChecked ->

            PreferenceManager.getDefaultSharedPreferences(buttonView.context).edit()
                .putBoolean(Constants.KEY_NIGHT_MODE_ACTIVATED, isChecked).apply()

            // Recreates the activity to apply the selected theme
            activity?.recreate()
        }
    }
}

