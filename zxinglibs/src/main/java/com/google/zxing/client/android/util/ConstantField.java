package com.google.zxing.client.android.util;

import android.os.Environment;
import android.provider.MediaStore;

public class ConstantField {
    public static final String SCAN_MODE = "scan_mode";
    public static final String DATA_UUID = "data_uuid";
    public static final String LOCAL_CAMERA_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();

    public static final String FUNCTION_JSON = "function_json";

    public static final String LOCAL_GALLERY_JSON = "local_gallery_json";

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

    public static class TimeStampFormat {
        public static final String DATE_FORMAT = "yyyy/MM/dd HH:mm";
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
        public static final int ACCEPT_MEMBER = CONTACTS_PERMISSION + 1;

        public static final int LOCAL_IMAGE_CODE = 65537;

        public static final int EULIX_SPACE_JOB_ID = 2000;
        public static final int EULIX_SPACE_FOREGROUND_ID = EULIX_SPACE_JOB_ID + 1;
        public static final int EULIX_SPACE_NOTIFICATION_START_ID = EULIX_SPACE_FOREGROUND_ID + 1;
    }

    //媒体文件类型
    public static class MediaType {
        public static final int MEDIA_IMAGE = 1;
        public static final int MEDIA_VIDEO = 2;
        public static final int MEDIA_FILE = 3;
        public static final int MEDIA_IMAGE_AND_VIDEO = 4;
    }

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

    //"所有图片"相册id
    public static String ALL_IMAGES_BUCKET_ID = "10000000";
}
