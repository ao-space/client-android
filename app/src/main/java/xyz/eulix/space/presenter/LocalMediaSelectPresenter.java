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

package xyz.eulix.space.presenter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.ArrayStack;
import xyz.eulix.space.bean.LocalMediaUpItem;
import xyz.eulix.space.util.DataUtil;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2021/7/22
 */
public class LocalMediaSelectPresenter extends AbsPresenter<LocalMediaSelectPresenter.ILocalMediaSelect> {
    private ArrayStack<UUID> uuids;
    //已选中项目
    public List<LocalMediaUpItem> dataSelected = new ArrayList<>();

    public interface ILocalMediaSelect extends IBaseView {
        void refreshShowCount(int selectedCount);
    }

    //刷新选中列表数据
    public void refreshSelectedData(LocalMediaUpItem item, boolean isSelected) {
        if (isSelected) {
            //新增选中
            if (!dataSelected.contains(item)) {
                dataSelected.add(item);
            }
        } else {
            //删除选中
            for (int i = 0; i < dataSelected.size(); i++) {
                if (dataSelected.get(i).getMediaId().equals(item.getMediaId())) {
                    dataSelected.remove(i);
//                    i--;
                    break;
                }
            }
        }
        iView.refreshShowCount(dataSelected.size());
    }

    public ArrayStack<UUID> getUuids() {
        return uuids;
    }

    public void setUuids(ArrayStack<UUID> uuids) {
        this.uuids = DataUtil.cloneUUIDStack(uuids);
    }
}
