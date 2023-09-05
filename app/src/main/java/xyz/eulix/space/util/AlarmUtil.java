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

package xyz.eulix.space.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/10/25 14:28
 */
public class AlarmUtil {
    public static final String ALARM_ACTION = "xyz.eulix.space.action.ALARM";
    private static int alarmId;

    private AlarmUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public synchronized static int setAlarm(@NonNull Context context, long timestamp, int id, String name, String role, long windowLengthMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return -1;
        }
        Intent intent = new Intent(ALARM_ACTION);
        intent.putExtra("id", id);
        intent.putExtra("time", timestamp);
        intent.putExtra("name", name);
        intent.putExtra("role", role);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        if (timestamp > System.currentTimeMillis()) {
            if (windowLengthMillis > 0) {
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, timestamp, windowLengthMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timestamp, pendingIntent);
            }
        } else {
            return 0;
        }
        return id;
    }

    public static void cancelAlarm(@NonNull Context context, int id) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent intent = new Intent(ALARM_ACTION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            try {
                alarmManager.cancel(pendingIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static int getAlarmId() {
        if (alarmId < Integer.MAX_VALUE) {
            alarmId += 1;
        } else {
            alarmId = 1;
        }
        return alarmId;
    }
}
