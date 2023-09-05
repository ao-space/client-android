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

import android.Manifest;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;

import xyz.eulix.space.network.gateway.VersionCheckResponseBody;
import xyz.eulix.space.transfer.TransferHelper;

/**
 * @author: chenjiawei
 * date: 2021/6/1 14:50
 */
public class ConstantField {
    public static final String PACKAGE_NAME = "xyz.eulix.space";
    public static final String LAUNCHER_CLASS_NAME = "xyz.eulix.space.EulixSpaceLaunchActivity";
    public static final String UNIQUE_ID = Build.BOARD;
    public static final String EXTERNAL_DATA_FILE_NAME = "EulixSpaceData";
    public static final String OFFICE_PACKAGE_NAME = "com.wxiwei.office";
    public static final String ZXING_PACKAGE_NAME = "com.google.zxing.client.android";
    public static final String WEB_VIEW_USER_AGENT_EXTEND = " Eulix/Android";
    public static final String FOR_RESULT = "for_result";

    public static final String QR_CODE_RESULT = "qr_code_result";
    public static final String NOT_APPLICABLE = "N/A";
    public static final String WIFI_SSIDS = "wifi_ssids";
    public static final String FAST_DISK_INITIALIZE = "fast_disk_initialize";
    public static final String SSID = "ssid";
    public static final String PASSWORD = "password";
    public static final String EULIX_DEVICE = "eulix_device";
    public static final String OTHER_DEVICE = "other_device";
    public static final String BLUETOOTH_ID = "bluetooth_id";
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADDRESS = "device_address";
    public static final String IP_INFORMATION = "ip_information";
    public static final String BOX_NAME = "box_name";
    public static final String PRODUCT_ID = "product_id";
    public static final String BOUND = "bound";
    public static final String BASE_URL = "base_url";
    public static final String DOMAIN = "domain";
    public static final String BLE_KEY = "ble_key";
    public static final String BLE_IV = "ble_iv";
    public static final String CODE = "code";
    public static final String CATEGORY = "category";
    public static final String BOX_UUID = "box_uuid";
    public static final String BOX_BIND = "box_bind";
    public static final String BOX_DOMAIN = "box_domain";
    public static final String CLIENT_UUID = "client_uuid";
    public static final String BIND_TYPE = "bind_type";
    public static final String BIND_RESULT = "bind_result";
    public static final String PLATFORM_URL = "platform_url";
    public static final String DISTRIBUTE_NETWORK = "distribute_network";
    public static final String ONLINE_DISTRIBUTE = "online_distribute";
    public static final String BLUETOOTH_ADDRESS = "bluetooth_address";
    public static final String SCREEN_SHOT_PATH = "screen_shot_path";
    public static final String INITIAL_ESTIMATE_TIME = "initial_estimate_time";
    public static final String INIT_RESPONSE = "init_response";
    public static final String FILE_UUID = "file_uuid";
    public static final String DELETE_MEMBER_RESULT = "delete_member_result";
    public static final String DELETE_SELF = "delete_self";
    public static final String OFFLINE_BOX_UUID = "offline_box_uuid";
    public static final String OFFLINE_BOX_BIND = "offline_box_bind";
    public static final String DEVICE_MODEL_NUMBER = "device_model_number";
    // 见本类的OfflineBluetoothFunction类
    public static final String OFFLINE_BLUETOOTH_FUNCTION = "offline_bluetooth_function";

    public static final String PLATFORM_KEY = "platform_key";
    public static final String BOX_KEY = "box_key";
    public static final String BTID = "btid";
    public static final String SN = "sn";
    public static final String IPADDR = "ipaddr";
    public static final String PORT = "port";
    public static final String FORCE = "force";
    public static final String FROM_CALL = "from_call";
    public static final String AO_ID = "ao_id";
    public static final String TERMINAL_TYPE = "terminal_type";
    public static final String TERMINAL_MODE = "terminal_mode";
    public static final String USER_DOMAIN = "user_domain";
    public static final String MESSAGE_TYPE = "message_type";
    public static final String BIND_RESULT_RETRY = "bind_result_retry";
    public static final String BIND_RESULT_RETRY_FAIL = "bind_result_retry_fail";
    public static final String WLAN_SSID = "wlan_ssid";
    public static final String IS_WIRE = "is_wire";

    public static final String HARDWARE_INDEX = "hardware_index";
    public static final String DATA_UUID = "data_uuid";
    public static final String GRANTER_DATA_UUID = "granter_data_uuid";
    public static final String AUTHENTICATION_UUID = "authentication_uuid";
    public static final String FORGET_PASSWORD_UUID = "forget_password_uuid";
    public static final String GRANTER_CLIENT_UUID = "granter_client_uuid";

    public static final String DISK_INITIALIZE_ENABLE = "disk_initialize_enable";
    public static final String DISK_INITIALIZE = "disk_initialize";
    public static final String DISK_INITIALIZE_NO_MAIN_STORAGE = "disk_initialize_no_main_storage";
    public static final String DISK_INITIALIZE_DEVICE_LIST = "disk_initialize_device_list";

    // 用于快捷访问蓝牙或局域网功能，见本类的HardwareFunction
    public static final String HARDWARE_FUNCTION = "hardware_function";
    // 安保功能，0表示不使用，其余值见本类的SecurityFunction
    public static final String SECURITY_FUNCTION = "security_function";
    // 安保验证方式，0表示不使用，其余值见本类的AuthenticationFunction
    public static final String AUTHENTICATION_FUNCTION = "authentication_function";
    public static final String RESET_PASSWORD = "reset_password";
    public static final String MAILBOX_ACCOUNT = "mailbox_account";
    public static final String GRANTEE_SECURITY_TOKEN_EXPIRE = "grantee_security_token_expire";
    // 磁盘功能，见本类的DiskFunction
    public static final String DISK_FUNCTION = "disk_function";
    public static final String DISK_INDEX = "disk_index";
    public static final String DISK_EXPAND = "disk_expand";

    public static final String NETWORK_CONFIG_DNS = "network_config_dns";
    public static final String IS_IPV6 = "is_ipv6";
    public static final String IS_CONNECT = "is_connect";

    public static final int SERVER_EXCEPTION_CODE = -1;
    public static final int OBTAIN_ACCESS_TOKEN_CODE = -200;
    public static final int NETWORK_ERROR_CODE = -500;
    public static final int FILE_ALREADY_EXISTS_CODE = 1013;
    public static final int FILE_DISCONNECT_CODE = -1000;
    public static final int FILE_NOT_EXIST = 1003;
    public static final int PLATFORM_UNAVAILABLE_CODE = 577;

    public static final double GOLD_RATIO = ((Math.sqrt(5) - 1) / 2.0);

    public static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";

    public static class EulixCommonInputExtra {
        public static final String INPUT_CONTENT = "input_content";
        public static final String INPUT_FUNCTION = "input_function";

        public static final class InputFunction {
            public static final int SECURITY_EMAIL_ACCOUNT_FUNCTION = 1;
            public static final int SECURITY_EMAIL_PASSWORD_FUNCTION = SECURITY_EMAIL_ACCOUNT_FUNCTION + 1;
            public static final int SECURITY_EMAIL_SMTP_SERVER_FUNCTION = SECURITY_EMAIL_PASSWORD_FUNCTION + 1;
            public static final int SECURITY_EMAIL_PORT_FUNCTION = SECURITY_EMAIL_SMTP_SERVER_FUNCTION + 1;
        }

        public static final String INPUT_TYPE = "input_type";

        public static final class InputType {
            public static final int NONE = 0;
            public static final int TEXT = NONE + 1;
            public static final int TEXT_PASSWORD = TEXT + 1;
            public static final int NUMBER = TEXT_PASSWORD + 1;
            public static final int NUMBER_PASSWORD = NUMBER + 1;
        }

        public static final String IME_OPTIONS = "ime_options";

        public static final class ImeOptions {
            public static final int NORMAL = 1;
            public static final int ACTION_GO = NORMAL + 1;
            public static final int ACTION_SEARCH = ACTION_GO + 1;
            public static final int ACTION_SEND = ACTION_SEARCH + 1;
            public static final int ACTION_NEXT = ACTION_SEND + 1;
            public static final int ACTION_DONE = ACTION_NEXT + 1;
            public static final int ACTION_PREVIOUS = ACTION_DONE + 1;
        }

        public static final String COMMON_INPUT_ID = "common_input_id";
        public static final String COMMON_TITLE = "common_title";
        public static final String INPUT_HINT = "input_hint";
        public static final String INPUT_TEXT_MATCH = "input_text_match";
        public static final String INPUT_COMMON_FUNCTION = "input_common_function";
        public static final String INPUT_CHARACTER_MAX_LENGTH = "input_character_max_length";
        public static final String INPUT_CHARACTER_MIN_LENGTH = "input_character_min_length";

        public static final class InputCommonFunction {
            public static final int COMMON_CLEAR = 1;
            public static final int COMMON_PRIVATE = COMMON_CLEAR + 1;
        }

        public static final String COMMON_HINT = "common_hint";
    }

