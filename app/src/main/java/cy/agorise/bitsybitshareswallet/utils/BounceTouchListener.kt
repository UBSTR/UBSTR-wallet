package cy.agorise.bitsybitshareswallet.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.view.MotionEventCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class BounceTouchListener(private val mRecyclerView: RecyclerView) : View.OnTouchListener {

    private var downCalled = false
    private var mDownY: Float = 0.toFloat()
    private var mSwipingDown: Boolean = false
    private var mSwipingUp: Boolean = false
    private val mInterpolator = DecelerateInterpolator(3f)
    private var mActivePointerId = -99
    private var mLastTouchY = -99f
    private var mMaxAbsTranslation = -99

    init {
        mRecyclerView.pivotY = 0F
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        val action = MotionEventCompat.getActionMasked(motionEvent)

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                onDownMotionEvent(motionEvent)
                view.onTouchEvent(motionEvent)
                downCalled = true
                if (this.mRecyclerView.translationY == 0f) {
                    return false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mActivePointerId == -99) {
                    onDownMotionEvent(motionEvent)
                    downCalled = true
                }
                val pointerIndex = MotionEventCompat.findPointerIndex(motionEvent, mActivePointerId)
                val y = MotionEventCompat.getY(motionEvent, pointerIndex)
                if (!hasHitTop() && !hasHitBottom() || !downCalled) {
                    if (!downCalled) {
                        downCalled = true
                    }
                    mDownY = y
                    view.onTouchEvent(motionEvent)
                    return false
                }
                val deltaY = y - mDownY
                if (Math.abs(deltaY) > 0 && hasHitTop() && deltaY > 0) {
                    mSwipingDown = true
                    sendCancelEventToView(view, motionEvent)
                }
                if (Math.abs(deltaY) > 0 && hasHitBottom() && deltaY < 0) {
                    mSwipingUp = true
                    sendCancelEventToView(view, motionEvent)
                }
                if (mSwipingDown || mSwipingUp) {
                    if (deltaY <= 0 && mSwipingDown || deltaY >= 0 && mSwipingUp) {
                        mDownY = 0f
                        mSwipingDown = false
                        mSwipingUp = false
                        downCalled = false
                        val downEvent = MotionEvent.obtain(motionEvent)
                        downEvent.action = MotionEvent.ACTION_DOWN or
                                (MotionEventCompat.getActionIndex(motionEvent) shl MotionEventCompat.ACTION_POINTER_INDEX_SHIFT)
                        view.onTouchEvent(downEvent)
                    }
                    var translation = (deltaY / Math.abs(deltaY) * Math.pow(Math.abs(deltaY).toDouble(), .8)).toInt()
                    if (mMaxAbsTranslation > 0) {
                        if (translation < 0) {
                            translation = Math.max(-mMaxAbsTranslation, translation)
                        } else {
                            translation = Math.min(mMaxAbsTranslation, translation)
                        }
                    }
                    mRecyclerView.translationY = translation.toFloat()
                    translate(mRecyclerView.translationY)
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                mActivePointerId = -99
                // cancel
                mRecyclerView.animate()
                    .setInterpolator(mInterpolator)
                    .translationY(0f)
                    .setDuration(600L)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            (animation as ValueAnimator).addUpdateListener {
                                translate(mRecyclerView.translationY)
                            }
                            super.onAnimationStart(animation)
                        }
                    })

                mDownY = 0f
                mSwipingDown = false
                mSwipingUp = false
                downCalled = false
            }

            MotionEvent.ACTION_CANCEL -> {
                mActivePointerId = -99
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = MotionEventCompat.getActionIndex(motionEvent)
                val pointerId = MotionEventCompat.getPointerId(motionEvent, pointerIndex)

                if (pointerId == mActivePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    mLastTouchY = MotionEventCompat.getY(motionEvent, newPointerIndex)
                    mActivePointerId = MotionEventCompat.getPointerId(motionEvent, newPointerIndex)

                    if (this.mRecyclerView.translationY > 0) {
                        mDownY = mLastTouchY - Math.pow(this.mRecyclerView.translationY.toDouble(), (10f / 8f).toDouble()).toInt()
                        this.mRecyclerView.animate().cancel()
                    } else if (this.mRecyclerView.translationY < 0) {
                        mDownY = mLastTouchY +
                                Math.pow((-this.mRecyclerView.translationY).toDouble(), (10f / 8f).toDouble()).toInt()
                        this.mRecyclerView.animate().cancel()
                    }
                }
            }
        }
        return false
    }

    private fun translate(translation: Float) {
        val scale = 2 * translation / mRecyclerView.measuredHeight + 1
        mRecyclerView.scaleY = Math.pow(scale.toDouble(), .6).toFloat()
    }

    private fun sendCancelEventToView(view: View, motionEvent: MotionEvent) {
        (view as ViewGroup).requestDisallowInterceptTouchEvent(true)
        val cancelEvent = MotionEvent.obtain(motionEvent)
        cancelEvent.action = MotionEvent.ACTION_CANCEL or
                (MotionEventCompat.getActionIndex(motionEvent) shl MotionEventCompat.ACTION_POINTER_INDEX_SHIFT)
        view.onTouchEvent(cancelEvent)
    }

    private fun onDownMotionEvent(motionEvent: MotionEvent) {
        val pointerIndex = MotionEventCompat.getActionIndex(motionEvent)
        mLastTouchY = MotionEventCompat.getY(motionEvent, pointerIndex)
        mActivePointerId = MotionEventCompat.getPointerId(motionEvent, 0)

        if (this.mRecyclerView.translationY > 0) {
            mDownY = mLastTouchY - Math.pow(this.mRecyclerView.translationY.toDouble(), (10f / 8f).toDouble()).toInt()
            this.mRecyclerView.animate().cancel()
        } else if (this.mRecyclerView.translationY < 0) {
            mDownY = mLastTouchY + Math.pow((-this.mRecyclerView.translationY).toDouble(), (10f / 8f).toDouble()).toInt()
            this.mRecyclerView.animate().cancel()
        } else {
            mDownY = mLastTouchY
        }
    }

    private fun hasHitBottom(): Boolean {
        val recyclerView = this.mRecyclerView
        if (recyclerView.adapter != null && recyclerView.layoutManager != null) {
            val adapter = recyclerView.adapter
            if (adapter!!.itemCount > 0) {
                val layoutManager = recyclerView.layoutManager
                if (layoutManager is LinearLayoutManager) {
                    val linearLayoutManager = layoutManager as LinearLayoutManager?
                    return linearLayoutManager!!.findLastCompletelyVisibleItemPosition() == adapter.itemCount - 1
                } else if (layoutManager is StaggeredGridLayoutManager) {
                    val staggeredGridLayoutManager = layoutManager as StaggeredGridLayoutManager?
                    val checks = staggeredGridLayoutManager!!.findLastCompletelyVisibleItemPositions(null)
                    for (check in checks) {
                        if (check == adapter.itemCount - 1)
                            return true
                    }
                }
            }
        }
        return false
    }

    private fun hasHitTop(): Boolean {
        val recyclerView = this.mRecyclerView
        if (recyclerView.adapter != null && recyclerView.layoutManager != null) {
            val adapter = recyclerView.adapter
            if (adapter!!.itemCount > 0) {
                val layoutManager = recyclerView.layoutManager
                if (layoutManager is LinearLayoutManager) {
                    val linearLayoutManager = layoutManager as LinearLayoutManager?
                    return linearLayoutManager!!.findFirstCompletelyVisibleItemPosition() == 0
                } else if (layoutManager is StaggeredGridLayoutManager) {
                    val staggeredGridLayoutManager = layoutManager as StaggeredGridLayoutManager?
                    val checks = staggeredGridLayoutManager!!.findFirstCompletelyVisibleItemPositions(null)
                    for (check in checks) {
                        if (check == 0)
                            return true
                    }
                }
            }
        }

        return false
    }
}