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

package xyz.eulix.space.adapter.bind;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.airbnb.lottie.LottieAnimationView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xyz.eulix.space.R;
import xyz.eulix.space.bean.BoxGenerationShowBean;
import xyz.eulix.space.bean.EulixBoxExtraInfo;
import xyz.eulix.space.bean.LanServiceInfo;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.LottieUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/12/2 18:08
 */
public class LanFindDevicePagerAdapter extends PagerAdapter implements View.OnClickListener {
    private Context mContext;
    private List<LanServiceInfo> mNsdServiceInfoList;
    private EulixBoxExtraInfo mExtraInfo;
    private OnItemClickListener mOnItemClickListener;
    private Map<Integer, View> viewMap = new HashMap<>();

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public LanFindDevicePagerAdapter(Context context, List<LanServiceInfo> nsdServiceInfoList, EulixBoxExtraInfo extraInfo) {
        mContext = context;
        mNsdServiceInfoList = nsdServiceInfoList;
        mExtraInfo = extraInfo;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void updateData(List<LanServiceInfo> nsdServiceInfos) {
        mNsdServiceInfoList = nsdServiceInfos;
        notifyDataSetChanged();
    }

    public void updateView(int position, boolean isBusy) {
        if (position >= 0 && viewMap.containsKey(position)) {
            View view = viewMap.get(position);
            if (view != null) {
                LottieAnimationView loadingAnimation = view.findViewById(R.id.loading_animation);
                TextView content = view.findViewById(R.id.loading_content);
                if (loadingAnimation != null && content != null) {
                    if (isBusy) {
                        content.setText(R.string.binding);
                        loadingAnimation.setVisibility(View.VISIBLE);
                        LottieUtil.loop(loadingAnimation, "loading_button.json");
                    } else {
                        LottieUtil.stop(loadingAnimation);
                        loadingAnimation.setVisibility(View.GONE);
                        content.setText(R.string.confirm_bind);
                    }
                }
            }
        } else if (position == -1) {
            Set<Integer> keySet = viewMap.keySet();
            for (int key : keySet) {
                updateView(key, isBusy);
            }
        }
    }

    @Override
    public int getCount() {
        return (mNsdServiceInfoList == null ? 0 : mNsdServiceInfoList.size());
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return (view == object);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.eulix_device_found_item, null);
        viewMap.put(position, view);
        if (mNsdServiceInfoList != null && mNsdServiceInfoList.size() > position) {
            LanServiceInfo nsdServiceInfo = mNsdServiceInfoList.get(position);
            if (nsdServiceInfo != null) {
                ImageView eulixDeviceImage = view.findViewById(R.id.eulix_device_image);
                TextView name = view.findViewById(R.id.name);
                TextView deviceHint = view.findViewById(R.id.device_hint);
                ImageButton itemExit = view.findViewById(R.id.item_exit);
                LinearLayout loadingButtonContainer = view.findViewById(R.id.loading_button_container);
                boolean isBind = false;
                int deviceModelNumber = 0;
                if (mExtraInfo != null) {
                    isBind = mExtraInfo.isBind();
                    deviceModelNumber = mExtraInfo.getDeviceModelNumber();
                }
                BoxGenerationShowBean boxGenerationShowBean = new BoxGenerationShowBean(mContext.getString(R.string.device_server_name), R.drawable.eulix_box_v3_2x);
                boxGenerationShowBean = DataUtil.generationBoxGenerationShowBean(mContext, deviceModelNumber, boxGenerationShowBean);
                name.setText(boxGenerationShowBean.getBoxName());
                eulixDeviceImage.setImageResource(boxGenerationShowBean.getBoxResId());
                deviceHint.setVisibility(isBind ? View.VISIBLE : View.INVISIBLE);
                loadingButtonContainer.setTag(position);
                updateView(position, false);
                loadingButtonContainer.setOnClickListener(this);
                if (mNsdServiceInfoList.size() <= 1) {
                    itemExit.setVisibility(View.VISIBLE);
                    itemExit.setTag(position);
                    itemExit.setOnClickListener(this);
                } else {
                    itemExit.setVisibility(View.INVISIBLE);
                    itemExit.setClickable(false);
                }
            }
            view.setTag(position);
        }
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (object instanceof View) {
            container.removeView((View) object);
        } else {
            super.destroyItem(container, position, object);
        }
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        int position = (mNsdServiceInfoList == null ? -1 : mNsdServiceInfoList.indexOf(object));
        return (position == -1 ? POSITION_NONE : position);
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            Object positionTag = v.getTag();
            if (positionTag instanceof Integer) {
                int position = (int) positionTag;
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(v, position);
                }
            }
        }
    }

}