    public static class ActivityIndex {
        public static final int EULIX_DEVICE_LIST_ACTIVITY_INDEX = 1;
        public static final int LOGIN_TERMINAL_ACTIVITY_INDEX = EULIX_DEVICE_LIST_ACTIVITY_INDEX + 1;
        public static final int ABOUT_US_ACTIVITY_INDEX = LOGIN_TERMINAL_ACTIVITY_INDEX + 1;
        public static final int SYSTEM_UPDATE_ACTIVITY_INDEX = ABOUT_US_ACTIVITY_INDEX + 1;
    }

    public static class SizeUnit {
        public static final String FORMAT_1F = "%.1f";
        public static final String FORMAT_2F = "%.2f";
        public static final String BYTE = "B";
        public static final String KILO_BYTE = "KB";
        public static final String MEGA_BYTE = "MB";
        public static final String GIGA_BYTE = "GB";
        public static final String TERA_BYTE = "TB";
        public static final String KILO_BYTE_SIMPLE = "K";
        public static final String MEGA_BYTE_SIMPLE = "M";
        public static final String GIGA_BYTE_SIMPLE = "G";
        public static final String TERA_BYTE_SIMPLE = "T";

        public static final String BYTE_PER_SECOND = "B/s";
        public static final String KILO_BYTE_PER_SECOND = "KB/s";
        public static final String MEGA_BYTE_PER_SECOND = "MB/s";
        public static final String GIGA_BYTE_PER_SECOND = "GB/s";
        public static final String TERA_BYTE_PER_SECOND = "TB/s";
    }

    public static class FragmentIndex {
        public static final int TAB_HOME = 1000;
        public static final int TAB_FILE = TAB_HOME + 1000;
        public static final int FILE_ALL = TAB_FILE + 100;
        public static final int FILE_IMAGE = FILE_ALL + 100;
        public static final int FILE_VIDEO = FILE_IMAGE + 100;
        public static final int FILE_DOCUMENT = FILE_VIDEO + 100;
        public static final int FILE_OTHER = FILE_DOCUMENT + 100;
        public static final int TAB_CIRCLE = TAB_FILE + 1000;
        public static final int TAB_MINE = TAB_CIRCLE + 1000;

        public static final int TAB_INSTALL_MIRROR_GENERAL_SETTINGS = 100;
        public static final int TAB_INSTALL_MIRROR_ADVANCED_SETTINGS = TAB_INSTALL_MIRROR_GENERAL_SETTINGS + 100;
    }

    public static class TimeStampFormat {
        public static final String DATE_FORMAT = "yyyy/MM/dd HH:mm";
        public static final String DATE_FORMAT_2 = "yyyy-MM-dd HH:mm";
        public static final String DATE_FORMAT_WITHOUT_YEAR = "MM/dd HH:mm";
        public static final String DATE_FORMAT_WITHOUT_YEAR_2 = "MM-dd HH:mm";
        public static final String TODAY_FORMAT = "HH:mm";
        public static final String FILE_API_DAY_FORMAT = "yyyy-MM-dd";
        public static final String FILE_API_MONTH_FORMAT = "yyyy-MM";
        public static final String FILE_API_YEAR_FORMAT = "yyyy";
        public static final String FILE_API_DAY_FORMAT_ZH = "yyyy年M月d日";
        public static final String FILE_API_DAY_FORMAT_WEEK = "yyyy-MM-dd EEEE";
        public static final String FILE_API_DAY_FORMAT_ZH_WEEK = "yyyy年M月d日 EEEE";
        public static final String FILE_API_MONTH_FORMAT_ZH = "yyyy年M月";
        public static final String FILE_API_YEAR_FORMAT_ZH = "yyyy年";
        public static final String FILE_API_SPLIT = "T";
        public static final String EMAIL_FORMAT = "yyyy-MM-dd HH:mm:ss";
        public static final String FILE_API_MINUTE_FORMAT = "yyyy-MM-dd HH:mm";
        public static final String DATE_FORMAT_ONE_DAY = "HH:mm:ss";
    }

    public static class TimeUnit {
        public static final long SECOND_UNIT = 1000L;
        public static final long MINUTE_UNIT = 60000L;
        public static final long HOUR_UNIT = 3600000L;
        public static final long DAY_UNIT = 86400000L;
        public static final long WEEK_UNIT = 604800000L;
    }

    public static class MemberInviteResultCode {
        public static final int MEMBER_DUPLICATE = 4031;
        public static final int MEMBER_NICKNAME_ILLEGAL = 4032;
        public static final int INVITE_CODE_INVALID = 4033;
        public static final int MEMBER_FULL = 4034;
        public static final int SPACE_ID_REPEAT = 400;
    }

    public static class StringPattern {
        public static final String CHINESE_REG = "[\\u4e00-\\u9fa5]";

        public static final String LETTER_REG = "[a-zA-Z]";

        public static final String NUMBER_REG = "[0-9]";

        public static final String LETTER_CHINESE_REG = "[a-zA-Z\\u4e00-\\u9fa5]";

        public static final String LETTER_NUMBER_REG = "[a-zA-Z0-9]";

        public static final String LETTER_NUMBER_CHINESE_REG = "[a-zA-Z0-9\\u4e00-\\u9fa5]";
        // 支持中文、英文、数字、符号（键盘按住shift打出的符号）组成，不能有空格
        public static final String NICKNAME_REG_EX = "[^(a-zA-Z0-9\\u4e00-\\u9fa5\\`~!@#$%^&*()-_+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？)]";
        //空间名称规则
        public static final String SPACE_NAME_REG = "^[a-zA-Z0-9\\u4e00-\\u9fa5\\`~!@#$%^&*()-_+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]*$";
        //手机号码校验规则
        public static final String PHONE_NUMBER_REG = "(((\\+86)|(86))?1[3|5|7|8|][0-9]{9})|(\\d{3}-\\d{8})|(\\d{4}-\\d{7})";
        //邮箱校验规则
        public static final String EMAIL_REG = "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$";
        // 域名规则
        public static final String DOMAIN_REG = "^[a-zA-Z][a-zA-Z0-9]{5,19}$";
    }

    public static class EulixSpaceSPKey {
        public static final String UUID = "uuid";
        public static final String CLIENT_RSA_2048_PUBLIC_KEY = "client_rsa_2048_public_key";
        public static final String CLIENT_RSA_2048_PRIVATE_KEY = "client_rsa_2048_private_key";
        public static final String BOX_RSA_512_KEY = "box_rsa_512_key";
        public static final String APPLICATION_LOCALE = "application_locale";
        public static final String BOX_UUID = "box_uuid";
        public static final String APK_SIZE = "apk_size";
        public static final String APK_DOWNLOAD_URL = "apk_download_url";
        public static final String APK_MD5 = "apk_md5";
        public static final String APK_VERSION = "apk_version";
        public static final String APK_DESCRIPTION = "apk_description";
        public static final String LAST_EULIX_SPACE = "last_eulix_space";
        public static final String FILE_SORT_ORDER = "file_sort_order";
        public static final String APK_DOWNLOAD_PATH = "apk_download_path";
        public static final String NOTIFICATION_REMINDER_VERSION = "notification_reminder_version";
        public static final String SYSTEM_MESSAGE_ENABLE = "system_message_enable";
        public static final String BUSINESS_MESSAGE_ENABLE = "business_message_enable";
        public static final String BOX_ENVIRONMENT = "box_environment";
        public static final String APP_UPDATE_VERSION = "app_update_version";
        public static final String SECURITY_EMAIL_CONFIGURATIONS = "security_email_configurations";
        public static final String SECURITY_EMAIL_REMINDER_BOX_LIST = "security_email_reminder_box_list";
        public static final String BACKGROUND_RUNNING_TIMESTAMP = "background_running_timestamp";
        public static final String SWITCH_PLATFORM_TASK = "switch_platform_task";
        public static final String PLATFORM_ABILITY_MAP = "platform_ability_map";
    }

    public static class CookieHeader {
        public static final String COOKIE_HEADER_NAME = "Cookie";
        public static final String COOKIE_ASSIGN = "=";
        public static final String COOKIE_SPLIT = "; ";

        public static class CookieName {
            public static final String CLIENT_UUID = "client_uuid";
        }
    }

    public static class HardwareIndex {
        public static final int BLUETOOTH = 1;
        public static final int LAN = BLUETOOTH + 1;
    }

    public static class Algorithm {
        public static final String RSA = "RSA";
        public static final String AES = "AES";
        public static final String ECC = "EC";
        public static final int RSA_2048_PUBLIC_LENGTH = 392;

        public static class Transformation {
            public static final String RSA_ECB_PKCS1 = "RSA/ECB/PKCS1Padding";
            public static final String AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding";
        }

        public static class SignatureAlgorithm {
            public static final String SHA256_WITH_RSA = "SHA256withRSA";
        }
    }

    public static class Language {
        public static final int SYSTEM = 0;
        public static final int CHINESE = SYSTEM + 1;
        public static final int ENGLISH = CHINESE + 1;
    }

    public static class Category {
        // 跳转页面所用
        public static final String DOCUMENT = "document";
        public static final String VIDEO = "video";
        public static final String PICTURE = "picture";
        public static final String OTHER = "other";
        public static final String PICTURE_AND_VIDEO = "picture,video";

        // 数据库所用
        public static final String FILE_ROOT = "file_root";
        public static final String FILE_IMAGE = "file_image";
        public static final String FILE_VIDEO = "file_video";
        public static final String FILE_DOCUMENT = "file_document";
        public static final String FILE_OTHER = "file_other";
        public static final String FILE_RECYCLE = "file_recycle";
    }

