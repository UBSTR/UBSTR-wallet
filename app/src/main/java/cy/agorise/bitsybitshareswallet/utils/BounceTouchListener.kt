package cy.agorise.bitsybitshareswallet.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ListView
import android.widget.ScrollView
import androidx.annotation.IdRes
import androidx.core.view.MotionEventCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class BounceTouchListener private constructor(
    private val mMainView: View,
    contentResId: Int,
    private val onTranslateListener: OnTranslateListener?
) :
    View.OnTouchListener {

    private var downCalled = false
    private val mContent: View
    private var mDownY: Float = 0.toFloat()
    private var mSwipingDown: Boolean = false
    private var mSwipingUp: Boolean = false
    private val mInterpolator = DecelerateInterpolator(3f)
    private val swipUpEnabled = true
    private var mActivePointerId = -99
    private var mLastTouchY = -99f
    private var mMaxAbsTranslation = -99


    init {
        mContent = if (contentResId == -1) mMainView else mMainView.findViewById(contentResId)
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        val action = MotionEventCompat.getActionMasked(motionEvent)

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                run {
                    onDownMotionEvent(motionEvent)
                    view.onTouchEvent(motionEvent)
                    downCalled = true
                    if (mContent.translationY == 0f) {
                        return false
                    }
                }
                run {
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
                    if (swipUpEnabled) {
                        if (Math.abs(deltaY) > 0 && hasHitBottom() && deltaY < 0) {
                            mSwipingUp = true
                            sendCancelEventToView(view, motionEvent)
                        }
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
                        var translation =
                            (deltaY / Math.abs(deltaY) * Math.pow(Math.abs(deltaY).toDouble(), .8)).toInt()
                        if (mMaxAbsTranslation > 0) {
                            if (translation < 0) {
                                translation = Math.max(-mMaxAbsTranslation, translation)
                            } else {
                                translation = Math.min(mMaxAbsTranslation, translation)
                            }
                        }
                        mContent.translationY = translation.toFloat()
                        onTranslateListener?.onTranslate(mContent.translationY)

                        return true
                    }
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
                if (swipUpEnabled) {
                    if (Math.abs(deltaY) > 0 && hasHitBottom() && deltaY < 0) {
                        mSwipingUp = true
                        sendCancelEventToView(view, motionEvent)
                    }
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
                    mContent.translationY = translation.toFloat()
                    onTranslateListener?.onTranslate(mContent.translationY)
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                mActivePointerId = -99
                // cancel
                mContent.animate()
                    .setInterpolator(mInterpolator)
                    .translationY(0f)
                    .setDuration(DEFAULT_ANIMATION_TIME)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            (animation as ValueAnimator).addUpdateListener {
                                onTranslateListener?.onTranslate(mContent.translationY)
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

                    if (mContent.translationY > 0) {
                        mDownY = mLastTouchY - Math.pow(mContent.translationY.toDouble(), (10f / 8f).toDouble()).toInt()
                        mContent.animate().cancel()
                    } else if (mContent.translationY < 0) {
                        mDownY = mLastTouchY +
                                Math.pow((-mContent.translationY).toDouble(), (10f / 8f).toDouble()).toInt()
                        mContent.animate().cancel()
                    }
                }
            }
        }
        return false
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

        if (mContent.translationY > 0) {
            mDownY = mLastTouchY - Math.pow(mContent.translationY.toDouble(), (10f / 8f).toDouble()).toInt()
            mContent.animate().cancel()
        } else if (mContent.translationY < 0) {
            mDownY = mLastTouchY + Math.pow((-mContent.translationY).toDouble(), (10f / 8f).toDouble()).toInt()
            mContent.animate().cancel()
        } else {
            mDownY = mLastTouchY
        }
    }

    private fun hasHitBottom(): Boolean {
        if (mMainView is ScrollView) {
            val scrollView = mMainView
            val view = scrollView.getChildAt(scrollView.childCount - 1)
            val diff = view.bottom - (scrollView.height + scrollView.scrollY)// Calculate the scrolldiff
            return diff == 0
        } else if (mMainView is ListView) {
            val listView = mMainView
            if (listView.adapter != null) {
                if (listView.adapter.count > 0) {
                    return listView.lastVisiblePosition == listView.adapter.count - 1 && listView.getChildAt(listView.childCount - 1).bottom <= listView.height
                }
            }
        } else if (mMainView is RecyclerView) {
            val recyclerView = mMainView
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
        }
        return false
    }

    private fun hasHitTop(): Boolean {
        if (mMainView is ScrollView) {
            return mMainView.scrollY == 0
        } else if (mMainView is ListView) {
            val listView = mMainView
            if (listView.adapter != null) {
                if (listView.adapter.count > 0) {
                    return listView.firstVisiblePosition == 0 && listView.getChildAt(0).top >= 0
                }
            }
        } else if (mMainView is RecyclerView) {
            val recyclerView = mMainView
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
        }

        return false
    }

    fun setMaxAbsTranslation(maxAbsTranslation: Int) {
        this.mMaxAbsTranslation = maxAbsTranslation
    }

    interface OnTranslateListener {
        fun onTranslate(translation: Float)
    }

    companion object {
        private val DEFAULT_ANIMATION_TIME = 600L

        /**
         * Creates a new BounceTouchListener
         *
         * @param mainScrollableView  The main view that this touch listener is attached to
         * @param onTranslateListener To perform action on translation, can be null if not needed
         * @return A new BounceTouchListener attached to the given scrollable view
         */
        fun create(mainScrollableView: View, onTranslateListener: OnTranslateListener?): BounceTouchListener {
            return create(mainScrollableView, -1, onTranslateListener)
        }

        /**
         * Creates a new BounceTouchListener
         *
         * @param mainView            The main view that this touch listener is attached to
         * @param contentResId        Resource Id of the scrollable view
         * @param onTranslateListener To perform action on translation, can be null if not needed
         * @return A new BounceTouchListener attached to the given scrollable view
         */
        fun create(
            mainView: View, @IdRes contentResId: Int,
            onTranslateListener: OnTranslateListener?
        ): BounceTouchListener {
            return BounceTouchListener(mainView, contentResId, onTranslateListener)
        }
    }
}