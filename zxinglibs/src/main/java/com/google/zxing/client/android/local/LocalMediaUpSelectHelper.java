package com.google.zxing.client.android.local;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.google.zxing.client.android.R;
import com.google.zxing.client.android.bean.LocalMediaUpItem;
import com.google.zxing.client.android.bean.PhotoUpImageBucket;
import com.google.zxing.client.android.util.ConstantField;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class LocalMediaUpSelectHelper extends AsyncTask<Object, Object, Object> {
    private Context context;
    private ContentResolver contentResolver;
    //图片集列表
    private HashMap<String, PhotoUpImageBucket> bucketList = new HashMap<>();
    //图片、视频集列表（包含图片、视频）
    private HashMap<String, PhotoUpImageBucket> bucketImageAndVideoList = new HashMap<>();
    private GetAlbumListListener getAlbumListListener;
    private GetCameraMediaListener getCameraMediaListener;
    //全部文件列表，不区分文件夹
    private List<LocalMediaUpItem> totalFilesList = new ArrayList<>();
    //本地相册目录下的所有图片和视频
    private List<LocalMediaUpItem> cameraMediaList = new ArrayList<>();
    //本地相册目录下图片
    private List<LocalMediaUpItem> cameraImageList = new ArrayList<>();
    //本地相册目录下视频
    private List<LocalMediaUpItem> cameraVideoList = new ArrayList<>();

    //文件类型
    private int mediaType;

    //文件过滤开关、大小限制
    private boolean mLimitSwitch = false;
    private long mLimitSize = 300 * 1024 * 1024;

    /**
     * 是否创建了图片集
     */
    boolean hasBuildImagesBucketList = false;

    boolean hasBuildCameraList = false;
    //是否包含gif图片
    private boolean includeGif = true;
    //是否生成全部文件相簿
    private boolean createAllFlag = false;
    //是否来自相册同步（仅限Camera下文件，同时按照修改时间正序排列）
    private boolean isFromSync = false;

    private String mLocalAllImageText = null;

    private String FILE_COLUMNS_DATA = MediaStore.Files.FileColumns.DATA;

    //文件字段
    private final static String[] FILE_PROJECTION = {
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.TITLE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE
    };


    private LocalMediaUpSelectHelper() {
    }

    public static LocalMediaUpSelectHelper getHelper() {
        LocalMediaUpSelectHelper instance = new LocalMediaUpSelectHelper();
        return instance;
    }

    /**
     * 初始化
     *
     * @param context
     */
    public void init(Context context, int mediaType) {
        if (this.context == null) {
            this.context = context.getApplicationContext();
            contentResolver = context.getContentResolver();
        }
        this.mediaType = mediaType;
    }

    public void init(Context context, int mediaType, String localAllImageText) {
        if (this.context == null) {
            this.context = context.getApplicationContext();
            contentResolver = context.getContentResolver();
        }
        this.mediaType = mediaType;
        mLocalAllImageText = localAllImageText;
    }

    public void setIncludeGif(boolean includeGif) {
        this.includeGif = includeGif;
    }

    //限制文件大小开关
    public void setLimitMaxSize(boolean isLimitMaxSize) {
        this.mLimitSwitch = isLimitMaxSize;
    }

    //设置仅获取相机目录下内容
    public void setFromSync(boolean fromSync) {
        this.isFromSync = fromSync;
    }

    //创建全部图片相簿
    public void setCreateAll(boolean createAll) {
        this.createAllFlag = createAll;
    }

    @Override
    protected Object doInBackground(Object... params) {
        if (mediaType == ConstantField.MediaType.MEDIA_IMAGE_AND_VIDEO) {
            return getCameraImageAndVideo((Boolean) (params[0]));
        } else {
            return getMediaBucketList((Boolean) (params[0]));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onPostExecute(Object result) {
        super.onPostExecute(result);
        if (mediaType == ConstantField.MediaType.MEDIA_IMAGE_AND_VIDEO) {
            getCameraMediaListener.onGetCameraMedia((List<PhotoUpImageBucket>) result, cameraMediaList, cameraImageList, cameraVideoList);
        } else {
            getAlbumListListener.onGetAlbumList((List<PhotoUpImageBucket>) result, totalFilesList);
        }
    }

    /**
     * 得到图片/视频集
     */
    void buildMediaBucketList() {
        Cursor cur;
        // 获取指定列的索引
        int photoIDIndex;
        int photoPathIndex;
        int bucketDisplayNameIndex;
        int bucketIdIndex;
        int videoDurationIndex = 0;
        int fileSizeIndex = -1;
        int dateIndex;
        int mimeTypeIndex;
        if (mediaType == ConstantField.MediaType.MEDIA_IMAGE) {
            cur = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ConstantField.IMAGE_PROJECTION, null, null,
                    MediaStore.Images.Media.DATE_MODIFIED + " desc");

            photoIDIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            photoPathIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            bucketDisplayNameIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            bucketIdIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);
            fileSizeIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
            dateIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED);
            mimeTypeIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE);
        } else if (mediaType == ConstantField.MediaType.MEDIA_VIDEO) {
            cur = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, ConstantField.VIDEO_PROJECTION, null, null,
                    MediaStore.Video.Media.DATE_MODIFIED + " desc");

            photoIDIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            photoPathIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            bucketDisplayNameIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
            bucketIdIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID);
            videoDurationIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            fileSizeIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
            dateIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED);
            mimeTypeIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE);
        } else {
            //支持文件类型：txt,doc,docx,xls,xlsx,ppt,pptx,md,pdf,caj,kdh,nh
            //doc、docx、xls、xlsx、ppt、pptx、txt、pdf、caj、kdh、nh
            //音频类：mp3、wav、wma
            //压缩包类：tar、gz、rar、zip、7z
            //apk
            String selection = "(" + FILE_COLUMNS_DATA + " LIKE '%.txt'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.doc'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.docx'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.xls'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.xlsx'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.ppt'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.pptx'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.md'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.pdf'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.caj'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.kdh'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.nh'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.tar'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.gz'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.rar'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.zip'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.7z'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.apk'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.mp3'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.wav'" +
                    " or " + FILE_COLUMNS_DATA + " LIKE '%.wma'" +
                    ")";
            String[] selectionArgs = null;

            cur = contentResolver.query(MediaStore.Files.getContentUri("external"), FILE_PROJECTION, selection, selectionArgs,
                    MediaStore.Files.FileColumns.DATE_MODIFIED + " desc");
            photoIDIndex = cur.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
            photoPathIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            bucketDisplayNameIndex = cur.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME);
            bucketIdIndex = cur.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID);
            fileSizeIndex = cur.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
            dateIndex = cur.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
            mimeTypeIndex = cur.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);
        }
        if (cur.moveToFirst()) {
            do {
                int typeIndex = cur.getString(photoPathIndex).lastIndexOf(".");
                String fileName = null;
                try {
                    fileName = cur.getString(photoPathIndex).substring(
                            cur.getString(photoPathIndex).lastIndexOf("/") + 1,
                            typeIndex);
                } catch (Exception e) {
                    Log.e("zfy", e.getMessage());
                }
                if (TextUtils.isEmpty(fileName)) {
                    continue;
                }
                String suffix = cur.getString(photoPathIndex).substring(typeIndex + 1);
                if (suffix.length() <= 0 || cur.getLong(fileSizeIndex) <= 0) {
                    //过滤没有后缀名 or 大小为0的文件
                    continue;
                } else if (suffix.equals("gif") && !includeGif) {
                    //过滤gif文件
                    continue;
                } else {
                    String _id = cur.getString(photoIDIndex);
                    String path = cur.getString(photoPathIndex);
                    String bucketName = cur.getString(bucketDisplayNameIndex);

                    File file = new File(path);
                    if (!file.exists()) {
                        Log.d("zfy", "file not exist." + file.getAbsolutePath());
                        continue;
                    }

                    String bucketId = cur.getString(bucketIdIndex);
                    long fileSize = cur.getLong(fileSizeIndex);
                    if (mLimitSwitch && fileSize > mLimitSize) {
                        //文件大小超过限制
                        Log.d("zfy", fileName + ", file over size: " + fileSize);
                        continue;
                    }
                    long modifiedDate = cur.getLong(dateIndex);
                    String mimeType = cur.getString(mimeTypeIndex);
                    PhotoUpImageBucket bucket = bucketList.get(bucketId);
                    if (bucket == null) {
                        bucket = new PhotoUpImageBucket();
                        bucket.setBucketId(bucketId);
                        bucketList.put(bucketId, bucket);
                        bucket.setImageList(new ArrayList<LocalMediaUpItem>());
                        bucket.setBucketName(bucketName);
                    }
                    bucket.setCount(bucket.getCount() + 1);
                    LocalMediaUpItem imageItem = new LocalMediaUpItem();
                    imageItem.setMediaId(_id);
                    imageItem.setDisplayName(fileName + "." + suffix);
                    imageItem.setMediaPath(path);
                    imageItem.setSize(fileSize);
                    imageItem.setModifiedDate(modifiedDate);
                    imageItem.setMimeType(mimeType);
                    if (mediaType == ConstantField.MediaType.MEDIA_VIDEO) {
                        String duration = cur.getString(videoDurationIndex);
                        imageItem.setDuration(duration);
                    }
                    bucket.getImageList().add(imageItem);
                    totalFilesList.add(imageItem);
                }
            } while (cur.moveToNext());
        }
        cur.close();
        hasBuildImagesBucketList = true;
    }


    /**
     * 得到图片集
     *
     * @param refresh
     * @return
     */
    private List<PhotoUpImageBucket> getMediaBucketList(boolean refresh) {
        if (refresh || !hasBuildImagesBucketList) {
            buildMediaBucketList();
        }
        List<PhotoUpImageBucket> tmpList = new ArrayList<>();
        Iterator<Map.Entry<String, PhotoUpImageBucket>> itr = bucketList.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, PhotoUpImageBucket> entry = itr.next();
            tmpList.add(entry.getValue());
        }

        if (createAllFlag && tmpList.size() > 0) {
            //创建“所有图片”相簿
            PhotoUpImageBucket allImageBucket = new PhotoUpImageBucket();
            allImageBucket.setBucketId(ConstantField.ALL_IMAGES_BUCKET_ID);
            if (mLocalAllImageText == null || TextUtils.isEmpty(mLocalAllImageText)) {
                allImageBucket.setBucketName(context.getResources().getString(R.string.all_pictures));
            } else {
                allImageBucket.setBucketName(mLocalAllImageText);
            }
            allImageBucket.setImageList(totalFilesList);
            allImageBucket.setCount(totalFilesList.size());
            tmpList.add(0, allImageBucket);
        }

        if (mediaType == ConstantField.MediaType.MEDIA_IMAGE || mediaType == ConstantField.MediaType.MEDIA_VIDEO) {
            // 文件夹按内容数量排序
            Collections.sort(tmpList, new Comparator<PhotoUpImageBucket>() {
                @Override
                public int compare(PhotoUpImageBucket o1, PhotoUpImageBucket o2) {
                    if (o1 == null || o2 == null || o1.getImageList() == null || o2.getImageList() == null) {
                        return 0;
                    }
                    int lSize = o1.getImageList().size();
                    int rSize = o2.getImageList().size();
                    return Integer.compare(rSize, lSize);
                }
            });
        }
        return tmpList;
    }

    //获取本地相册路径下的图片和视频
    private void buildCameraMediaList(int mediaType) {
        Cursor cur;
        // 获取指定列的索引
        int photoIDIndex;
        int photoPathIndex;
        int bucketDisplayNameIndex;
        int bucketIdIndex;
        int videoDurationIndex = 0;
        int fileSizeIndex = -1;
        int dateIndex;
        int mimeTypeIndex;

        String selection = null;
        //获取图片
        if (mediaType == ConstantField.MediaType.MEDIA_IMAGE) {
            String sortOrder = " desc";
            if (isFromSync) {
                selection = "( " + MediaStore.Images.Media.DATA + " like '" + ConstantField.LOCAL_CAMERA_PATH + "/Camera%' or " +
                        MediaStore.Images.Media.DATA + " like '" + ConstantField.LOCAL_CAMERA_PATH + "/camera%' or " +
                        MediaStore.Images.Media.DATA + " like '" + ConstantField.LOCAL_CAMERA_PATH + "/100ANDRO%' )";
                sortOrder = " asc";
            }

            cur = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ConstantField.IMAGE_PROJECTION, selection, null,
                    MediaStore.Images.Media.DATE_MODIFIED + sortOrder);

            photoIDIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            photoPathIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            bucketDisplayNameIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            bucketIdIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);
            fileSizeIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
            dateIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED);
            mimeTypeIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE);
        } else {
            //获取视频
            String sortOrder = " desc";
            if (isFromSync) {
                selection = "( " + MediaStore.Video.Media.DATA + " like '" + ConstantField.LOCAL_CAMERA_PATH + "/Camera%' or " +
                        MediaStore.Video.Media.DATA + " like '" + ConstantField.LOCAL_CAMERA_PATH + "/camera%' or " +
                        MediaStore.Video.Media.DATA + " like '" + ConstantField.LOCAL_CAMERA_PATH + "/100ANDRO%' )";
                sortOrder = " asc";
            }

            cur = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, ConstantField.VIDEO_PROJECTION, selection, null,
                    MediaStore.Video.Media.DATE_MODIFIED + sortOrder);

            photoIDIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            photoPathIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            bucketDisplayNameIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
            bucketIdIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID);
            videoDurationIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            fileSizeIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
            dateIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED);
            mimeTypeIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE);
        }
        if (cur.moveToFirst()) {
            do {
                int typeIndex = cur.getString(photoPathIndex).lastIndexOf(".");
                String fileName = null;
                try {
                    fileName = cur.getString(photoPathIndex).substring(
                            cur.getString(photoPathIndex).lastIndexOf("/") + 1,
                            typeIndex);
                } catch (Exception e) {
                    Log.e("zfy", e.getMessage());
                }
                if (TextUtils.isEmpty(fileName)) {
                    continue;
                }
                String suffix = cur.getString(photoPathIndex).substring(typeIndex + 1);
                if (suffix.length() <= 0 || cur.getLong(fileSizeIndex) <= 0) {
                    //过滤没有后缀名 or 大小为0的文件
                } else {
                    String _id = cur.getString(photoIDIndex);
                    String path = cur.getString(photoPathIndex);
                    long fileSize = cur.getLong(fileSizeIndex);
                    long modifiedDate = cur.getLong(dateIndex);
                    String mimeType = cur.getString(mimeTypeIndex);
                    String bucketId = cur.getString(bucketIdIndex);
                    String bucketName = cur.getString(bucketDisplayNameIndex);

                    LocalMediaUpItem mediaItem = new LocalMediaUpItem();
                    mediaItem.setMediaId(_id);
                    mediaItem.setMediaPath(path);
                    mediaItem.setSize(fileSize);
                    mediaItem.setDisplayName(fileName + "." + suffix);
                    mediaItem.setModifiedDate(modifiedDate);
                    mediaItem.setMimeType(mimeType);
                    mediaItem.setBucketId(bucketId);
                    mediaItem.setBucketName(bucketName);
                    if (mediaType == ConstantField.MediaType.MEDIA_VIDEO) {
                        String duration = cur.getString(videoDurationIndex);
                        mediaItem.setDuration(duration);
                        cameraVideoList.add(mediaItem);
                    } else {
                        cameraImageList.add(mediaItem);
                    }

                    hasBuildCameraList = true;

                }
            } while (cur.moveToNext());
        }
        cur.close();
    }

    //获取本地相册路径下的图片和视频
    public List<PhotoUpImageBucket> getCameraImageAndVideo(boolean refresh) {
        if (refresh || !hasBuildCameraList) {

            buildCameraMediaList(ConstantField.MediaType.MEDIA_IMAGE);
            buildCameraMediaList(ConstantField.MediaType.MEDIA_VIDEO);
            //按顺序合并图像、视频列表
            int videoIndex = 0;
            int imageIndex = 0;
            while (videoIndex < cameraVideoList.size() && imageIndex < cameraImageList.size()) {
                LocalMediaUpItem itemVideo = cameraVideoList.get(videoIndex);
                LocalMediaUpItem itemImage = cameraImageList.get(imageIndex);
                LocalMediaUpItem tmpItem;
                if (isFromSync) {
                    if (itemVideo.getModifiedDate() < itemImage.getModifiedDate()) {
                        tmpItem = itemVideo;
                        videoIndex++;
                    } else {
                        tmpItem = itemImage;
                        imageIndex++;
                    }
                } else {
                    if (itemVideo.getModifiedDate() > itemImage.getModifiedDate()) {
                        tmpItem = itemVideo;
                        videoIndex++;
                    } else {
                        tmpItem = itemImage;
                        imageIndex++;
                    }
                }
                cameraMediaList.add(tmpItem);
                PhotoUpImageBucket bucket = bucketImageAndVideoList.get(tmpItem.getBucketId());
                if (bucket == null) {
                    bucket = new PhotoUpImageBucket();
                    bucket.setBucketId(tmpItem.getBucketId());
                    bucketImageAndVideoList.put(tmpItem.getBucketId(), bucket);
                    bucket.setImageList(new ArrayList<LocalMediaUpItem>());
                    bucket.setBucketName(tmpItem.getBucketName());
                }
                bucket.getImageList().add(tmpItem);
            }
            if (imageIndex < cameraImageList.size()) {
                while (imageIndex < cameraImageList.size()) {
                    cameraMediaList.add(cameraImageList.get(imageIndex));
                    PhotoUpImageBucket bucket = bucketImageAndVideoList.get(cameraImageList.get(imageIndex).getBucketId());
                    if (bucket == null) {
                        bucket = new PhotoUpImageBucket();
                        bucket.setBucketId(cameraImageList.get(imageIndex).getBucketId());
                        bucketImageAndVideoList.put(cameraImageList.get(imageIndex).getBucketId(), bucket);
                        bucket.setImageList(new ArrayList<LocalMediaUpItem>());
                        bucket.setBucketName(cameraImageList.get(imageIndex).getBucketName());
                    }
                    imageIndex++;
                }
            }
            if (videoIndex < cameraVideoList.size()) {
                while (videoIndex < cameraVideoList.size()) {
                    cameraMediaList.add(cameraVideoList.get(videoIndex));
                    PhotoUpImageBucket bucket = bucketImageAndVideoList.get(cameraVideoList.get(videoIndex).getBucketId());
                    if (bucket == null) {
                        bucket = new PhotoUpImageBucket();
                        bucket.setBucketId(cameraVideoList.get(videoIndex).getBucketId());
                        bucketImageAndVideoList.put(cameraVideoList.get(videoIndex).getBucketId(), bucket);
                        bucket.setImageList(new ArrayList<LocalMediaUpItem>());
                        bucket.setBucketName(cameraVideoList.get(videoIndex).getBucketName());
                    }
                    videoIndex++;
                }
            }

            //创建相簿
            List<PhotoUpImageBucket> tmpList = new ArrayList<>();
            Iterator<Map.Entry<String, PhotoUpImageBucket>> itr = bucketImageAndVideoList.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, PhotoUpImageBucket> entry = itr.next();
                tmpList.add(entry.getValue());
            }

            if (createAllFlag && tmpList.size() > 0) {
                //创建“所有图片”相簿
                PhotoUpImageBucket allImageBucket = new PhotoUpImageBucket();
                allImageBucket.setBucketId(ConstantField.ALL_IMAGES_BUCKET_ID);
                allImageBucket.setBucketName(context.getResources().getString(R.string.all_file));
                allImageBucket.setImageList(cameraMediaList);
                allImageBucket.setCount(cameraMediaList.size());
                tmpList.add(0, allImageBucket);
            }

            // 文件夹按内容数量排序
            Collections.sort(tmpList, new Comparator<PhotoUpImageBucket>() {
                @Override
                public int compare(PhotoUpImageBucket o1, PhotoUpImageBucket o2) {
                    if (o1 == null || o2 == null || o1.getImageList() == null || o2.getImageList() == null) {
                        return 0;
                    }
                    int lSize = o1.getImageList().size();
                    int rSize = o2.getImageList().size();
                    return lSize == rSize ? 0 : (lSize < rSize ? 1 : -1);
                }
            });
            return tmpList;
        }
        return null;
    }

    public void destoryList() {
        bucketList.clear();
        bucketList = null;
        bucketImageAndVideoList.clear();
        bucketImageAndVideoList = null;
        totalFilesList.clear();
        totalFilesList = null;
        cameraVideoList.clear();
        cameraVideoList = null;
        cameraImageList.clear();
        cameraImageList = null;
    }

    public void setGetAlbumListListener(GetAlbumListListener getAlbumListListener) {
        this.getAlbumListListener = getAlbumListListener;
    }

    public void setGetCameraMediaListener(GetCameraMediaListener getCameraMediaListener) {
        this.getCameraMediaListener = getCameraMediaListener;
    }

    public interface GetAlbumListListener {
        void onGetAlbumList(List<PhotoUpImageBucket> list, List<LocalMediaUpItem> totalFilesList);
    }

    public interface GetCameraMediaListener {
        void onGetCameraMedia(List<PhotoUpImageBucket> list, List<LocalMediaUpItem> mediaList, List<LocalMediaUpItem> imageList, List<LocalMediaUpItem> videoList);
    }
}