    public static class Sort {
        public static final String NAME_DESCEND = "is_dir desc,name desc";
        public static final String NAME_ASCEND = "is_dir desc,name asc";
        public static final String OPERATION_TIME_DESCEND = "is_dir desc,operation_time desc";
        public static final String OPERATION_TIME_ASCEND = "is_dir desc,operation_time asc";
        public static final String MIME_DESCEND = "mime desc";
        public static final String MIME_ASCEND = "mime asc";
    }

    public static class MimeType {
        public static final String FOLDER = "folder";
        public static final String BMP = "bmp";
        public static final String GIF = "gif";
        public static final String JPEG = "jpeg";
        public static final String JPG = "jpg";
        public static final String PNG = "png";
        public static final String PDF = "pdf";
        public static final String DOC = "doc";
        public static final String UNKNOWN = "file";

        public static final String[][] MIME_MAP_TABLE = {
                {"apk", "application/vnd.android.package-archive"},
                {"bin", "application/octet-stream"},
                /*支持预览的图片：*/
                {"bmp", "image/bmp"},
                {"webp", "image/webp"},
                {"gif", "image/gif"},
                {"jpeg", "image/jpeg"},
                {"jpg", "image/jpeg"},
                {"heic", "image/heic"},
                {"png", "image/png"},
                /*不支持预览的图片：*/
                {"pcx", "image/pcx"},
                {"tif", "image/tif"},
                {"tga", "image/tga"},
                {"svg", "image/svg"},
                {"tga", "image/tga"},
                /*支持预览的视频：*/
                {"3gp", "video/3gpp"},
                {"mov", "video/quicktime"},
                {"mp4", "video/mp4"},
                {"mpg4", "video/mp4"},
                {"mpe", "video/mpeg"},
                {"mpeg", "video/mpeg"},
                {"mpg", "video/mpeg"},
                {"avi", "video/x-msvideo"},
                {"mkv", "video/mkv"},
                /*不支持预览的视频：*/
                {"asf", "video/x-ms-asf"},
                {"m4u", "video/vnd.mpegurl"},
                {"m4v", "video/x-m4v"},
                {"flv", "video/flv"},
                {"vob", "video/vob"},
                {"dat", "video/dat"},

                {"c", "text/plain"},
                {"class", "application/octet-stream"},
                {"conf", "text/plain"},
                {"cpp", "text/plain"},
                {"doc", "application/msword"},
                {"docx", "application/msword"},
                {"exe", "application/octet-stream"},
                {"gtar", "application/x-gtar"},
                {"gz", "application/x-gzip"},
                {"h", "text/plain"},
                {"htm", "text/html"},
                {"html", "text/html"},
                {"jar", "application/java-archive"},
                {"java", "text/plain"},
                {"js", "application/x-javascript"},
                {"log", "text/plain"},
                {"m3u", "audio/x-mpegurl"},
                {"m4a", "audio/mp4a-latm"},
                {"m4b", "audio/mp4a-latm"},
                {"m4p", "audio/mp4a-latm"},
                {"md", "text/plain"},
                {"mp2", "audio/x-mpeg"},
                {"mp3", "audio/x-mpeg"},
                {"mpc", "application/vnd.mpohun.certificate"},
                {"mpga", "audio/mpeg"},
                {"msg", "application/vnd.ms-outlook"},
                {"ogg", "audio/ogg"},
                {"pdf", "application/pdf"},
                {"pps", "application/vnd.ms-powerpoint"},
                {"ppt", "application/vnd.ms-powerpoint"},
                {"pptx", "application/vnd.ms-powerpoint"},
                {"prop", "text/plain"},
                {"rar", "application/x-rar-compressed"},
                {"rc", "text/plain"},
                {"rmvb", "audio/x-pn-realaudio"},
                {"rtf", "application/rtf"},
                {"sh", "text/plain"},
                {"tar", "application/x-tar"},
                {"tgz", "application/x-compressed"},
                {"txt", "text/plain"},
                {"wav", "audio/x-wav"},
                {"wma", "audio/x-ms-wma"},
                {"wmv", "audio/x-ms-wmv"},
                {"wps", "application/vnd.ms-works"},
                {"xls", "application/vnd.ms-excel"},
                {"xlsx", "application/vnd.ms-excel"},
                //{"xml",    "text/xml"},
                {"xml", "text/plain"},
                {"z", "application/x-compress"},
                {"zip", "application/zip"},
                {"", "*/*"}};
    }

    public static class ViewType {
        public static final int LINEAR_VIEW = 0;
        public static final int GRID_VIEW = LINEAR_VIEW + 1;
        public static final int BOX_FILE_LINEAR_VIEW = GRID_VIEW + 1;
        public static final int BOX_SPACE_VIEW = BOX_FILE_LINEAR_VIEW + 1;
        public static final int CLIENT_MEMBER_VIEW = BOX_SPACE_VIEW + 1;
        public static final int MENU_LOGIN_MORE_SPACE_VIEW = CLIENT_MEMBER_VIEW + 1;
    }

    public static class ShowType {
        public static final int NORMAL = 0;
        public static final int EDIT = NORMAL + 1;
        public static final int SELECT = EDIT + 1;
    }

    public static class ServiceType {
        public static final String EULIXSPACE_SD_TCP = "_eulixspace-sd._tcp.";
    }

    /**
     * 盒子状态，转移关系如(->: 离线,<-: 在线)：
     * ACTIVE <-> OFFLINE_USE
     * REQUEST_LOGIN or REQUEST_USE <-> OFFLINE
     */
    public static class EulixDeviceStatus {
        // 离线
        public static final int OFFLINE = 0;
        // 预备登录
        public static final int REQUEST_LOGIN = OFFLINE + 1;
        // 预备使用
        public static final int REQUEST_USE = REQUEST_LOGIN + 1;
        // 使用中
        public static final int ACTIVE = REQUEST_USE + 1;
        // 离线使用中
        public static final int OFFLINE_USE = ACTIVE + 1;
        // 离线未初始化
        public static final int OFFLINE_UNINITIALIZED = OFFLINE_USE + 1;
        // 在线未初始化
        public static final int ONLINE_UNINITIALIZED = OFFLINE_UNINITIALIZED + 1;
        // 瞬态，数据库里不存在，视觉存在
        public static final int PROGRESS = ONLINE_UNINITIALIZED + 1;
        // 中间态，数据库里存在，视觉不存在
        public static final int BINDING = OFFLINE - 1;
        // 盒子失效，用于在线试用
        public static final int INVALID = -1000;
    }

    public static class BindDeviceStatus {
        public static final int SEARCHING = 0;
        public static final int EMPTY = SEARCHING - 1;
        public static final int PC_HOST_PREPARE = EMPTY - 1;
        public static final int BINDING = SEARCHING + 1;
        public static final int BIND_FAILED = BINDING + 1;
        public static final int BIND_SUCCESS = BIND_FAILED + 1;
        public static final int BIND_DUPLICATE = BIND_SUCCESS + 1;
        public static final int BIND_CONFLICT = BIND_DUPLICATE + 1;
    }

    public static class BindDeviceHttpCode {
        public static final int BIND_DUPLICATE_CODE = 202;
        public static final int BIND_CONFLICT_CODE = 409;
        public static final int BIND_EXIST_MEMBER_CODE = 410;
        public static final int BIND_PLATFORM_FAILED = 577;
    }


    public static class RevokeCode {
        public static final int REVOKE_SUCCESS = 200;
        public static final int REVOKE_PASSWORD_EXCEED = 461;
        public static final int REVOKE_NOT_PAIR = 462;
        public static final int REVOKE_PASSWORD_WRONG = 463;
        public static final int REVOKE_SERVICE_ERROR = 560;
    }

    public static class HardwareFunction {
        public static final int SECURITY_VERIFICATION = 1;
        public static final int SWITCH_SPACE_PLATFORM = SECURITY_VERIFICATION + 1;
    }

    public static class SecurityFunction {
        // 暂定需要跳转的大于0，需要反馈结果的小于0，禁用0
        public static final int RESET_PASSWORD = 1;
        public static final int INITIALIZE_SECURITY_MAILBOX = RESET_PASSWORD + 1;
        public static final int CHANGE_SECURITY_MAILBOX = INITIALIZE_SECURITY_MAILBOX + 1;
        public static final int SWITCH_SPACE_PLATFORM = CHANGE_SECURITY_MAILBOX + 1;

        public static class NewDeviceSecurityFunction {
            public static final int VERIFY_SECURITY_MAILBOX = CHANGE_SECURITY_MAILBOX + 1;
            public static final int NEW_DEVICE_RESET_PASSWORD = VERIFY_SECURITY_MAILBOX + 1;
            public static final int NEW_DEVICE_APPLY_RESET_PASSWORD = NEW_DEVICE_RESET_PASSWORD + 1;
            public static final int SECURITY_MESSAGE_POLL = NEW_DEVICE_APPLY_RESET_PASSWORD + 1;
            public static final int SECURITY_EMAIL_SETTING = SECURITY_MESSAGE_POLL + 1;
        }

        public static class DeveloperOptionsSecurityFunction {
            public static final int OPEN_DEVELOPER_OPTIONS = -1;
        }
    }

