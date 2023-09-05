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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.bean.CustomizeFile;
import xyz.eulix.space.fragment.ImgPreviewFragment;
import xyz.eulix.space.transfer.TransferHelper;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2022/1/11
 */
public class ImgPreviewAdapter extends FragmentStateAdapter {
    public List<CustomizeFile> dataList = new ArrayList<>();
    public String from = TransferHelper.FROM_FILE;

    public ImgPreviewAdapter(@NonNull @NotNull FragmentActivity fragmentActivity, String from) {
        super(fragmentActivity);
        this.from = from;
    }

    @NonNull
    @NotNull
    @Override
    public Fragment createFragment(int position) {
        return ImgPreviewFragment.newInstance(dataList.get(position), from);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

}
