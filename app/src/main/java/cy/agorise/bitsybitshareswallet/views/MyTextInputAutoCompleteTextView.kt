package cy.agorise.bitsybitshareswallet.views

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputLayout
import android.view.inputmethod.InputConnection
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import cy.agorise.bitsybitshareswallet.utils.hideKeyboard

/**
 * Custom AutoCompleteTextView to be used inside a TextInputLayout, so that they can share their hint
 * From https://stackoverflow.com/a/41864063/5428997
 * And also hides the keyboard when it loses focus.
 */
class MyTextInputAutoCompleteTextView : AppCompatAutoCompleteTextView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val ic = super.onCreateInputConnection(outAttrs)
        if (ic != null && outAttrs.hintText == null) {
            // If we don't have a hint and our parent is a TextInputLayout, use it's hint for the
            // EditorInfo. This allows us to display a hint in 'extract mode'.
            val parent = parent
            if (parent is TextInputLayout) {
                outAttrs.hintText = parent.hint
            }
        }
        // An EditText that lets you use actions ("Done", "Go", etc.) on multi-line edits.
        val imeActions = outAttrs.imeOptions and EditorInfo.IME_MASK_ACTION
        if (imeActions and EditorInfo.IME_ACTION_DONE != 0) {
            // clear the existing action
            outAttrs.imeOptions = outAttrs.imeOptions xor imeActions
            // set the DONE action
            outAttrs.imeOptions = outAttrs.imeOptions or EditorInfo.IME_ACTION_DONE
        }
        if (outAttrs.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) {
            outAttrs.imeOptions = outAttrs.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION.inv()
        }
        return ic
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (!focused) this.hideKeyboard()
    }
}