    public static class AuthenticationFunction {
        public static final int HARDWARE_DEVICE = 1;
        public static final int SECURITY_PASSWORD = HARDWARE_DEVICE + 1;
        public static final int SECURITY_MAILBOX = SECURITY_PASSWORD + 1;
        public static final int OLD_SECURITY_MAILBOX = SECURITY_MAILBOX + 1;
    }

    public static class OfflineBluetoothFunction {
        public static final int DISTRIBUTE_NETWORK = 1;
        public static final int DISK_MANAGEMENT = DISTRIBUTE_NETWORK + 1;
    }

    public static class DiskFunction {
        public static final int DISK_INITIALIZE = 0;
        public static final int DISK_EXPAND = DISK_INITIALIZE + 1;
    }

    public static class UUID {
        public static final String BLUETOOTH_SERIAL_PORT_SERVICE_CLASS_UUID = "00001101-0000-1000-8000-00805f9b34fb";
        public static final String BLUETOOTH_SERVICE_UUID_BASE_SUFFIX = "0000-1000-8000-00805f9b34fb";
        public static final String FILE_ROOT_UUID = "00000000-0000-0000-0000-000000000000";
    }

    public static class SocketMethod {
        public static final String LOGIN = "login";
        public static final String PING = "ping";
        public static final String ACK = "ack";
        public static final String PUSH = "push";
        public static final String QUERY = "query";
    }

    public static class ApiType {
        public static final int CREATE_AUTH_TOKEN = 1;
        public static final int REFRESH_AUTH_TOKEN = CREATE_AUTH_TOKEN + 1;
    }

    public static class ServiceName {
        public static final String EULIXSPACE_FILE_SERVICE = "eulixspace-file-service";
        public static final String EULIXSPACE_MAILBOX_SERVICE = "eulixspace-mailbox-service";
        public static final String EULIXSPACE_ACCOUNT_SERVICE = "eulixspace-account-service";
        public static final String EULIXSPACE_PREVIEW_SERVICE = "eulixspace-filepreview-service";
        public static final String EULIXSPACE_AGENT_SERVICE = "eulixspace-agent-service";
        public static final String EULIXSPACE_MEDIA_VOD_SERVICE = "aospace-media-vod-service";
    }

    public static class ServiceFunction {
        // 文件列表
        public static final String LIST_FOLDERS = "list_folders";
        // 文件夹信息
        public static final String FOLDER_INFO = "folder_info";
        // 文件重命名
        public static final String MODIFY_FILE = "modify_file";
        // 文件复制
        public static final String COPY_FILE = "copy_file";
        // 文件移动
        public static final String MOVE_FILE = "move_file";
        // 文件删除
        public static final String DELETE_FILE = "delete_file";
        //查询异步任务状态
        public static final String ASYNC_TASK_INFO = "async_task_info";
        // 新建文件夹
        public static final String CREATE_FOLDER = "create_folder";
        // 上传文件
        public static final String UPLOAD_FILE = "upload_file";
        // 下载文件
        public static final String DOWNLOAD_FILE = "download_file";
        // 文件搜索
        public static final String SEARCH_FILES = "search_files";
        // 查询文件信息
        public static final String FILE_INFO = "get_fileinfo";
        // 回收站列表
        public static final String LIST_RECYCLED = "list_recycled";
        // 还原回收站文件
        public static final String RESTORE_RECYCLED = "restore_recycled";
        // 删除回收站文件
        public static final String CLEAR_RECYCLED = "clear_recycled";

        // 盒子存储信息
        public static final String STORAGE_INFO_SHOW = "storageinfo_show";
        //获取头像
        public static final String IMAGE_SHOW = "image_show";
        //获取用户信息（昵称、签名）
        public static final String PERSONALINFO_SHOW = "personalinfo_show";
        //修改昵称或签名
        public static final String PERSONALINFO_UPDATE = "personalinfo_update";
        //修改域名
        public static final String PERSONAL_INFO_UPDATE = "personal_info_update";
        //修改头像
        public static final String PERSONAL_IMAGE_UPDATE = "image_update";
        // 成员空间使用情况
        public static final String MEMBER_USED_STORAGE = "member_used_storage";
        // 成员列表
        public static final String MEMBER_LIST = "member_list";
        // 更新成员名字
        public static final String MEMBER_NAME_UPDATE = "member_name_update";
        // 获取终端列表
        public static final String TERMINAL_INFO_ALL_SHOW = "terminal_info_all_show";
        // 终端下线
        public static final String TERMINAL_INFO_DELETE = "terminal_info_delete";
        //下载缩略图
        public static final String DOWNLOAD_THUMBNAILS = "download_thumbnails";
        //下载压缩图
        public static final String DOWNLOAD_COMPRESSED = "download_compressed";
        //下载预览文件
        public static final String DOWNLOAD_PREVIEW = "download_preview";
        //下载备份文件用户头像
        public static final String DOWNLOAD_RESTORE_HEADER = "get_headimage";
        //校验bkey
        public static final String BOX_LOGIN_BKEY_VERIFY = "auth_totp_bkey_verify";
        //盒子登录获取授权码
        public static final String BOX_LOGIN_GET_AUTH_CODE = "auth_totp_auth-code";
        //盒子登录轮询授权结果
        public static final String BOX_LOGIN_POLL_AUTH_RESULT = "auth_totp_bkey_poll";

        //盒子系统升级相关
        //获取升级配置
        public static final String GET_UPGRADE_CONFIG = "get_upgrade_config";
        //配置升级配置
        public static final String SET_UPGRADE_CONFIG = "set_upgrade_config";
        //开始下载升级包
        public static final String UPGRADE_START_PULL = "upgrade_start_down";
        //开始安装升级
        public static final String UPGRADE_START_UPGRADE = "upgrade_start_upgrade";
        //查询升级状态
        public static final String UPGRADE_STATUS = "upgrade_status";
        //获取傲空间设备信息
        public static final String DEVICE_INFO = "device_version";

        public static final String LOCAL_IPS = "localips";

        public static final String NETWORK = "network";

        public static final String NET_CONFIG = "netconfig";

        public static final String NET_CONFIG_SETTING = "netconfigsetting";

        public static final String NOTIFICATION_GET = "notification_get";

        public static final String NOTIFICATION_GET_ALL = "notification_get_all";

        public static final String NOTIFICATION_DELETE_ALL = "notification_delete_all";

        //断点续传相关
        public static final String MULTIPART_CREATE_UPLOAD = "multipart_create";
        public static final String MULTIPART_UPLOAD_UPLOAD = "multipart_upload";
        public static final String MULTIPART_LIST_UPLOAD = "multipart_list";
        public static final String MULTIPART_COMPLETE_UPLOAD = "multipart_complete";
        public static final String MULTIPART_DELETE_UPLOAD = "multipart_delete";

        //获取Https证书文件
        public static final String GET_HTTPS_CERT = "get_lan_cert";

        // 安全设置相关
        // 获取硬件信息
        public static final String DEVICE_HARDWARE_INFO = "device_hardware_info";
        // 获取密保邮箱配置
        public static final String SECURITY_EMAIL_CONFIGURATIONS = "security_email_configurations";
        // 绑定端修改安全密码
        public static final String SECURITY_PASSWORD_MODIFY_BINDER = "security_passwd_modify_binder";
        // 验证安全密码
        public static final String SECURITY_PASSWORD_VERIFY = "security_passwd_verify";
        // 绑定端重置安全密码
        public static final String SECURITY_PASSWORD_RESET_BINDER = "security_passwd_reset_binder";
        // 授权端请求修改安全密码
        public static final String SECURITY_PASSWORD_MODIFY_AUTHORIZED_APPLY = "security_passwd_modify_auther_apply";
        // 绑定端处理授权端修改安全密码
        public static final String SECURITY_PASSWORD_MODIFY_BINDER_ACCEPT = "security_passwd_modify_binder_accept";
        // 授权端请求修改安全密码
        public static final String SECURITY_PASSWORD_RESET_AUTHORIZED_APPLY = "security_passwd_reset_auther_apply";
        // 绑定端处理授权端重置安全密码
        public static final String SECURITY_PASSWORD_RESET_BINDER_ACCEPT = "security_passwd_reset_binder_accept";
        // 授权端修改安全密码
        public static final String SECURITY_PASSWORD_MODIFY_AUTHORIZED = "security_passwd_modify_auther";
        // 授权端重置安全密码
        public static final String SECURITY_PASSWORD_RESET_AUTHORIZED = "security_passwd_reset_auther";
        // 授权端拉取消息
        public static final String SECURITY_MESSAGE_POLL = "security_message_poll";
        public static final String ADMINISTRATOR_GET_DEVELOP_OPTIONS_SWITCH = "admin_get_dev-options_switch";

        public static final String ADMINISTRATOR_UPDATE_DEVELOP_OPTIONS_SWITCH = "admin_update_dev-options_switch";

