/*
 * Copyright (c) 2022 Institute of Software, Chinese Academy of Sciences (ISCAS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.eulix.space.view.banner

import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.*
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.viewpager.widget.ViewPager
import xyz.eulix.space.R
import java.lang.ref.WeakReference

/**
 * Author:      Zhu Fuyu
 * Description: 自动滚动横幅
 * History:     2021/11/15
 */
class AutoScrollBanner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RelativeLayout(context, attrs, defStyle) {
    private var decentBannerAdapter: AutoScrollBannerAdapter? = null
    private var pointList = ArrayList<View>()
    private var realViewNum = 0
    private var pointHeight: Int
    private var pointSelectedWidth: Int
    private var pointUnselectedWidth: Int
    private val handler: ScrollHandler
    private var homeColumnScrollInterval: Long = 3000L

    private var datas = ArrayList<AdContentBean>()

    /**
     * 滑动的最小距离
     */
    private var mTouchSlop: Int

    private var mIndex: Int = 0


    private var totalViewsCount = 0

    private var isJump = false
    private var isTouching = false

    private var view_pager: ViewPager
    private var point_layout: LinearLayout

    private var mBannerType: Int = TYPE_NORMAL

    companion object {
        private const val MESSAGE_SCROLL = 123
        private const val TYPE_NORMAL = 1
        const val TYPE_ALBUM_HISTORY = 2
    }

    /**
     * 使用静态内部类，持有当前view的弱引用，
     */
    private class ScrollHandler(banner: AutoScrollBanner) : Handler() {
        val weakReference: WeakReference<AutoScrollBanner>? = WeakReference(banner)

        override fun handleMessage(msg: Message) {
            if (weakReference == null) {
                return
            }
            val decentBanner = weakReference.get() ?: return
            if (msg.what == MESSAGE_SCROLL) {
                decentBanner.scrollToNext()
            }
        }

    }


    private fun scrollToNext() {
        if (isTouching) {
            return
        }
        mIndex = view_pager.currentItem
        mIndex++
        view_pager.currentItem += 1
//            startAutoPlay()
    }

    private fun scrollToBack() {
        mIndex = view_pager.currentItem
        mIndex--
        view_pager.currentItem -= 1
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> isTouching = true
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> isTouching = false

        }
        return super.dispatchTouchEvent(ev)
    }

    private fun startAutoPlay() {
        stopAutoPlay()
        if (realViewNum == 1) {
            return
        }
        handler.sendEmptyMessageDelayed(
            MESSAGE_SCROLL,
            homeColumnScrollInterval
        )
    }

    private fun stopAutoPlay() {
        handler.removeMessages(MESSAGE_SCROLL)
    }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.AutoScrollBanner)
        try {
            mBannerType = ta.getInt(R.styleable.AutoScrollBanner_banner_type, TYPE_NORMAL)
        } finally {
            ta.recycle()
        }
        if (mBannerType == TYPE_ALBUM_HISTORY) {
            LayoutInflater.from(context).inflate(R.layout.decent_banner_layout_album, this)
        } else {
            LayoutInflater.from(context).inflate(R.layout.decent_banner_layout, this)
        }
        handler = ScrollHandler(this)
        pointHeight = resources.getDimensionPixelSize(R.dimen.dp_5)
        pointSelectedWidth = resources.getDimensionPixelSize(R.dimen.dp_5)
        pointUnselectedWidth = resources.getDimensionPixelSize(R.dimen.dp_5)

        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

        view_pager = findViewById(R.id.view_pager)
        point_layout = findViewById(R.id.point_layout)
        if (mBannerType == TYPE_ALBUM_HISTORY) {
            point_layout.visibility = INVISIBLE
            view_pager.pageMargin = context.resources.getDimensionPixelSize(R.dimen.dp_5)
        }
    }


    fun init(adContentBeans: List<AdContentBean>) {
        datas.clear()
        datas.addAll(adContentBeans)
        realViewNum = datas.size
        viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    viewTreeObserver
                        .removeOnPreDrawListener(this)
                    return true
                }
            })

        val newData = ArrayList<AdContentBean>()
        if (realViewNum > 1) {
            newData.add(adContentBeans[datas.lastIndex])
        }
        newData.addAll(datas)
        if (realViewNum > 1) {
            newData.add(adContentBeans[0])
        }
        totalViewsCount = newData.size
        decentBannerAdapter =
            AutoScrollBannerAdapter(context, mBannerType, totalViewsCount, newData)

        view_pager.adapter = decentBannerAdapter
        addPoints(realViewNum)
        view_pager.offscreenPageLimit = 2
        mIndex = 1
        view_pager.currentItem = mIndex
        view_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    if (mIndex == 0) {
                        mIndex = totalViewsCount - 2
                        isJump = true
                        view_pager.setCurrentItem(mIndex, false)//切换，不要动画效果
                    } else if (mIndex == totalViewsCount - 1) {
                        mIndex = 1
                        isJump = true
                        view_pager.setCurrentItem(mIndex, false)//切换，不要动画效果
                    }
                    startAutoPlay()
                }
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {

            }

            override fun onPageSelected(position: Int) {
                mIndex = position
                if (mIndex == 0) {
                    setPointPosition(realViewNum - 1)
                    return
                }
                if (isJump) {
                    //跳转的不改变指示器
                    isJump = false
                } else if (mIndex != 0) {
                    setPointPosition(mIndex - 1)
                }
            }
        })
        startAutoPlay()
    }

    private fun addPoints(size: Int) {
        if (size <= 1) {
            point_layout.visibility = View.GONE
            return
        }
        point_layout.removeAllViews()
        pointList.clear()
        for (index in 0 until size) {
            val v = LinearLayout(context)
            val lp: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            if (index != 0) {
                lp.leftMargin = resources.getDimensionPixelSize(R.dimen.dp_7)
            }
            pointList.add(v)
            point_layout.addView(v, lp)
        }
        setPointPosition(0)
    }


    private fun setPointPosition(position: Int) {
        val pointSize = pointList.size
        if (pointSize == 0) {
            return
        }
        val pointPosition = position % pointSize
        if (pointSize > 0 && pointPosition < pointSize) {
            for (v in pointList) {
                val unSelectedLayoutParams = v.layoutParams
                unSelectedLayoutParams.width = pointUnselectedWidth
                unSelectedLayoutParams.height = pointHeight
                v.layoutParams = unSelectedLayoutParams
                v.setBackgroundResource(R.drawable.shape_bg_ad_unselected)
            }
            val selectedView = pointList[pointPosition]
            val selectedLayoutParams = selectedView.layoutParams
            selectedLayoutParams.width = pointSelectedWidth
            selectedLayoutParams.height = pointHeight
            selectedView.layoutParams = selectedLayoutParams
            selectedView.setBackgroundResource(R.drawable.shape_bg_ad_selected)
        }

    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAutoPlay()
        handler.removeCallbacksAndMessages(null)
    }
}