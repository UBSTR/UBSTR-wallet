package cy.agorise.bitsybitshareswallet.views

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.os.Message
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import com.google.android.material.picker.MaterialDatePickerDialog
import cy.agorise.bitsybitshareswallet.fragments.FilterOptionsDialog
import java.util.*

class DatePickerFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {

    companion object {
        const val TAG = "DatePickerFragment"

        const val KEY_WHICH = "key_which"
        const val KEY_CURRENT = "key_current"
        const val KEY_MAX = "key_max"

        fun newInstance(
            which: Int, currentTime: Long, maxTime: Long,
            handler: FilterOptionsDialog.DatePickerHandler
        ): DatePickerFragment {
            val f = DatePickerFragment()
            val bundle = Bundle()
            bundle.putInt(KEY_WHICH, which)
            bundle.putLong(KEY_CURRENT, currentTime)
            bundle.putLong(KEY_MAX, maxTime)
            f.arguments = bundle
            f.setHandler(handler)
            return f
        }
    }

    private var which: Int = 0
    private var mHandler: FilterOptionsDialog.DatePickerHandler? = null

    fun setHandler(handler: FilterOptionsDialog.DatePickerHandler) {
        mHandler = handler
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        which = arguments!!.getInt(KEY_WHICH)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val currentTime = arguments!!.getLong(KEY_CURRENT)
        val maxTime = arguments!!.getLong(KEY_MAX)

        // Use the current date as the default date in the picker
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Create a new instance of DatePickerDialog and return it
        val datePicker = MaterialDatePickerDialog(activity!!, this, year, month, day)

        // Set maximum date allowed to today
        datePicker.datePicker.maxDate = maxTime

        return datePicker
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        val msg = Message.obtain()
        msg.arg1 = which
        val calendar = GregorianCalendar()
        calendar.set(year, month, day)
        val bundle = Bundle()
        bundle.putLong(FilterOptionsDialog.KEY_TIMESTAMP, calendar.time.time)
        msg.data = bundle
        mHandler!!.sendMessage(msg)
    }
}