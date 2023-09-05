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

package xyz.eulix.space.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.bean.DeviceVersionInfoBean;

/**
 * Author:      Zhu Fuyu
 * Description: 系统规格-服务 适配器
 * History:     2021/7/19
 */
public class DockerServiceAdapter extends RecyclerView.Adapter {
    private Context mContext;
    public List<DeviceVersionInfoBean.ServiceVersion> dataList = new ArrayList<>();

    public DockerServiceAdapter(Context context) {
        mContext = context;
    }

    @NonNull
    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.layout_item_docker_service_info, parent, false);
        return new SourceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position) {
        if (dataList.isEmpty()) {
            return;
        }
        SourceViewHolder viewHolder = (SourceViewHolder) holder;
        DeviceVersionInfoBean.ServiceVersion item = dataList.get(position);

        viewHolder.tvServiceName.setText(item.serviceName);
        viewHolder.tvVersionInfo.setText(item.version);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class SourceViewHolder extends RecyclerView.ViewHolder {
        public TextView tvServiceName;
        public TextView tvVersionInfo;

        public SourceViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            tvServiceName = itemView.findViewById(R.id.tv_service_name);
            tvVersionInfo = itemView.findViewById(R.id.tv_version_info);
        }
    }
}
