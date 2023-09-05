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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import xyz.eulix.space.R
import xyz.eulix.space.event.CloseBannerEvent
import xyz.eulix.space.ui.EulixImageActivity
import xyz.eulix.space.ui.EulixWebViewActivity
import xyz.eulix.space.util.ConstantField
import xyz.eulix.space.util.DebugUtil
import xyz.eulix.space.util.EventBusUtil

/**
 * Author:      Zhu Fuyu
 * Description: 自动滚动横幅适配器
 * History:     2021/11/15
 */
class AutoScrollBannerAdapter constructor(
    context: Context,
    bannerType: Int,
    itemNum: Int,
    views: ArrayList<AdContentBean>
) : PagerAdapter() {


    private val list: List<AdContentBean> = views
    private val itemNum: Int = itemNum
    private val mContext: Context = context
    private val mBannerType: Int = bannerType;

    override fun getCount(): Int {
        return itemNum
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        var root = if (mBannerType == AutoScrollBanner.TYPE_ALBUM_HISTORY) {
            LayoutInflater.from(mContext).inflate(R.layout.ad_single_layout_album, container, false)
        } else {
            LayoutInflater.from(mContext).inflate(R.layout.ad_single_layout, container, false)
        }
        var imageView: ImageView = root.findViewById(R.id.ad_pic)
        var tvTitle: TextView = root.findViewById(R.id.tv_title)
        var imgClose: ImageView = root.findViewById(R.id.img_close);
        var listSize: Int = list.size
        var listPosition: Int = position % listSize
        if (listPosition < listSize) {
            var bean: AdContentBean = list.get(listPosition)

            if (bean.imgResId > 0) {
//                GlideUtil.load(bean.imgUrl, imageView)
                imageView.setImageResource(bean.imgResId);
            }
            imageView.setOnClickListener {
                if (bean.webUrl != null) {
                    if (bean.webUrl.equals(ConstantField.URL.SERVER_GUIDE_API) || bean.webUrl.equals(
                            ConstantField.URL.OTHER_GUIDE_API
                        )
                    ) {
                        EulixImageActivity.startImage(
                            mContext,
                            null,
                            bean.imageAssertName,
                            true
                        )
                    } else if (bean.webUrl.equals(ConstantField.URL.SERVER_LOCAL_API) || bean.webUrl.equals(
                            ConstantField.URL.OTHER_LOCAL_NETWORK_API
                        )
                    ) {
                        EulixImageActivity.startImage(
                            mContext,
                            null,
                            bean.imageAssertName,
                            true
                        )
                    } else {
                        val url = DebugUtil.getOfficialEnvironmentWeb() + bean.webUrl;
                        EulixWebViewActivity.startWeb(mContext, bean.titleName, url)
                    }
                }
            }
            imgClose.setOnClickListener {
                //关闭
                EventBusUtil.post(CloseBannerEvent(bean.adId))
            }

        }

        var viewPager: ViewPager = container as ViewPager
        viewPager.addView(root)
        return root
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}