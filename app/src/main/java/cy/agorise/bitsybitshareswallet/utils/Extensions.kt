package cy.agorise.bitsybitshareswallet.utils

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

fun FloatingActionButton.enable(colorResource: Int) {
    this.isEnabled = true
    this.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this.context, colorResource))
}

fun FloatingActionButton.disable(colorResource: Int) {
    this.isEnabled = false
    this.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this.context, colorResource))
}