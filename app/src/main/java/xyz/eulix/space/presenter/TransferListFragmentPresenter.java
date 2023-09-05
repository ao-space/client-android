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

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.transfer.model.TransferItem;
import xyz.eulix.space.util.Logger;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2021/8/13
 */
public class TransferListFragmentPresenter extends AbsPresenter<TransferListFragmentPresenter.ITransferListFragment> {
    public List<TransferItem> dataDoingSelected = new ArrayList<>();
    public List<TransferItem> dataDoneSelected = new ArrayList<>();
    public List<Integer> selectedPositionList = new ArrayList<>();

    public interface ITransferListFragment extends IBaseView {
        void refreshSelectedCount(int selectedCount);
    }

    //刷新选中列表数据
    public void refreshDoingSelectedData(TransferItem item, boolean isSelected) {
        if (isSelected) {
            //新增选中
            boolean hasAdded = false;
            for (int i = 0; i < dataDoingSelected.size(); i++) {
                if (dataDoingSelected.get(i).keyName.equals(item.keyName)){
                    hasAdded = true;
                    break;
                }
            }
            if (!hasAdded){
                dataDoingSelected.add(item);
            }
        } else {
            //删除选中
            for (int i = 0; i < dataDoingSelected.size(); i++) {
                if (dataDoingSelected.get(i).keyName.equals(item.keyName)) {
                    dataDoingSelected.remove(i);
                    i--;
                }
            }
        }
        iView.refreshSelectedCount(dataDoingSelected.size());
    }

    //刷新选中列表数据
    public void refreshDoneSelectedData(TransferItem item, boolean isSelected, int position) {
        if (isSelected) {
            //新增选中
            boolean hasAdded = false;
            for (int i = 0; i < dataDoneSelected.size(); i++) {
                if (dataDoneSelected.get(i).keyName.equals(item.keyName)){
                    hasAdded = true;
                    break;
                }
            }
            if (!hasAdded){
                dataDoneSelected.add(item);
                selectedPositionList.add(position);
            }
        } else {
            //删除选中
            for (int i = 0; i < dataDoneSelected.size(); i++) {
                if (dataDoneSelected.get(i).keyName.equals(item.keyName)) {
                    Logger.d("zfy","delete done item");
                    dataDoneSelected.remove(i);
                    selectedPositionList.remove(i);
                    break;
                }
            }
        }
        iView.refreshSelectedCount(dataDoneSelected.size());
    }

}