        // 二代盒子
        // 磁盘管理列表
        public static final String DISK_MANAGEMENT_LIST = "disk_management_list";
        // 磁盘raid获取
        public static final String DISK_MANAGEMENT_RAID_INFO = "disk_management_raid_info";
        // 磁盘扩容
        public static final String DISK_MANAGEMENT_EXPAND = "disk_management_expand";
        // 磁盘扩容进度
        public static final String DISK_MANAGEMENT_EXPAND_PROGRESS = "disk_management_expand_progress";
        // 盒子关机
        public static final String SYSTEM_SHUTDOWN = "system_shutdown";
        // 详细网络配置
        public static final String NETWORK_CONFIG = "network_config";
        // 设置详细网络配置
        public static final String NETWORK_CONFIG_UPDATE = "network_config_update";
        // 忽略此网络
        public static final String NETWORK_IGNORE = "network_ignore";
        // 设备能力
        public static final String DEVICE_ABILITY = "device_ability";
        // 获取网络通道情况
        public static final String GET_NETWORK_CHANNEL_INFO = "get_network_channel_info";
        // 设置互联网通道
        public static final String SET_NETWORK_CHANNEL_WAN = "set_network_channel_wan";

        public static final String INTERNET_SERVICE_GET_CONFIG = "internet_service_get_config";

        public static final String INTERNET_SERVICE_CONFIG = "internet_service_config";

        public static final String GET_DID_DOCUMENT = "get_did_document";

        //在线播放相关
        //是否支持
        public static final String MEDIA_VOD_CHECK = "check";
        //下载m3u8文件
        public static final String MEDIA_VOD_M3U8_DOWNLOAD = "m3u8_file";
    }

    public static class BoxVersionName {
        public static final String VERSION_0_1_0 = "0.1.0";
        public static final String VERSION_0_1_6 = "0.1.6";
        public static final String VERSION_0_2_0 = "0.2.0";
        public static final String VERSION_0_2_5 = "0.2.5";
    }

    public static class RequestCode {
        public static final int ALL_PERMISSION = 1;
        public static final int BLUETOOTH_PERMISSION = ALL_PERMISSION + 1;
        public static final int ACCESS_LOCATION_PERMISSION = BLUETOOTH_PERMISSION + 1;
        public static final int EXTERNAL_STORAGE_PERMISSION = ACCESS_LOCATION_PERMISSION + 1;
        public static final int MANAGE_EXTERNAL_STORAGE_PERMISSION = EXTERNAL_STORAGE_PERMISSION + 1;
        public static final int CAMERA_PERMISSION = MANAGE_EXTERNAL_STORAGE_PERMISSION + 1;
        public static final int REQUEST_CODE_SCAN = CAMERA_PERMISSION + 1;
        public static final int REQUEST_INSTALL_PACKAGES = REQUEST_CODE_SCAN + 1;
        public static final int CONTACTS_PERMISSION = REQUEST_INSTALL_PACKAGES + 1;

        public static final int REQUEST_BLUETOOTH_ENABLE_CODE = 1000;
        public static final int WIFI_DISTRIBUTE_CODE = REQUEST_BLUETOOTH_ENABLE_CODE + 1;
        public static final int BIND_DEVICE_CODE = WIFI_DISTRIBUTE_CODE + 1;
        public static final int FIND_DEVICE_CODE = BIND_DEVICE_CODE + 1;
        public static final int LOGIN_DEVICE_CODE = FIND_DEVICE_CODE + 1;
        public static final int UNBIND_DEVICE_CODE = LOGIN_DEVICE_CODE + 1;
        public static final int DOMAIN_EDIT_CODE = UNBIND_DEVICE_CODE + 1;
        public static final int RESET_SECURITY_PASSWORD_CODE = DOMAIN_EDIT_CODE + 1;
        public static final int SET_SECURITY_EMAIL_CODE = RESET_SECURITY_PASSWORD_CODE + 1;
        public static final int EULIX_COMMON_INPUT_CODE = SET_SECURITY_EMAIL_CODE + 1;
        public static final int SPACE_PLATFORM_ENVIRONMENT_CODE = EULIX_COMMON_INPUT_CODE + 1;
        public static final int INSTALL_MIRROR_CODE = SPACE_PLATFORM_ENVIRONMENT_CODE + 1;
        public static final int INSTALL_MIRROR_SETTINGS_CODE = INSTALL_MIRROR_CODE + 1;
        public static final int SECURITY_PASSWORD_VERIFICATION_CODE = INSTALL_MIRROR_SETTINGS_CODE + 1;
        public static final int BIND_RESULT_CODE = SECURITY_PASSWORD_VERIFICATION_CODE + 1;
        public static final int LOCAL_IMAGE_CODE = BIND_RESULT_CODE + 1;
        public static final int EULIX_PERMISSION_MANAGER = LOCAL_IMAGE_CODE + 1;

        public static final int EULIX_SPACE_JOB_ID = 2000;
        public static final int EULIX_SPACE_FOREGROUND_ID = EULIX_SPACE_JOB_ID + 1;
        public static final int EULIX_SPACE_NOTIFICATION_START_ID = EULIX_SPACE_FOREGROUND_ID + 1;
    }

    public static class Action {
        public static final String JOB_ACTION = "xyz.eulix.space.action.JOB";
        public static final String LAUNCH_ACTION = "xyz.eulix.space.action.LAUNCH";
        public static final String TOKEN_ACTION = "xyz.eulix.space.action.TOKEN";
        public static final String STORAGE_ACTION = "xyz.eulix.space.action.STORAGE";
    }

    public static class BroadcastCommunication {
        public static final String EULIX_SPACE_LAN = "xyz.eulix.space.action.LAN";
        public static final String LAN_ENABLE = "lan_enable";
    }

    public static class ZxingCommunication {
        public static final String ZXING_CAPTURE_SEND_ACTION = "com.google.zxing.client.android.action.CAPTURE_SEND";
        public static final String ZXING_CAPTURE_RECV_ACTION = "com.google.zxing.client.android.action.CAPTURE_RECV";

        // 表示广播类型，值为int
        public static final String KEY_TYPE = "key_type";
        public static final int QUERY_ACTIVE_BOX_BIND = 10001;
        public static final int QUERY_BLUETOOTH_VALID = QUERY_ACTIVE_BOX_BIND + 1;
        public static final int QUERY_PC_HOST_VALID = QUERY_BLUETOOTH_VALID + 1;
        public static final int REPLY_ACTIVE_BOX_BIND = 20001;
        public static final int REPLY_BLUETOOTH_VALID = REPLY_ACTIVE_BOX_BIND + 1;
        public static final int REPLY_PC_HOST_VALID = REPLY_BLUETOOTH_VALID + 1;

        public static final int CUSTOMIZE_PATTERN_SCAN_DEVICE_QR_CODE = 1;

        public static final String REQUEST_ID = "request_id";

        // 表示二维码信息用作校验，值为String
        public static final String CONTENT = "content";

        // 表示扫码功能，值为int
        public static final String FUNCTION_EXTRA_KEY = "function";
        public static final int CUSTOMIZE_VALUE = 0;
        public static final int LOGIN_EXTRA_VALUE = CUSTOMIZE_VALUE + 1;
        public static final int BLUETOOTH_EXTRA_VALUE = LOGIN_EXTRA_VALUE + 1;
        public static final int PC_HOST_EXTRA_VALUE = BLUETOOTH_EXTRA_VALUE + 1;

        // 表示扫码匹配规则，FUNCTION_EXTRA_KEY值为0使用
        public static final String CUSTOMIZE_MATCH_REG = "customize_match_reg";

        // 表示是否立即响应，值为boolean
        public static final String IMMEDIATE_EXTRA_KEY = "immediate";

        public static final String DEFAULT_STATUS = "default_status";

        public static final String LOCAL_GALLERY_TEXT = "local_gallery_text";

        public static final String CUSTOMIZE_PATTERN = "customize_pattern";

        public static final String CUSTOMIZE_PATTERN_HINT = "customize_pattern_hint";

        public static final String CUSTOMIZE_QR_TIP_RES_ID = "customize_qr_tip_res_id";

        // 表示处理结果，值为boolean
        public static final String RESULT = "result";
    }

    public static class EXTRA {
        public static final String FOREGROUND = "foreground";
    }

    public static class NSD_SERVICE_INFO {
        public static final String SERVICE_NAME = "service_name";
        public static final String SERVICE_TYPE = "service_type";
        public static final String SERIAL_NUMBER = "serial_number";
        public static final String HOST_ADDRESS = "host_address";
        public static final String ATTRIBUTE = "attribute";
    }

