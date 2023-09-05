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

import java.util.Map;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/3/30 15:00
 */
public class HighlightString {
    private String content;
    private Map<Integer, Integer> highlightIndexMap;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<Integer, Integer> getHighlightIndexMap() {
        return highlightIndexMap;
    }

    public void setHighlightIndexMap(Map<Integer, Integer> highlightIndexMap) {
        this.highlightIndexMap = highlightIndexMap;
    }
}
