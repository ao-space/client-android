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

package xyz.eulix.space;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.bean.BoxGenerationShowBean;
import xyz.eulix.space.bean.LanDeviceInfoBean;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.Logger;

/**
 * Author:      Zhu Fuyu
 * Description: 局域网设备Adapter
 * History:     2023/3/17
 */
public class LanDeviceSearchAdapter extends RecyclerView.Adapter {
    private Context mContext;
    public List<LanDeviceInfoBean> dataList = new ArrayList<>();
    private OnItemClickListener mListener;

    public LanDeviceSearchAdapter(Context context) {
        this.mContext = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.lan_device_info_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position > dataList.size() - 1) {
            return;
        }
        ViewHolder viewHolder = (ViewHolder) holder;
        LanDeviceInfoBean item = dataList.get(position);

        BoxGenerationShowBean boxGenerationShowBean = new BoxGenerationShowBean(mContext.getResources().getString(R.string.device_server_name), R.drawable.eulix_box_2x);
        if (item.devicemodel != null) {
            int modelNum = 0;
            try {
                modelNum = Integer.valueOf(item.devicemodel);
            } catch (Exception e) {
                Logger.d(e.getMessage());
            }
            boxGenerationShowBean = DataUtil.generationBoxGenerationShowBean(mContext, modelNum, boxGenerationShowBean);
        }
        viewHolder.tvDeviceName.setText(boxGenerationShowBean.getBoxName());
        viewHolder.imgDevice.setImageResource(boxGenerationShowBean.getBoxResId());

        viewHolder.tvIpAddress.setText("IP：" + item.ipAddress);

        viewHolder.btnConfirm.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(position, item);
            }
        });

    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvDeviceName;
        public TextView tvIpAddress;
        public ImageView imgDevice;
        public TextView btnConfirm;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            tvIpAddress = itemView.findViewById(R.id.tv_ip_address);
            imgDevice = itemView.findViewById(R.id.img_device);
            btnConfirm = itemView.findViewById(R.id.btn_confirm);
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(int position, LanDeviceInfoBean lanDeviceInfoBean);
    }
}