    public static class Permission {
        public static final String CAMERA = Manifest.permission.CAMERA;
        public static final String ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
        public static final String ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
        public static final String READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;
        public static final String WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        public static final String MANAGE_EXTERNAL_STORAGE = Manifest.permission.MANAGE_EXTERNAL_STORAGE;
        public static final String INTERNET = Manifest.permission.INTERNET;
        public static final String ACCESS_NETWORK_STATE = Manifest.permission.ACCESS_NETWORK_STATE;
        public static final String BLUETOOTH = Manifest.permission.BLUETOOTH;
        public static final String BLUETOOTH_ADMIN = Manifest.permission.BLUETOOTH_ADMIN;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static class Permission_23 {
        public static final String ACCESS_NOTIFICATION_POLICY = Manifest.permission.ACCESS_NOTIFICATION_POLICY;
        public static final String REQUEST_INSTALL_PACKAGES = Manifest.permission.REQUEST_INSTALL_PACKAGES;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public static class Permission_28 {
        public static final String FOREGROUND_SERVICE = Manifest.permission.FOREGROUND_SERVICE;
        public static final String USE_BIOMETRIC = Manifest.permission.USE_BIOMETRIC;
    }

    public static class PushType {
        // APP自定义推送事件
        public static class NativeType {
            public static final String TRIAL_INVALID = "trial_invalid";
        }

        // 网关发送
        public static final String LOGIN = "login";
        public static final String LOGOUT = "logout";
        public static final String REVOKE = "revoke";
        public static final String MEMBER_SELF_DELETE = "member_self_delete";
        public static final String LOGIN_CONFIRM = "login_confirm";
        public static final String UPGRADE_SUCCESS = "upgrade_success";
        public static final String SECURITY_PASSWORD_MODIFY_APPLY = "security_passwd_mod_apply";
        public static final String SECURITY_PASSWORD_MODIFY_SUCCESS = "security_passwd_mod_succ";
        public static final String SECURITY_PASSWORD_RESET_APPLY = "security_passwd_reset_apply";
        public static final String SECURITY_PASSWORD_RESET_SUCCESS = "security_passwd_reset_succ";
        public static final String ABILITY_CHANGE = "ability_change";
        //盒子升级包下载完成
        public static final String BOX_UPGRADE_PACKAGE_PULLED = "upgrade_download_success";
        //盒子开始升级
        public static final String BOX_START_UPGRADE = "upgrade_installing";
        //盒子系统开始重启
        public static final String BOX_SYSTEM_RESTART = "upgrade_restart";
        // 平台发送
        public static final String BOX_UPGRADE = "box_upgrade";
        public static final String APP_UPGRADE = "app_upgrade";
    }

    public static class PushExtraKey {
        public static final String OPT_TYPE = "optType";
        public static final String MESSAGE_ID = "messageId";
        public static final String SUB_DOMAIN = "subdomain";
    }

    public static class URL {
        public static final String BASE_SERVER_URL_RELEASE = "https://services.eulix.xyz/";
        @Deprecated
        public static final String REGISTER_DEVICE_API = "platform/v1/api/register/device/";

        public static final String SERVERS_STUN_DETAIL_V2_API = "v2/platform/servers/stun/detail";

        public static final String EULIX_XYZ_URL = "https://eulix.xyz/";
        public static final String EULIX_TOP_URL = "https://eulix.top/";
        public static final String DEV_EULIX_XYZ_URL = "https://dev.eulix.xyz/";
        public static final String TEST_EULIX_XYZ_URL = "https://test.eulix.xyz/";
        public static final String QA_EULIX_XYZ_URL = "https://qa.eulix.xyz/";
        public static final String SIT_EULIX_XYZ_URL = "https://sit.eulix.xyz/";
        public static final String AO_SPACE_URL = "https://ao.space/";
        public static final String LOGIN_API = "login";
        public static final String EN_LOGIN_API = "en/login";
        public static final String SUPPORT_HELP_API = "support/help";
        public static final String EN_SUPPORT_HELP_API = "en/support/help";
        public static final String SERVER_INVITE_MEMBERS_API = "invite/members";
        public static final String EN_SERVER_INVITE_MEMBERS_API = "en/invite/members";
        public static final String SERVER_LOCAL_API = "#/s_local";
        public static final String OTHER_LOCAL_NETWORK_API = "other/localnetwork";
        public static final String SERVER_GUIDE_API = "#/s_guide";
        public static final String OTHER_GUIDE_API = "other/guide";
        public static final String PRIVACY_API = "https://ao.space/opensource/privacy";
        public static final String EN_PRIVACY_API = "https://ao.space/en/opensource/privacy";
        public static final String AGREEMENT_API = "https://ao.space/opensource/agreement";
        public static final String EN_AGREEMENT_API = "https://ao.space/en/opensource/agreement";
        public static final String OTHER_GOTO_PATH = "other/goto?goto=/";
        public static final String EN_OTHER_GOTO_PATH = "en/other/goto?goto=/";

        public static final String FILE_LIST_API = "space/v1/api/file/list";
        public static final String FOLDER_INFO_API = "space/v1/api/folder/info";
        public static final String RENAME_FILE_API = "space/v1/api/file/rename";
        public static final String COPY_FILE_API = "space/v1/api/file/copy";
        public static final String MOVE_FILE_API = "space/v1/api/file/move";
        public static final String DELETE_FILE_API = "space/v1/api/file/delete";
        public static final String ASYNC_TASK_STATUS_API = "space/v1/api/async/task";
        public static final String CREATE_FOLDER_API = "space/v1/api/folder/create";
        public static final String SEARCH_FILE_API = "space/v1/api/file/search";
        public static final String FILE_INFO_API = "space/v1/api/file/info";
        public static final String RECYCLED_LIST_API = "space/v1/api/recycled/list";
        public static final String RECYCLED_RESTORE_API = "space/v1/api/recycled/restore";
        public static final String RECYCLED_CLEAR_API = "space/v1/api/recycled/clear";

        public static final String BASE_MAILBOX_URL_DEBUG = "http://192.168.2.37:8000/";
        public static final String MESSAGES_API = "space/v1/api/mailbox/message/";
        public static final String SEND_MESSAGES_API = "space/v1/api/mailbox/sendmessage";
        public static final String DELETE_MESSAGES_API = "space/v1/api/mailbox/deletemessage";

        public static final String BASE_GATEWAY_URL_DEBUG = "https://mybox.space.eulix.xyz/";
        public static final String SPACE_STATUS_API = "space/status";
        public static final String SPACE_POLL_API = "space/v1/api/gateway/poll";
        public static final String CREATE_AUTH_TOKEN_API = "space/v1/api/gateway/auth/token/create";
        public static final String REFRESH_AUTH_TOKEN_API = "space/v1/api/gateway/auth/token/refresh";
        public static final String REFRESH_BOX_KEY_AUTH_TOKEN_API = "space/v1/api/auth/bkey/refresh";
        public static final String VERIFY_AUTH_TOKEN_API = "space/v1/api/gateway/auth/token/verify";
        public static final String CALL_GATEWAY_API = "space/v1/api/gateway/call";
        public static final String UPLOAD_GATEWAY_API = "space/v1/api/gateway/upload/whole";
        public static final String MULTIPART_UPLOAD_GATEWAY_API = "space/v1/api/gateway/upload";
        public static final String DOWNLOAD_GATEWAY_API = "space/v1/api/gateway/download";
        public static final String NETWORK_CHANNEL_INFO_API = "space/v1/api/device/network/channel/info";
        public static final String NETWORK_CHANNEL_WAN_API = "space/v1/api/device/network/channel/wan";

        public static final String AGENT_INFO_API = "agent/info";
        public static final String PUBLIC_KEY_EXCHANGE_API = "agent/v1/api/pubkeyexchange";
        public static final String KEY_EXCHANGE_API = "agent/v1/api/keyexchange";
        public static final String PAIRING_API = "agent/v1/api/pairing";
        public static final String PAIRING_API_V2 = "agent/test/api/pairing";
        public static final String AUTH_INFO_API = "agent/v1/api/authinfo";
        public static final String SET_PASSWORD_API = "agent/v1/api/setpassword";
        public static final String ADMIN_REVOKE_API = "agent/v1/api/admin/revoke";
        public static final String RESET_API = "agent/v1/api/reset";
        public static final String INITIAL_API = "agent/v1/api/initial";
        public static final String PAIR_INIT_API = "agent/v1/api/pair/init";
        public static final String AGENT_PASSTHROUGH_API = "agent/v1/api/passthrough";
        public static final String SPACE_READY_CHECK_API = "agent/v1/api/space/ready/check";
        public static final String DISK_RECOGNITION_API = "agent/v1/api/disk/recognition";
        public static final String DISK_INITIALIZE_API = "agent/v1/api/disk/initialize";
        public static final String DISK_INITIALIZE_PROGRESS_API = "agent/v1/api/disk/initialize/progress";
        public static final String DISK_MANAGEMENT_LIST_API = "agent/v1/api/disk/management/list";
        public static final String SYSTEM_SHUTDOWN_API = "agent/v1/api/system/shutdown";
        public static final String SYSTEM_REBOOT_API = "agent/v1/api/system/reboot";
        public static final String DISK_MANAGEMENT_RAID_INFO_API = "agent/v1/api/disk/management/raid/info";
        public static final String DISK_MANAGEMENT_EXPAND_API = "agent/v1/api/disk/management/expand";
        public static final String DISK_MANAGEMENT_EXPAND_PROGRESS_API = "agent/v1/api/disk/management/expand/progress";
        public static final String NETWORK_CONFIG_API = "agent/v1/api/network/config";
        public static final String NETWORK_IGNORE_API = "agent/v1/api/network/ignore";
        public static final String DEVICE_ABILITY_API = "agent/v1/api/device/ability";
        public static final String BIND_COMMUNICATION_START_API = "agent/v1/api/bind/com/start";
        public static final String BIND_COMMUNICATION_PROGRESS_API = "agent/v1/api/bind/com/progress";
        public static final String BIND_SPACE_CREATE_API = "agent/v1/api/bind/space/create";
        public static final String BIND_REVOKE_API = "agent/v1/api/bind/revoke";
        public static final String BIND_INTERNET_SERVICE_CONFIG_API = "agent/v1/api/bind/internet/service/config";
        public static final String DID_DOCUMENT_API = "agent/v1/api/did/document";
        public static final String TEST_SERVICE_URL = "https://test.eulix.xyz/";
        public static final String SERVICE_URL = "https://eulix.xyz/";
        public static final String SERVICE_TOP_URL = "https://eulix.top/";
        public static final String DEV_SERVICE_URL = "https://dev.eulix.xyz/";
        public static final String QA_SERVICE_URL = "https://qa.eulix.xyz/";
        public static final String SIT_SERVICE_URL = "https://sit.eulix.xyz/";
        public static final String SERVICE_AO_SPACE_URL = "https://ao.space/";

        public static final String RC_WEB_BASE_URL = "https://eulix.xyz";
        public static final String RC_SPACE_API = "eulix.xyz";
        public static final String DEV_WEB_BASE_URL = "https://dev.eulix.xyz";
        public static final String DEV_SPACE_API = "dev-space.eulix.xyz";
        public static final String TEST_WEB_BASE_URL = "https://test.eulix.xyz";
        public static final String TEST_SPACE_API = "test-space.eulix.xyz";
        public static final String QA_WEB_BASE_URL = "https://qa.eulix.xyz";
        public static final String QA_SPACE_API = "qa-space.eulix.xyz";
        public static final String SIT_WEB_BASE_URL = "https://sit.eulix.xyz";
        public static final String SIT_SPACE_API = "sit-space.eulix.xyz";
        public static final String PROD_WEB_BASE_URL = "https://ao.space";
        public static final String PROD_SPACE_API = "ao.space";

        public static final String PLATFORM_ABILITY_API = "v2/platform/ability";
        public static final String PLATFORM_SWITCH_API = "agent/v1/api/switch";
        public static final String PLATFORM_SWITCH_STATUS_API = "agent/v1/api/switch/status";

        public static final String AUTH_PLATFORM_KEY_BOX_INFO_API = "platform/v1/api/auth/pkey/boxinfo";
        public static final String AUTH_PLATFORM_KEY_BOX_INFO_API_V2_PREFIX = "v2/platform/pkeys/";
        public static final String AUTH_PLATFORM_KEY_BOX_INFO_API_V2_SUFFIX = "/boxinfo";
        public static final String AUTH_BOX_KEY_CREATE_API = "space/v1/api/auth/bkey/create";
        public static final String AUTH_BOX_LOGIN_AUTH_KEY_CREATE_API = "space/v2/api/auth/totp/auth-code";
        public static final String AUTH_BOX_KEY_POLL_API = "space/v1/api/auth/bkey/poll";
        public static final String AUTH_BOX_LONGIN_AUTH_KEY_POLL_API = "space/v1/api/auth/totp/bkey/poll";
        public static final String AUTH_BOX_LONGIN_BKEY_VERIFY_API = "space/v1/api/auth/totp/bkey/verify";

        @Deprecated
        public static final String APP_INFO_CHECK = "platform/v1/api/appinfo/check";

        public static final String DEVICE_STORAGE_INFO = "space/v1/api/device/storage/info";
        public static final String PERSONALINFO_SHOW_API = "space/v1/api/personal/info";
        public static final String PERSONALINFO_UPDATE_API = "space/v1/api/personal/info/update";
        public static final String MEMBER_USED_STORAGE_API = "space/v1/api/member/used/storage";
        public static final String MEMBER_LIST_API = "space/v1/api/member/list";
        public static final String GATEWAY_CREATE_MEMBER_API = "space/v1/api/gateway/auth/member/create";
        public static final String GATEWAY_CREATE_MEMBER_TOKEN_API = "space/v1/api/gateway/auth/token/create/member";
        public static final String UPDATE_MEMBER_NAME_API = "space/v1/api/member/name/update";
        //成员解绑
        public static final String GATEWAY_REVOKE_MEMBER_API = "space/v1/api/gateway/auth/revoke/member";
        //管理员解绑
        public static final String GATEWAY_REVOKE_ADMIN_API = "space/v1/api/gateway/auth/revoke";
        // 终端列表
        public static final String TERMINAL_INFO_API = "space/v1/api/authorized/terminal/all/info";
        // 终端下线
        public static final String TERMINAL_INFO_DELETE_API = "space/v1/api/authorized/terminal/info/delete";

        //盒子系统升级查询
        public static final String GATEWAY_VERSION_BOX = "space/v1/api/gateway/version/box";
        //app升级查询
        public static final String GATEWAY_VERSION_APP = "space/v1/api/gateway/version/app";
        //获取当前盒子系统版本
        public static final String GATEWAY_CURRENT_BOX_VERSION = "space/v1/api/gateway/version/box/current";
        // app和盒子版本
        public static final String GATEWAY_VERSION_COMPATIBLE = "space/v1/api/gateway/version/compatible";
        //系统自动升级配置
        public static final String AGENT_UPGRADE_CONFIG = "agent/v1/api/upgrade/config";
        //查询升级状态
        public static final String AGENT_UPGRADE_STATUS = "agent/v1/api/upgrade/status";
        //开始升级
        public static final String AGENT_UPGRADE_START_UPGRADE = "agent/v1/api/upgrade/start-upgrade";
        //拉取镜像
        public static final String AGENT_UPGRADE_START_PULL = "agent/v1/api/upgrade/start-down";
        //获取设备信息详情
        public static final String AGENT_DEVICE_INFO = "agent/v1/api/device/version";

        public static final String LOCAL_IPS_API = "agent/v1/api/pair/net/localips";
        public static final String NET_CONFIG_API = "agent/v1/api/pair/net/netconfig";

        public static final String ADMINISTRATOR_DEVELOP_OPTIONS_SWITCH_API = "space/v1/api/admin/dev-options/switch";

        //断点续传相关：
        //创建上传任务
        public static final String MULTI_UPLOAD_CREATE_API = "space/v1/api/multipart/create";
        //上传分片
        public static final String MULTI_UPLOAD_UPLOAD_API = "space/v1/api/multipart/upload";
        //获取已上传片段列表
        public static final String MULTI_UPLOAD_LIST_API = "space/v1/api/multipart/list";
        //合并已上传片段
        public static final String MULTI_UPLOAD_COMPLETE_API = "space/v1/api/multipart/complete";
        //删除已上传片段
        public static final String MULTI_UPLOAD_DELETE_API = "space/v1/api/multipart/delete";
        //文件下载
        public static final String FILE_DOWNLOAD_API = "space/v1/api/file/download";

        //获取https证书
        public static final String GET_HTTPS_CERT_API = "agent/v1/api/cert/get";

        public static final String AUTH_AUTO_LOGIN_API = "space/v1/api/auth/auto/login";

        public static final String AUTH_AUTO_LOGIN_POLL_API = "space/v1/api/auth/auto/login/poll";

        public static final String AUTH_AUTO_LOGIN_CONFIRM_API = "space/v1/api/auth/auto/login/confirm";

        public static final String GET_NOTIFICATION_API = "space/v1/api/notification";
        public static final String NOTIFICATION_ALL_API = "space/v1/api/notification/all";
        public static final String NOTIFICATION_ALL_DELETE_API = "space/v1/api/notification/all/delete";

        // 安全设置相关
        public static final String GET_DEVICE_HARDWARE_INFO_API = "space/v1/api/device/hardware/info";
        public static final String BINDER_MODIFY_SECURITY_PASSWORD_API = "space/v1/api/security/passwd/modify/binder";
        public static final String VERIFY_SECURITY_PASSWORD_API = "space/v1/api/security/passwd/verify";
        public static final String BINDER_RESET_SECURITY_PASSWORD_API = "space/v1/api/security/passwd/reset/binder";
        public static final String GRANTEE_APPLY_MODIFY_SECURITY_PASSWORD_API = "space/v1/api/security/passwd/modify/auther/apply";
        public static final String BINDER_ACCEPT_MODIFY_SECURITY_PASSWORD_API = "space/v1/api/security/passwd/modify/binder/accept";
        public static final String GRANTEE_APPLY_RESET_SECURITY_PASSWORD_API = "space/v1/api/security/passwd/reset/auther/apply";
        public static final String BINDER_ACCEPT_RESET_SECURITY_PASSWORD_API = "space/v1/api/security/passwd/reset/binder/accept";
        public static final String GRANTEE_MODIFY_SECURITY_PASSWORD_API = "space/v1/api/security/passwd/modify/auther";
        public static final String GRANTEE_RESET_SECURITY_PASSWORD_API = "space/v1/api/security/passwd/reset/auther";
        public static final String SECURITY_MESSAGE_POLL_API = "space/v1/api/security/message/poll";

        //在线播放相关
        //查询是否支持
        public static final String MEDIA_VOD_CHECK = "space/v1/api/vod/check";
    }

    public static class HttpRequestMethod {
        public static final String GET = "GET";
        public static final String POST = "POST";
    }

    public static class AgentApi {
        public static class ServiceName {
            public static final String EULIXSPACE_GATEWAY = "eulixspace-gateway";
        }

        public static class ApiVersion {
            public static final String V1 = "v1";
        }

        public static class ApiPath {
            public static final String SECURITY_PASSWORD_RESET_BINDER_LOCAL = "/api/security/passwd/reset/binder/local";
            public static final String SECURITY_PASSWORD_RESET_AUTHORIZED_LOCAL = "/api/security/passwd/reset/auther/local";
            public static final String SECURITY_PASSWORD_RESET_NEW_DEVICE_LOCAL = "/api/security/passwd/reset/newdevice/local";
            public static final String SECURITY_PASSWORD_RESET_NEW_DEVICE_APPLY_LOCAL = "/api/security/passwd/reset/newdevice/apply/local";
            public static final String SECURITY_MESSAGE_POLL_LOCAL = "/api/security/message/poll/local";
        }
    }

    public static class KnownError {
        public static final int MEMBER_LIST_ERROR_CODE = 4012;
        public static final int AUTHORIZATION_MEMBER_ERROR_CODE = 4018;

        public static class BindError {
            public static final int BAD_REQUEST = 400;
            public static final int BOUND_CODE = 460;
            public static final int CONTAINER_STARTING = 470;
            public static final int CONTAINER_STARTED = 471;
            public static final int SERVER_ERROR = 500;
            public static final int CALL_SERVICE_FAILED = 560;
        }

        public static class DomainError {
            public static final int DOMAIN_RESERVED = 2051;
            public static final int DOMAIN_EXIST = 2018;
            public static final int DOMAIN_MODIFY_ONCE_YEAR_CODE = 4022;
            public static final int DOMAIN_ERROR = 4001;
        }

        public static class TerminalError {
            public static final int TERMINAL_OFFLINE_DUPLICATE_CODE = 4034;
        }

        public static class AutoLoginError {
            public static final int CONTINUE_WAITING = 4044;
            public static final int AUTO_LOGIN_INVALID = 4045;
            public static final int LOGIN_REFUSE = 4046;
        }

        public static class GatewayCallError {
            public static final int INTERNAL_SERVER_ERROR = 500;
            public static final int ACCESS_TOKEN_ERROR = 4015;
        }

        public static class NotificationError {
            public static final int MESSAGE_NOT_EXIST = 5005;
        }

        public static class AccountCommonError {
            public static final int ACCOUNT_403 = 403;
            public static final int ACCOUNT_404 = 404;
            public static final int ACCOUNT_410 = 410;
            public static final int NICK_REPEAT = 400;
        }

        public static class GatewayCommonError {
            public static final int GATEWAY_406 = 406;
            public static final int GATEWAY_461 = 461;
        }

        public static class EulixSecurityError {
            public static final int AUTHENTICATION_FAIL = 4011;
            public static final int BOUNDED_MAILBOX = 4051;
            public static final int VERIFICATION_EXPIRE = 4052;
            public static final int SECURITY_TOKEN_EXPIRE = 4053;
        }

        public static class DiskInitializationError {
            public static final int DISK_NOT_PAIR = 462;
        }

        public static class SwitchPlatformError {
            public static final int PARAM_ERROR = 401;
            public static final int RESOURCE_BUSY_ERROR = 402;
            public static final int DOMAIN_ERROR = 570;
            public static final int TO_NEW_SSP_ERROR = 571;
            public static final int NETWORK_TEST_ERROR = 572;
            public static final int RECALL_GATEWAY_ERROR = 573;
            public static final int DOING_ERROR = 574;
            public static final int GET_ACCOUNT_ERROR = 575;
            public static final int MIGRATE_ERROR = 576;
            public static final int TASK_NOT_FOUND_ERROR = 580;
            public static final int CONNECT_ERROR = 581;

            public static final int REDIRECT_INVALID_ERROR = 460;
            public static final int DOMAIN_NON_EXIST_ERROR = 400;
        }

        public static class FileError {
            //文件操作时间长，需异步执行
            public static final int OPERATE_ASYNC_TASK_CODE = 201;
        }

    }

    public static class KnownSource {
        public static final String GATEWAY = "GW";
        public static final String ACCOUNT = "ACC";
        public static final String AGENT = "AG";
    }

    public static class Role {
        public static final String ADMINISTRATOR = "ADMINISTRATOR";
        public static final String GUEST = "GUEST";
    }



    public static class QAEnvironment {
        public static final String BLE_KEY = "51ff2e5142f133621052dcadb804b059";
        public static final String BLE_IV = "v3/QEXuw06ZVbMbiCYu4Hw==";
    }

    public static final String FRAG_FILE = "tab_file";
    public static final String FRAG_MINE = "tab_mine";
    public static final String FRAG_FILE_ALL = "file_all";
    public static final String FRAG_FILE_IMAGE = "file_image";
    public static final String FRAG_FILE_VIDEO = "file_video";
    public static final String FRAG_FILE_DOCUMENT = "file_document";
    public static final String FRAG_FILE_OTHER = "file_other";

    public static final String FRAGMENT_TAG = "fragment_tag";
    public static final String FRAGMENT_TARGET = "fragment_target";

    //媒体文件类型
    public static class MediaType {
        public static final int MEDIA_IMAGE = 1;
        public static final int MEDIA_VIDEO = 2;
        public static final int MEDIA_FILE = 3;
        public static final int MEDIA_IMAGE_AND_VIDEO = 4;
    }

    //文件传输类型
    public static class TransferType {
        public static final int TYPE_DOWNLOAD = TransferHelper.TYPE_DOWNLOAD;  //下载
        public static final int TYPE_UPLOAD = TransferHelper.TYPE_UPLOAD;    //上传
    }

    private ConstantField() {
        throw new AssertionError("not allow to be instantiation!");
    }

    //path
    public static final String SDCARD_ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final String APP_PATH = "AO.Space";
    public static final String LOCAL_CAMERA_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
    //相册-缩略图缓存路径
    public static final String ALBUM_THUMBS_CACHE_PATH = "/album/thumbs";
    //相册-压缩图缓存路径
    public static final String ALBUM_COMPRESSED_CACHE_PATH = "/album/compressed";
    //文件-缩略图缓存路径
    public static final String FILE_THUMBS_CACHE_PATH = "/thumbs";
    //文件-缓存目录
    public static final String FILE_CACHE_PATH = "/cache";
    //文件-压缩图缓存路径
    public static final String FILE_COMPRESSED_CACHE_PATH = "/compressed";
    //相册-缓存目录
    public static final String ALBUM_CACHE_PATH = "/album/cache";

    //图片查询字段
    public final static String[] IMAGE_PROJECTION = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.PICASA_ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.TITLE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_MODIFIED
    };
    //视频查询字段
    public final static String[] VIDEO_PROJECTION = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.MIME_TYPE
    };


