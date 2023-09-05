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

package xyz.eulix.space.adapter.files;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.List;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/7/20 17:12
 */
public class FilePagerAdapter extends FragmentStatePagerAdapter {
    private List<Fragment> fragments;
    private List<String> titles;

    public FilePagerAdapter(@NonNull FragmentManager fm, List<Fragment> fragmentList, List<String> titleList) {
        this(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, fragmentList, titleList);
    }

    public FilePagerAdapter(@NonNull FragmentManager fm, int behavior, List<Fragment> fragmentList, List<String> titleList) {
        super(fm, behavior);
        fragments = fragmentList;
        titles = titleList;
    }


    @NonNull
    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return titles.get(position);
    }
}
