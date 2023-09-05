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

package xyz.eulix.space.bean;

import java.util.List;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/1/14 18:31
 */
public class FileSearchData {
    private String searchContent;
    private int searchScroll;
    private List<CustomizeFile> customizeFiles;

    public String getSearchContent() {
        return searchContent;
    }

    public void setSearchContent(String searchContent) {
        this.searchContent = searchContent;
    }

    public int getSearchScroll() {
        return searchScroll;
    }

    public void setSearchScroll(int searchScroll) {
        this.searchScroll = searchScroll;
    }

    public List<CustomizeFile> getCustomizeFiles() {
        return customizeFiles;
    }

    public void setCustomizeFiles(List<CustomizeFile> customizeFiles) {
        this.customizeFiles = customizeFiles;
    }
}