    public static final class ScreenShotKeyword {
        public static final String[] SCREEN_SHOT_KEYWORDS_PREFIX = {"screen"};
        public static final String[] SCREEN_SHOT_KEYWORDS_SUFFIX = {"shot", "capture", "cap"};
    }


    //系统升级整体流程状态："", downloading, downloaded, installing, installed, download-err，install-err
    public final static class UpgradeStatus {
        public static final String STATUS_PULLING = "downloading";
        public static final String STATUS_PULLED = "downloaded";
        public static final String STATUS_UPPING = "installing";
        public static final String STATUS_UPPED = "installed";
        public static final String STATUS_PULL_ERR = "download-err";
        public static final String STATUS_UP_ERR = "install-err";
    }

    //盒子系统升级数据
    public static VersionCheckResponseBody.Results boxVersionCheckBody;


    //接口返回错误码（注意添加注释！！）
    public static class ErrorCode {
        //恢复数据，校验安全密码失败
        public static final int RESTORE_PASSWORD_ERROR = 1026;
        //空间服务平台不通
        public static final int SPACE_PLATFORM_CONNECT_ERROR = 5005;
        //产品服务平台不通
        public static final int PRODUCT_PLATFORM_CONNECT_ERROR = 5006;
    }

    /**
     * 用户在盒子的身份
     */
    public static class UserIdentity {
        // 无身份
        public static final int NO_IDENTITY = 0;
        // 成员登录
        public static final int MEMBER_GRANTEE = NO_IDENTITY + 1;
        // 管理员登录
        public static final int ADMINISTRATOR_GRANTEE = MEMBER_GRANTEE + 1;
        // 成员
        public static final int MEMBER_IDENTITY = ADMINISTRATOR_GRANTEE + 1;
        // 管理员
        public static final int ADMINISTRATOR_IDENTITY = MEMBER_IDENTITY + 1;
    }

    //是否运行使用手机流量传输
    public static boolean sIAllowTransferWithMobileData = false;

    //"所有图片"相册id
    public static String ALL_IMAGES_BUCKET_ID = "10000000";

    //是否点击了 系统升级弹框-稍后安装
    public static boolean hasClickSystemUpgradeInstallLater = false;

    //上传业务来源
    public static class UploadBusinessIdType {
        //默认业务
        public static final int TYPE_DEFAULT = 0;
        //同步业务来源
        public static final int TYPE_SYNC = 1;
        //智能相册上传
        public static final int TYPE_ALBUM = 2;
    }

    //二维码扫描参数类型
    public static class QrScanParamType {
        //从平台登录业务类型
        public static final String BT_PLATFORM_LOGIN = "platform-login";
        //从盒子登录业务类型，包括局域网、用户域名
        public static final String BT_BOX_LOGIN = "box-login";
    }

    //文件异步操作状态类型
    public static class FileAsyncTaskStatus {
        public static final String STATUS_INIT = "init";
        public static final String STATUS_PROCESSING = "processing";
        public static final String STATUS_SUCCESS = "success";
        public static final String STATUS_FAILED = "failed";
    }
}
