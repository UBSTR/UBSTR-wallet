package cy.agorise.bitsybitshareswallet.utils

import android.content.Context
import android.content.res.ColorStateList
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Creates an enabled state, by enabling the button and using the given [colorResource] to color it.
 */
fun FloatingActionButton.enable(colorResource: Int) {
    this.isEnabled = true
    this.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this.context, colorResource))
}

/**
 * Creates a disabled state, by disabling the button and using the given [colorResource] to color it.
 */
fun FloatingActionButton.disable(colorResource: Int) {
    this.isEnabled = false
    this.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this.context, colorResource))
}

/**
 * Easily create a toast message with less boilerplate code
 */
fun Context.toast(message: CharSequence, duration: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, message, duration).show()
}