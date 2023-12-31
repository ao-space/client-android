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

package xyz.eulix.space.receiver;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import androidx.annotation.Nullable;

import xyz.eulix.space.util.Logger;

/**
 * Author:      Zhu Fuyu
 * Description: 图库变化观察类
 * History:     2021/10/18
 */
public class PhotoAlbumContentObserver extends ContentObserver {

    private OnChangeListener onChangeListener;

    public PhotoAlbumContentObserver(Handler handler) {
        super(handler);
    }

    @Override
    public void onChange(boolean selfChange, @Nullable Uri uri) {
        super.onChange(selfChange, uri);
        if (uri != null && (uri.toString().contains("images") || uri.toString().contains("video"))) {
            Logger.d("zfy", "photo album onChange：Uri = " + uri.toString());
            if (onChangeListener != null) {
                onChangeListener.onChange(uri);
            }
        }
    }

    public void setOnChangeListener(OnChangeListener onChangeListener) {
        this.onChangeListener = onChangeListener;
    }

    public interface OnChangeListener {
        void onChange(Uri uri);
    }
}
