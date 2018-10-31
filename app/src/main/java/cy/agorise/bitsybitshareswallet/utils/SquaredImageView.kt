package cy.agorise.crystalwallet.util

import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet

/**
 * Created by xd on 1/24/18.
 * ImageView which adjusts its size to always create a square
 */

class SquaredImageView : AppCompatImageView {
    constructor(context: Context) : super(context) {}

    constructor(context: Context, @Nullable attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, @Nullable attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
    }

    protected fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val size = Math.min(getMeasuredWidth(), getMeasuredHeight())
        setMeasuredDimension(size, size)
    }
}
