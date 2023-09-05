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

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import okhttp3.HttpUrl;
import xyz.eulix.space.bean.HighlightString;
import xyz.eulix.space.bean.LocaleBean;
import xyz.eulix.space.bean.bind.InitResponseNetwork;

/**
 * @author: chenjiawei
 * date: 2021/6/1 14:52
 */
public class FormatUtil {
    private static final String GMT = "GMT";
    private static final long DAY_MILLIS = 86400000L;

    private FormatUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static Comparator<InitResponseNetwork> wireFirstComparator = (o1, o2) -> {
        if (o1 == null || o2 == null) {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return 1;
            } else {
                return -1;
            }
        } else {
            boolean isWire1 = o1.isWire();
            boolean isWire2 = o2.isWire();
            if (isWire1 == isWire2) {
                return 0;
            } else {
                return (isWire1 ? -1 : 1);
            }
        }
    };

    public static Comparator<InitResponseNetwork> wifiFirstComparator = (o1, o2) -> {
        if (o1 == null || o2 == null) {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return 1;
            } else {
                return -1;
            }
        } else {
            boolean isWire1 = o1.isWire();
            boolean isWire2 = o2.isWire();
            if (isWire1 == isWire2) {
                return 0;
            } else {
                return (isWire1 ? 1 : -1);
            }
        }
    };

    public static Map<String, String> formatQueryParams(String url) {
        Map<String, String> queryMap = null;
        if (url != null) {
            int index = url.indexOf("?");
            if (index >= 0 && index + 1 < url.length()) {
                url = url.substring(index + 1);
                String[] queryArray = url.split("&");
                if (queryArray.length > 0) {
                    queryMap = new HashMap<>();
                    for (String query : queryArray) {
                        if (query != null) {
                            int nIndex = query.indexOf("=");
                            if (nIndex >= 1 && nIndex < query.length()) {
                                String value = "";
                                if (nIndex < query.length() - 1) {
                                    value = query.substring(nIndex + 1);
                                }
                                queryMap.put(query.substring(0, nIndex), value);
                            }
                        }
                    }
                }
            }
        }
        return queryMap;
    }

    public static String formatSize(long size, String format) {
        return formatSize(size, format, 1024);
    }

    public static String formatSize(long size, String format, int unitSize) {
        String result = "";
        if (size > 0 && unitSize > 0) {
            String unit = ConstantField.SizeUnit.BYTE;
            double nSize = size * 1.0;
            while (nSize > 999.999999 && !unit.equalsIgnoreCase(ConstantField.SizeUnit.TERA_BYTE)) {
                nSize = (nSize / unitSize);
                switch (unit) {
                    case ConstantField.SizeUnit.BYTE:
                        unit = ConstantField.SizeUnit.KILO_BYTE;
                        break;
                    case ConstantField.SizeUnit.KILO_BYTE:
                        unit = ConstantField.SizeUnit.MEGA_BYTE;
                        break;
                    case ConstantField.SizeUnit.MEGA_BYTE:
                        unit = ConstantField.SizeUnit.GIGA_BYTE;
                        break;
                    case ConstantField.SizeUnit.GIGA_BYTE:
                        unit = ConstantField.SizeUnit.TERA_BYTE;
                        break;
                    case ConstantField.SizeUnit.TERA_BYTE:
                        break;
                    default:
                        break;
                }
            }
            try {
                result = String.format(format, nSize) + unit;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            result = "0" + ConstantField.SizeUnit.BYTE;
        }
        return result;
    }

    public static String formatSize(double size, String format) {
        return formatSize(size, format, 1024);
    }

    public static String formatSize(double size, String format, int unitSize) {
        String result = "";
        if (size > 0 && unitSize > 0) {
            String unit = ConstantField.SizeUnit.BYTE_PER_SECOND;
            double nSize = size;
            while (nSize > 999.999999 && !unit.equalsIgnoreCase(ConstantField.SizeUnit.TERA_BYTE_PER_SECOND)) {
                nSize = (nSize / unitSize);
                switch (unit) {
                    case ConstantField.SizeUnit.BYTE_PER_SECOND:
                        unit = ConstantField.SizeUnit.KILO_BYTE_PER_SECOND;
                        break;
                    case ConstantField.SizeUnit.KILO_BYTE_PER_SECOND:
                        unit = ConstantField.SizeUnit.MEGA_BYTE_PER_SECOND;
                        break;
                    case ConstantField.SizeUnit.MEGA_BYTE_PER_SECOND:
                        unit = ConstantField.SizeUnit.GIGA_BYTE_PER_SECOND;
                        break;
                    case ConstantField.SizeUnit.GIGA_BYTE_PER_SECOND:
                        unit = ConstantField.SizeUnit.TERA_BYTE_PER_SECOND;
                        break;
                    case ConstantField.SizeUnit.TERA_BYTE_PER_SECOND:
                        break;
                    default:
                        break;
                }
            }
            try {
                result = String.format(format, nSize) + unit;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            result = "0" + ConstantField.SizeUnit.BYTE_PER_SECOND;
        }
        return result;
    }

    public static String formatSimpleSize(long size, String format) {
        return formatSimpleSize(size, format, 1024);
    }

    public static String formatSimpleSize(long size, String format, int unitSize) {
        String result = "";
        if (size > 0 && unitSize > 0) {
            String unit = ConstantField.SizeUnit.BYTE;
            double nSize = size * 1.0;
            while (nSize > 999.999999 && !unit.equalsIgnoreCase(ConstantField.SizeUnit.TERA_BYTE_SIMPLE)) {
                nSize = (nSize / unitSize);
                switch (unit) {
                    case ConstantField.SizeUnit.BYTE:
                        unit = ConstantField.SizeUnit.KILO_BYTE_SIMPLE;
                        break;
                    case ConstantField.SizeUnit.KILO_BYTE_SIMPLE:
                        unit = ConstantField.SizeUnit.MEGA_BYTE_SIMPLE;
                        break;
                    case ConstantField.SizeUnit.MEGA_BYTE_SIMPLE:
                        unit = ConstantField.SizeUnit.GIGA_BYTE_SIMPLE;
                        break;
                    case ConstantField.SizeUnit.GIGA_BYTE_SIMPLE:
                        unit = ConstantField.SizeUnit.TERA_BYTE_SIMPLE;
                        break;
                    case ConstantField.SizeUnit.TERA_BYTE_SIMPLE:
                        break;
                    default:
                        break;
                }
            }
            try {
                result = String.format(format, nSize) + unit;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            result = "0" + ConstantField.SizeUnit.BYTE;
        }
        return result;
    }

    public static String formatTime(long timestamp, String format) {
        if (timestamp < 0) {
            return "";
        } else {
            return new SimpleDateFormat(format).format(new Date(timestamp));
        }
    }

    //转换2022-01-01样式日期为其他样式
    public static String formatDateStr(String dateStr, String format) {
        SimpleDateFormat dayFormat = new SimpleDateFormat(ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT);
        try {
            Date date = dayFormat.parse(dateStr);
            if (date != null) {
                return new SimpleDateFormat(format).format(date);
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    //转换2022-01-01样式日期为时间戳
    public static long dateToMillis(String dateStr) {
        Date date = null;
        try {
            date = new SimpleDateFormat(ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT).parse(dateStr);
            return date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getTimeSecond() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.SECOND);
    }

    public static String formatTime(long timestamp, String format, String todayFormat) {
        if (timestamp < 0) {
            return "";
        } else {
            Calendar calendar = Calendar.getInstance();
            int currentYear = calendar.get(Calendar.YEAR);
            int currentMonth = calendar.get(Calendar.MONTH);
            int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
            calendar.setTimeInMillis(timestamp);
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            return new SimpleDateFormat((year == currentYear && month == currentMonth
                    && day == currentDay) ? todayFormat : format).format(new Date(timestamp));
        }
    }

    @NonNull
    public static int[] getTimestampValue(long timestamp) {
        int[] values = new int[7];
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        values[0] = calendar.get(Calendar.YEAR);
        values[1] = calendar.get(Calendar.MONTH);
        values[2] = calendar.get(Calendar.DAY_OF_MONTH);
        values[3] = calendar.get(Calendar.HOUR_OF_DAY);
        values[4] = calendar.get(Calendar.MINUTE);
        values[5] = calendar.get(Calendar.SECOND);
        values[6] = calendar.get(Calendar.MILLISECOND);
        return values;
    }

    @NonNull
    public static String getEnMonth(int calendarMonth) {
        String month = "Und";
        switch (calendarMonth) {
            case Calendar.JANUARY:
                month = "Jan";
                break;
            case Calendar.FEBRUARY:
                month = "Feb";
                break;
            case Calendar.MARCH:
                month = "Mar";
                break;
            case Calendar.APRIL:
                month = "Apr";
                break;
            case Calendar.MAY:
                month = "May";
                break;
            case Calendar.JUNE:
                month = "Jun";
                break;
            case Calendar.JULY:
                month = "Jul";
                break;
            case Calendar.AUGUST:
                month = "Aug";
                break;
            case Calendar.SEPTEMBER:
                month = "Sep";
                break;
            case Calendar.OCTOBER:
                month = "Oct";
                break;
            case Calendar.NOVEMBER:
                month = "Nov";
                break;
            case Calendar.DECEMBER:
                month = "Dec";
                break;
            default:
                break;
        }
        return month;
    }

    public static String getEnDay(int calendarDay) {
        StringBuilder dayBuilder = new StringBuilder();
        dayBuilder.append(calendarDay);
        int day2 = (((Math.abs(calendarDay)) % 100) / 10);
        int day1 = ((Math.abs(calendarDay)) % 10);
        switch (day2) {
            case 1:
                dayBuilder.append("th");
                break;
            default:
                switch (day1) {
                    case 1:
                        dayBuilder.append("st");
                        break;
                    case 2:
                        dayBuilder.append("nd");
                        break;
                    case 3:
                        dayBuilder.append("rd");
                        break;
                    default:
                        dayBuilder.append("th");
                        break;
                }
                break;
        }
        return dayBuilder.toString();
    }

    public static int getTimestampYear(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return calendar.get(Calendar.YEAR);
    }

    public static long generateDayZeroTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long generateDayZeroTime(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long generateYearZeroTime(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static int dayOfDiff(long diffTimestamp, int limit) {
        return dayOfDiff(diffTimestamp, System.currentTimeMillis(), limit);
    }

    public static int dayOfDiff(long diffTimestamp, long standardTimestamp, int limit) {
        int diffDay = 0;
        if (diffTimestamp < standardTimestamp) {
            while ((diffTimestamp < standardTimestamp) && (limit > Math.abs(diffDay))) {
                diffDay -= 1;
                standardTimestamp -= DAY_MILLIS;
            }
        } else if (diffTimestamp > standardTimestamp) {
            diffDay = -1;
            while ((diffTimestamp > standardTimestamp) && (limit > Math.abs(diffDay))) {
                diffDay += 1;
                standardTimestamp += DAY_MILLIS;
            }
        }
        return diffDay;
    }

    public static long parseTimestamp(String dates, String format) {
        long timestamp = -1L;
        if (!TextUtils.isEmpty(dates) && !TextUtils.isEmpty(format)) {
            Calendar calendar = Calendar.getInstance();
            Date date = null;
            try {
                date = new SimpleDateFormat(format).parse(dates);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (date != null) {
                calendar.setTime(date);
                timestamp = calendar.getTimeInMillis();
            }
        }
        return timestamp;
    }

    /**
     * 将ZonedDateTime转换为时间戳
     *
     * @param date 格式如2022-08-17T16:34:50.855591+08:00[Asia/Shanghai]
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static long parseZonedDateTime(String date) {
        long timestamp = -1L;
        if (date != null && !TextUtils.isEmpty(date)) {
            ZonedDateTime zonedDateTime = null;
            try {
                zonedDateTime = ZonedDateTime.parse(date);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (zonedDateTime != null) {
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(zonedDateTime.getZone()));
                calendar.set(Calendar.YEAR, zonedDateTime.getYear());
                calendar.set(Calendar.MONTH, zonedToCalendarMonth(zonedDateTime.getMonthValue()));
                calendar.set(Calendar.DAY_OF_MONTH, zonedDateTime.getDayOfMonth());
                calendar.set(Calendar.HOUR_OF_DAY, zonedDateTime.getHour());
                calendar.set(Calendar.MINUTE, zonedDateTime.getMinute());
                calendar.set(Calendar.SECOND, zonedDateTime.getSecond());
                calendar.set(Calendar.MILLISECOND, (Math.max(Math.min((zonedDateTime.getNano() / 1000000), 999), 0)));
                timestamp = calendar.getTimeInMillis();
            }
        }
        return timestamp;
    }

    private static int zonedToCalendarMonth(int zonedMonth) {
        int calendarMonth = Calendar.JANUARY;
        switch (zonedMonth) {
            case 1:
                calendarMonth = Calendar.JANUARY;
                break;
            case 2:
                calendarMonth = Calendar.FEBRUARY;
                break;
            case 3:
                calendarMonth = Calendar.MARCH;
                break;
            case 4:
                calendarMonth = Calendar.APRIL;
                break;
            case 5:
                calendarMonth = Calendar.MAY;
                break;
            case 6:
                calendarMonth = Calendar.JUNE;
                break;
            case 7:
                calendarMonth = Calendar.JULY;
                break;
            case 8:
                calendarMonth = Calendar.AUGUST;
                break;
            case 9:
                calendarMonth = Calendar.SEPTEMBER;
                break;
            case 10:
                calendarMonth = Calendar.OCTOBER;
                break;
            case 11:
                calendarMonth = Calendar.NOVEMBER;
                break;
            case 12:
                calendarMonth = Calendar.DECEMBER;
                break;
            default:
                break;
        }
        return calendarMonth;
    }

    /**
     * @param newDate
     * @param oldDate
     * @param equalNew 完全相等返回true(TRUE)，否则false(FALSE)
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Boolean compareZonedDateTime(String newDate, String oldDate, boolean equalNew) {
        return compareZonedDateTime(newDate, oldDate, null, equalNew);
    }

    /**
     * @param newDate
     * @param oldDate
     * @param equalNew 完全相等返回true(TRUE)，否则false(FALSE)
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Boolean compareZonedDateTime(String newDate, String oldDate, DateTimeFormatter dateTimeFormatter, boolean equalNew) {
        Boolean isNewer = null;
        if (newDate != null && oldDate != null) {
            ZonedDateTime newZonedDateTime = null;
            ZonedDateTime oldZonedDateTime = null;
            try {
                if (dateTimeFormatter == null) {
                    newZonedDateTime = ZonedDateTime.parse(newDate);
                    oldZonedDateTime = ZonedDateTime.parse(oldDate);
                } else {
                    newZonedDateTime = ZonedDateTime.parse(newDate, dateTimeFormatter);
                    oldZonedDateTime = ZonedDateTime.parse(oldDate, dateTimeFormatter);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (newZonedDateTime != null && oldZonedDateTime != null) {
                isNewer = equalNew;
                int[] newTimes = new int[]{newZonedDateTime.getYear(), newZonedDateTime.getMonthValue(), newZonedDateTime.getDayOfMonth()
                        , newZonedDateTime.getHour(), newZonedDateTime.getMinute(), newZonedDateTime.getSecond(), newZonedDateTime.getNano()};
                int[] oldTimes = new int[]{oldZonedDateTime.getYear(), oldZonedDateTime.getMonthValue(), oldZonedDateTime.getDayOfMonth()
                        , oldZonedDateTime.getHour(), oldZonedDateTime.getMinute(), oldZonedDateTime.getSecond(), oldZonedDateTime.getNano()};
                for (int i = 0; i < 7; i++) {
                    int newTime = newTimes[i];
                    int oldTime = oldTimes[i];
                    if (newTime != oldTime) {
                        isNewer = (newTime > oldTime);
                        break;
                    }
                }
            } else if (newZonedDateTime != null) {
                isNewer = true;
            }
        } else if (newDate != null) {
            isNewer = true;
        }
        return isNewer;
    }

    public static long parseFileApiTimestamp(String dates, String dayFormat, String split) {
        long timestamp = -1L;
        if (dates != null && !TextUtils.isEmpty(dates) && !TextUtils.isEmpty(dayFormat) && !TextUtils.isEmpty(split)) {
            int splitIndex = dates.indexOf(split);
            int length = dates.length();
            if (splitIndex > 0 && (splitIndex + 1) < length) {
                String dayDates = dates.substring(0, splitIndex);
                String millisecondDates = dates.substring(splitIndex + 1);
                Calendar calendar = Calendar.getInstance();
                Date date = null;
                try {
                    date = new SimpleDateFormat(dayFormat).parse(dayDates);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if (date != null) {
                    calendar.setTime(date);
                    if (!TextUtils.isEmpty(millisecondDates)) {
                        String[] millisecondDate = millisecondDates.split(":");
                        int millisecondDateLength = millisecondDate.length;
                        if (millisecondDateLength > 0) {
                            int hour = -1;
                            try {
                                hour = Integer.parseInt(millisecondDate[0]);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                            if (hour >= 0 && hour < 24) {
                                calendar.set(Calendar.HOUR_OF_DAY, hour);
                                if (millisecondDateLength > 1) {
                                    int minute = -1;
                                    try {
                                        minute = Integer.parseInt(millisecondDate[1]);
                                    } catch (NumberFormatException e) {
                                        e.printStackTrace();
                                    }
                                    if (minute >= 0 && minute < 60) {
                                        calendar.set(Calendar.MINUTE, minute);
                                        if (millisecondDateLength > 2) {
                                            int second = -1;
                                            String millisecondsValue = millisecondDate[2];
                                            int secondSplitIndex = millisecondsValue.indexOf(".");
                                            int millisecondValueLength = millisecondsValue.length();
                                            if (secondSplitIndex > 0 && secondSplitIndex < millisecondValueLength) {
                                                try {
                                                    second = Integer.parseInt(millisecondsValue.substring(0, secondSplitIndex));
                                                } catch (NumberFormatException e) {
                                                    e.printStackTrace();
                                                }
                                            } else if (secondSplitIndex != 0 && millisecondsValue.length() > 1) {
                                                try {
                                                    second = Integer.parseInt(millisecondsValue.substring(0, 2));
                                                } catch (NumberFormatException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            if (second >= 0 && second < 60) {
                                                calendar.set(Calendar.SECOND, second);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    timestamp = calendar.getTimeInMillis();
                }
            }
            int offset = (Calendar.getInstance().get(Calendar.ZONE_OFFSET) / 1000 / 60 / 60);
            if (dates.contains(GMT)) {
                int gmtIndex = dates.indexOf(GMT);
                if (gmtIndex >= 0 && (gmtIndex + GMT.length() + 2) < dates.length()) {
                    String gmtSub = dates.substring((gmtIndex + GMT.length()));
                    if (gmtSub.startsWith("+") || gmtSub.startsWith("-")) {
                        if (gmtSub.length() > 2) {
                            char c1 = gmtSub.charAt(1);
                            char c2 = 't';
                            if (gmtSub.length() > 3) {
                                c2 = gmtSub.charAt(2);
                            }
                            int i = 0;
                            int i1 = c1 - '0';
                            int i2 = c2 - '0';
                            if (i1 < 10) {
                                i = i1;
                                if (i2 < 10) {
                                    i = i * 10 + i2;
                                }
                            }
                            if (gmtSub.startsWith("-")) {
                                i = (-1) * i;
                            }
                            offset = offset - i;
                        }
                    }
                }
            }
            timestamp += (offset * 60 * 60 * 1000);
        }
        return timestamp;
    }

    public static long parseZonedTimestamp(String dates, String dayFormat, String split) {
        long timestamp = -1L;
        if (dates != null && !TextUtils.isEmpty(dates) && !TextUtils.isEmpty(dayFormat) && !TextUtils.isEmpty(split)) {
            int splitIndex = dates.indexOf(split);
            int length = dates.length();
            if (splitIndex > 0 && (splitIndex + 1) < length) {
                String dayDates = dates.substring(0, splitIndex);
                String millisecondDates = dates.substring(splitIndex + 1);
                Calendar calendar = Calendar.getInstance();
                Date date = null;
                try {
                    date = new SimpleDateFormat(dayFormat).parse(dayDates);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if (date != null) {
                    calendar.setTime(date);
                    if (!TextUtils.isEmpty(millisecondDates)) {
                        String[] millisecondDate = millisecondDates.split(":");
                        int millisecondDateLength = millisecondDate.length;
                        if (millisecondDateLength > 0) {
                            int hour = -1;
                            try {
                                hour = Integer.parseInt(millisecondDate[0]);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                            if (hour >= 0 && hour < 24) {
                                calendar.set(Calendar.HOUR_OF_DAY, hour);
                                if (millisecondDateLength > 1) {
                                    int minute = -1;
                                    try {
                                        minute = Integer.parseInt(millisecondDate[1]);
                                    } catch (NumberFormatException e) {
                                        e.printStackTrace();
                                    }
                                    if (minute >= 0 && minute < 60) {
                                        calendar.set(Calendar.MINUTE, minute);
                                        if (millisecondDateLength > 2) {
                                            int second = -1;
                                            String millisecondsValue = millisecondDate[2];
                                            int secondSplitIndex = millisecondsValue.indexOf(".");
                                            int millisecondValueLength = millisecondsValue.length();
                                            if (secondSplitIndex > 0 && secondSplitIndex < millisecondValueLength) {
                                                try {
                                                    second = Integer.parseInt(millisecondsValue.substring(0, secondSplitIndex));
                                                } catch (NumberFormatException e) {
                                                    e.printStackTrace();
                                                }
                                            } else if (secondSplitIndex != 0 && millisecondsValue.length() > 1) {
                                                try {
                                                    second = Integer.parseInt(millisecondsValue.substring(0, 2));
                                                } catch (NumberFormatException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            if (second >= 0 && second < 60) {
                                                calendar.set(Calendar.SECOND, second);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    timestamp = calendar.getTimeInMillis();
                }
            }
            int plusIndex = dates.indexOf("+");
            int minusIndex = dates.indexOf("-");
            int zoneIndex = -1;
            if (plusIndex >= 0 && (plusIndex + 2) < dates.length()) {
                zoneIndex = plusIndex;
            } else if (minusIndex >= 0 && (minusIndex + 2) < dates.length()) {
                zoneIndex = minusIndex;
            }
            if (zoneIndex >= 0) {
                int offset = (Calendar.getInstance().get(Calendar.ZONE_OFFSET) / 1000 / 60 / 60);
                String gmtSub = dates.substring(zoneIndex);
                if (gmtSub.startsWith("+") || gmtSub.startsWith("-")) {
                    if (gmtSub.length() > 2) {
                        char c1 = gmtSub.charAt(1);
                        char c2 = 't';
                        if (gmtSub.length() > 3) {
                            c2 = gmtSub.charAt(2);
                        }
                        int i = 0;
                        int i1 = c1 - '0';
                        int i2 = c2 - '0';
                        if (i1 < 10) {
                            i = i1;
                            if (i2 < 10) {
                                i = i * 10 + i2;
                            }
                        }
                        if (gmtSub.startsWith("-")) {
                            i = (-1) * i;
                        }
                        offset = offset - i;
                    }
                }
                timestamp += (offset * 60 * 60 * 1000);
            }
        }
        return timestamp;
    }

    public static String generateHttpUrlString(String domain) {
        String baseUrl = domain;
        if (baseUrl != null) {
            while ((baseUrl.startsWith(":") || baseUrl.startsWith("/")) && baseUrl.length() > 1) {
                baseUrl = baseUrl.substring(1);
            }
            if (!TextUtils.isEmpty(baseUrl)) {
                if (!(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
                    baseUrl = "https://" + baseUrl;
                }
                if (!baseUrl.endsWith("/")) {
                    baseUrl = baseUrl + "/";
                }
            }
        }
        return baseUrl;
    }

    public static String generateHttpImageUrlString(String domain) {
        String baseUrl = domain;
        if (baseUrl != null) {
            while ((baseUrl.startsWith(":") || baseUrl.startsWith("/")) && baseUrl.length() > 1) {
                baseUrl = baseUrl.substring(1);
            }
            if (!TextUtils.isEmpty(baseUrl)) {
                if (!(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
                    baseUrl = "https://" + baseUrl;
                }
                while (baseUrl.endsWith("/")) {
                    int length = baseUrl.length();
                    baseUrl = baseUrl.substring(0, (length - 1));
                }
            }
        }
        return baseUrl;
    }

    public static boolean isHttpUrlString(String url) {
        boolean isHttpUrl = false;
        if (url != null) {
            HttpUrl httpUrl = null;
            try {
                httpUrl = HttpUrl.parse(url);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isHttpUrl = (httpUrl != null);
        }
        return isHttpUrl;
    }

    public static Locale getApplicationLocale(Context context) {
        return getApplicationLocale(context, Locale.getDefault());
    }

    public static Locale getApplicationLocale(Context context, Locale defaultLocale) {
        Locale locale = defaultLocale;
        LocaleBean localeBean = null;
        String localeValue = DataUtil.getApplicationLocale(context);
        if (localeValue != null) {
            try {
                localeBean = new Gson().fromJson(localeValue, LocaleBean.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        if (localeBean != null) {
            locale = localeBean.parseLocale();
        }
        return locale;
    }

    public static SimpleDateFormat generateSimpleDateFormat(@NonNull String pattern, Locale locale) {
        if (locale == null) {
            return new SimpleDateFormat(pattern);
        } else {
            return new SimpleDateFormat(pattern, locale);
        }
    }

    public static LocaleBean getLocale(Context context) {
        LocaleBean localeBean = null;
        if (context != null) {
            Resources resources = context.getResources();
            if (resources != null) {
                Configuration configuration = resources.getConfiguration();
                if (configuration != null) {
                    Locale locale = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        LocaleList localeList = configuration.getLocales();
                        if (localeList.size() > 0) {
                            locale = localeList.get(0);
                        }
                    } else {
                        locale = configuration.locale;
                    }
                    if (locale != null) {
                        localeBean = new LocaleBean();
                        localeBean.setLanguage(locale.getLanguage());
                        localeBean.setCountry(locale.getCountry());
                    }
                }
            }
        }
        return localeBean;
    }

    public static boolean isChinese(Context context, boolean defaultValue) {
        return isChinese(getLocale(context), defaultValue);
    }

    public static boolean isChinese(LocaleBean localeBean, boolean defaultValue) {
        boolean result = defaultValue;
        if (localeBean != null) {
            result = (Locale.CHINESE.getLanguage().equals(localeBean.getLanguage()));
        }
        return result;
    }

    public static boolean isEnglish(Context context, boolean defaultValue) {
        return isEnglish(getLocale(context), defaultValue);
    }

    public static boolean isEnglish(LocaleBean localeBean, boolean defaultValue) {
        boolean result = defaultValue;
        if (localeBean != null) {
            result = (Locale.ENGLISH.getLanguage().equals(localeBean.getLanguage()));
        }
        return result;
    }

    public static long quantification(int inValue, int accuracy, boolean isFloor) {
        long outValue = inValue;
        int quotient = inValue / accuracy;
        int remainder = inValue % accuracy;
        if (remainder != 0) {
            outValue = ((quotient + (isFloor ? 0 : 1)) * accuracy);
        }
        return outValue;
    }

    public static long formatMessageId(String messageId) {
        long messageTimestamp = -1L;
        if (messageId != null) {
            int index = messageId.indexOf("-");
            if (index > 0 && index < messageId.length()) {
                String timestampValue = messageId.substring(0, index);
                try {
                    messageTimestamp = Long.parseLong(timestampValue);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        return messageTimestamp;
    }

    @NonNull
    public static String autoWrap(String content, int textWidth, TextPaint textPaint) {
        StringBuilder wrapBuilder = new StringBuilder();
        if (content != null && textWidth > 0 && textPaint != null) {
            int contentLength = content.length();
            int startIndex = 0;
            while (startIndex < contentLength) {
                int endIndex = contentLength;
                String subContent = content.substring(startIndex, endIndex);
                while (textPaint.measureText(subContent) >= textWidth && endIndex > (startIndex + 1)) {
                    endIndex -= 1;
                    subContent = content.substring(startIndex, endIndex);
                }
                wrapBuilder.append(subContent);
                startIndex = endIndex;
                if (startIndex < contentLength) {
                    wrapBuilder.append("\n");
                }
            }
        } else {
            wrapBuilder.append(content);
        }
        return wrapBuilder.toString();
    }

    /**
     * 针对文件名过长的展示，保留后缀名，文件名称后省略
     *
     * @param content   文件名
     * @param textWidth TextView宽度
     * @param textPaint TextView的画笔
     * @return
     */
    public static String customizeFileEllipsize(String content, int textWidth, TextPaint textPaint) {
        String ellipsizeContent = content;
        if (content != null && textPaint != null) {
            float contentWidth = textPaint.measureText(content);
            if (textWidth < contentWidth) {
                int lastIndexOfPoint = content.lastIndexOf(".");
                if (lastIndexOfPoint >= 0 && lastIndexOfPoint < content.length()) {
                    String prefixContent = content.substring(0, lastIndexOfPoint);
                    String suffixContent = ".." + content.substring(lastIndexOfPoint);
                    float prefixWidth = textPaint.measureText(prefixContent);
                    float suffixWidth = textPaint.measureText(suffixContent);
                    if (suffixWidth < textWidth) {
                        while ((textWidth - prefixWidth) < suffixWidth && prefixContent.length() > 0) {
                            prefixContent = prefixContent.substring(0, (prefixContent.length() - 1));
                            prefixWidth = textPaint.measureText(prefixContent);
                        }
                        ellipsizeContent = (prefixContent + suffixContent);
                    }
                }
            }
        }
        return ellipsizeContent;
    }

    /**
     * @param content
     * @param textWidth
     * @param textPaint
     * @param indexMap
     * @return
     */
    public static HighlightString customizeFileEllipsize(String content, int textWidth, TextPaint textPaint, Map<Integer, Integer> indexMap) {
        HighlightString highlightContent = new HighlightString();
        String ellipsizeContent = content;
        if (content != null && textPaint != null) {
            int currentLength = content.length();
            float contentWidth = textPaint.measureText(content);
            if (textWidth < contentWidth) {
                int lastIndexOfPoint = content.lastIndexOf(".");
                if (lastIndexOfPoint >= 0 && lastIndexOfPoint < content.length()) {
                    String prefixContent = content.substring(0, lastIndexOfPoint);
                    String suffixContent = ".." + content.substring(lastIndexOfPoint);
                    float prefixWidth = textPaint.measureText(prefixContent);
                    float suffixWidth = textPaint.measureText(suffixContent);
                    if (suffixWidth < textWidth) {
                        while ((textWidth - prefixWidth) < suffixWidth && prefixContent.length() > 0) {
                            prefixContent = prefixContent.substring(0, (prefixContent.length() - 1));
                            prefixWidth = textPaint.measureText(prefixContent);
                        }
                        ellipsizeContent = (prefixContent + suffixContent);
                        if (indexMap != null && indexMap.size() > 0) {
                            Map<Integer, Integer> newIndexMap = new HashMap<>();
                            int prefixLength = prefixContent.length();
                            int diffLength = Math.max((currentLength - ellipsizeContent.length()), 0);
                            Set<Map.Entry<Integer, Integer>> entrySet = indexMap.entrySet();
                            for (Map.Entry<Integer, Integer> entry : entrySet) {
                                if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                                    int firstIndex = entry.getKey();
                                    int lastIndex = entry.getValue();
                                    if (firstIndex <= lastIndex) {
                                        int status = 0;
                                        if (!(lastIndex < prefixLength || firstIndex >= lastIndexOfPoint)) {
                                            if (firstIndex >= prefixLength && lastIndex < lastIndexOfPoint) {
                                                status = -1;
                                            } else if (firstIndex < prefixLength && lastIndex >= lastIndexOfPoint) {
                                                status = 2;
                                            } else {
                                                status = 1;
                                            }
                                        }
                                        switch (status) {
                                            case 2:
                                                // 包含省略号
                                                newIndexMap.put(firstIndex, (prefixLength - 1));
                                                newIndexMap.put((lastIndexOfPoint - diffLength), (lastIndex - diffLength));
                                                break;
                                            case 1:
                                                // 一端省略号外，一端省略号内
                                                if (firstIndex >= prefixLength) {
                                                    firstIndex = (lastIndexOfPoint - diffLength);
                                                }
                                                if (lastIndex < lastIndexOfPoint) {
                                                    lastIndex = (prefixLength - 1);
                                                } else {
                                                    lastIndex = (lastIndex - diffLength);
                                                }
                                                if (firstIndex <= lastIndex) {
                                                    newIndexMap.put(firstIndex, lastIndex);
                                                }
                                                break;
                                            case 0:
                                                // 省略号外
                                                if (firstIndex >= lastIndexOfPoint) {
                                                    newIndexMap.put((firstIndex - diffLength), (lastIndex - diffLength));
                                                } else {
                                                    newIndexMap.put(firstIndex, lastIndex);
                                                }
                                                break;
                                            default:
                                                // 省略号内
                                                break;
                                        }
                                    }
                                }
                            }
                            indexMap = newIndexMap;
                        }
                    }
                }
            }
        }
        highlightContent.setContent(ellipsizeContent);
        highlightContent.setHighlightIndexMap(indexMap);
        return highlightContent;
    }

    /**
     * 计算公钥的指纹
     * 去掉公钥的首行和尾行的标识. 中间 base64 部分先base64解码, 然后md5, 最后hexstring.
     *
     * @return String 指纹
     * @author wenchao
     * @date 2021-10-14 22:38:58
     **/
    public static String calRsaKeyFingerprint(String pemKey) {

        StringBuffer res = new StringBuffer();

        // 这里应该可以用正则，但是不同语言可能需要依赖其他库，所以方便集成就自己做处理了。
        String lineEnder = "\r\n";
        String[] arr = pemKey.split(lineEnder);

        if (arr.length <= 1) {
            lineEnder = "\n";
            arr = pemKey.split(lineEnder);
        }
        if (arr.length <= 1) {
            lineEnder = "\r";
            arr = pemKey.split(lineEnder);
        }
        if (arr.length <= 3) {
            return ""; // pem format error!
        }

        if (arr.length > 3) {
            for (Integer i = 1; i < arr.length - 1; i++) {
//                if (i != 1) {
//                    res.append(lineEnder);
//                }

                String s = arr[i];
                if (s.length() > 0 && false == s.contains("KEY")) {
                    res.append(arr[i]);
                }
            }
        }

        //System.out.printf("res:\n%s\n", res);
        byte[] base64decodedBytes = StringUtil.stringToByteArray(String.valueOf(res));
        if (base64decodedBytes == null || base64decodedBytes.length < 1) {
            return "";
        }

        byte[] b = md5(base64decodedBytes);
        if (b == null || b.length < 1) {
            return "";
        }

        return toHexString(b);
    }

    public static byte[] md5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static String toHexString(byte[] md5) {
        StringBuilder sb = new StringBuilder();
        for (byte b : md5) {
            String hexStr = Integer.toHexString(b & 0xff);
            if (hexStr.length() == 1) {
                sb.append(0);
            }
            sb.append(hexStr);
        }
        return sb.toString();
    }

    public static String getSHA256String(String inValue) {
        String outValue = inValue;
        if (inValue != null) {
            MessageDigest messageDigest = null;
            try {
                messageDigest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            if (messageDigest != null) {
                byte[] data = messageDigest.digest(StringUtil.stringToByteArray(inValue, StandardCharsets.UTF_8));
                outValue = StringUtil.byteToHexString(data);
            }
        }
        return outValue;
    }

    //转换ms->分钟，不足1分钟按1分钟算
    public static int formatMillSecondToMinute(long millSecond) {
        int minute = 1;
        long seconds = millSecond / 1000;
        if (seconds > 60) {
            minute = (int) (seconds / 60);
        }
        return minute;
    }

    //转换ms为s显示文本，如10.231s
    public static String formatMillSecondToShowText(long millSecond) {
        StringBuilder sb = new StringBuilder();
        long seconds = millSecond / 1000;
        sb.append(seconds);
        sb.append(".");
        long mill = millSecond % 1000;
        sb.append(mill);
        sb.append("s");
        return sb.toString();
    }

}
