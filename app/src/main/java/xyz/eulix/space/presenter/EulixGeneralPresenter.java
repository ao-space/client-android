package xyz.eulix.space.presenter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.LocaleBean;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.manager.ThumbManager;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.db.TransferDBManager;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/12 16:54
 */
public class EulixGeneralPresenter extends AbsPresenter<EulixGeneralPresenter.IEulixGeneral> {
    public long cacheFileSize = 0L;

    public interface IEulixGeneral extends IBaseView {
        void onCacheSizeRefresh(long totalSize);
    }

    public LocaleBean getLocaleBean() {
        LocaleBean localeBean = null;
        String localeValue = DataUtil.getApplicationLocale(context);
        if (localeValue != null) {
            try {
                localeBean = new Gson().fromJson(localeValue, LocaleBean.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        return localeBean;
    }

    //计算当前缓存
    public void calculateCacheSize() {
        ThreadPool.getInstance().execute(() -> {
            long totalSize = 0L;
            File dir = context.getExternalCacheDir();
            if (dir != null) {
                String cachePath = dir.getAbsolutePath();
                File cacheDirFile = new File(cachePath);
                if (cacheDirFile.exists()) {
                    File[] cacheDirList = cacheDirFile.listFiles();
                    if (cacheDirList != null) {
                        for (File dirItem : cacheDirList) {
                            long dirItemSize = FileUtil.getFolderSize(dirItem);
                            totalSize += dirItemSize;
                        }
                    }
                }
            }
            this.cacheFileSize = totalSize;

            if (iView != null) {
                iView.onCacheSizeRefresh(totalSize);
            }
        });
    }

    //清理缓存
    public void clearCache(Context context, ResultCallback callback) {
        ThreadPool.getInstance().execute(() -> {
            //清空文件夹
            String cachePath = context.getExternalCacheDir().getAbsolutePath();
            File cacheDirFile = new File(cachePath);
            if (cacheDirFile.exists()) {
                FileUtil.clearFolder(cacheDirFile);
            }
            this.cacheFileSize = 0L;
            //删除下载数据库内容
            TransferDBManager.getInstance(context).deleteByType(TransferHelper.TYPE_CACHE);
            ThumbManager.getInstance().resetLocalThumbPathMap();
            ThumbManager.getInstance().resetLocalCompressPathMap();

            new Handler(Looper.getMainLooper()).post(() -> {
                callback.onResult(true, null);
            });
        });
    }
}
