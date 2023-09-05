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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;


/**
 * Author:      Zhu Fuyu
 * Description: 网络工具类
 * History:     2021/10/9
 */
public class NetUtils {

    public static final String IP_REGULAR = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";

    private NetUtils() {
        throw new IllegalStateException("Utility class");
    }

    //网络是否可用
    public static boolean isNetAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (cm.getActiveNetworkInfo() == null) {
            return false;
        }
        return activeNetwork.isConnectedOrConnecting();
    }

    //获取网络类型
    public static String getNetworkType(Context context) {
        String strNetworkType = "";
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && isNetAvailable(context)) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                strNetworkType = "WIFI";
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                int networkType = networkInfo.getSubtype();
                switch (networkType) {
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                    case TelephonyManager.NETWORK_TYPE_IDEN:
                        strNetworkType = "2G";
                        break;
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                    case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    case TelephonyManager.NETWORK_TYPE_EHRPD:
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                        strNetworkType = "3G";
                        break;
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        strNetworkType = "4G";
                        break;
                    case 20:
                        strNetworkType = "5G";
                        break;
                    default:
                        break;
                }
            }
        }
        return strNetworkType;
    }

    //是否是移动网络
    public static boolean isMobileNetWork(Context context) {
        String type = getNetworkType(context);
        return type.equals("2G") || type.equals("3G") || type.equals("4G")
                || type.equals("5G");
    }

    //是否是Wifi
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null) {
            return false;
        }
        return activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }

    //获取wifi名称
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static String getCurrentWifiSSID(Context context) {
        String wifiSSID = null;
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            wifiSSID = wifiInfo.getSSID();
        }
        return wifiSSID;
    }

    //判断是否为IP地址
    public static boolean isIpAddress(String addr) {
        if (addr == null) {
            return false;
        }

        //过滤协议类型
        if (addr.startsWith("https://")) {
            addr = addr.substring(8);
        } else if (addr.startsWith("http://")) {
            addr = addr.substring(7);
        }

        String realAddr;
        if (addr.contains(":")) {
            //过滤端口号
            realAddr = addr.substring(0, addr.indexOf(":"));
        } else {
            realAddr = addr;
        }

        return realAddr.matches(IP_REGULAR);
    }
}
