package com.google.zxing.client.android.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FormatUtil {
    public static String timeLong2Str(long time) {
        if (time <= 0) {
            return "00:00";
        }
        Long leftTime = time;

        StringBuilder timeSb = new StringBuilder();
        long hour = leftTime / (1000 * 60 * 60);
        if (hour > 0) {
            if (hour < 10) {
                timeSb.append("0");
            }
            timeSb.append(hour + ":");
            leftTime -= hour * (1000 * 60 * 60);
        }
        long minute = leftTime / (1000 * 60);
        if (minute > 9) {
            timeSb.append(minute + ":");
            leftTime -= minute * (1000 * 60);
        } else {
            timeSb.append("0" + minute + ":");
            leftTime -= minute * (1000 * 60);
        }
        long secend = leftTime / (1000);
        if (secend > 9) {
            timeSb.append(secend);
        } else {
            timeSb.append("0" + secend);
        }
        return timeSb.toString();
    }

    public static String formatTime(long timestamp, String format) {
        if (timestamp < 0) {
            return "";
        } else {
            return new SimpleDateFormat(format).format(new Date(timestamp));
        }
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
}
