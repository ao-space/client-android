package com.wxiwei.office;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.wxiwei.office.common.IOfficeToPicture;
import com.wxiwei.office.constant.EventConstant;
import com.wxiwei.office.constant.wp.WPViewConstant;
import com.wxiwei.office.pg.control.Presentation;
import com.wxiwei.office.res.ResKit;
import com.wxiwei.office.ss.control.ExcelView;
import com.wxiwei.office.system.IMainFrame;
import com.wxiwei.office.system.MainControl;
import com.wxiwei.office.system.beans.pagelist.APageListView;
import com.wxiwei.office.system.beans.pagelist.IPageListViewListener;
import com.wxiwei.office.wp.control.Word;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by zjt on 18-8-11.
 *
 * Edit by zfy on 2021-12-21
 * add getOfficeView method,support excel view
 */

public abstract class IOffice implements IMainFrame {
    private MainControl control;
    private boolean writeLog = true;
    //view background
    private Object bg = Color.DKGRAY;
    private String tempFilePath;

    public IOffice() {
        initControl();
    }

    private void initControl() {
        control = new MainControl(this);
        control.setOffictToPicture(new IOfficeToPicture() {
            public Bitmap getBitmap(int componentWidth, int componentHeight) {
                if (componentWidth == 0 || componentHeight == 0) {
                    return null;
                }
                if (bitmap == null
                        || bitmap.getWidth() != componentWidth
                        || bitmap.getHeight() != componentHeight) {
                    // custom picture size
                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                    bitmap = Bitmap.createBitmap((int) (componentWidth), (int) (componentHeight), Bitmap.Config.ARGB_8888);
                }
                return bitmap;
            }

            public void callBack(Bitmap bitmap) {
                saveBitmapToFile(bitmap);
            }

            private Bitmap bitmap;

            @Override
            public void setModeType(byte modeType) {
                // TODO Auto-generated method stub

            }

            @Override
            public byte getModeType() {
                // TODO Auto-generated method stub
                return VIEW_CHANGE_END;
            }

            @Override
            public boolean isZoom() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void dispose() {
                // TODO Auto-generated method stub

            }
        });
    }

    private void saveBitmapToFile(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        if (tempFilePath == null) {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                tempFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            }
            File file = new File(tempFilePath + File.separatorChar + "tempPic");
            if (!file.exists()) {
                file.mkdir();
            }
            tempFilePath = file.getAbsolutePath();
        }

        File file = new File(tempFilePath + File.separatorChar + "export_image.jpg");
        try {
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
        } finally {
            //bitmap.recycle();
        }
    }

    public APageListView getView() {
        Object obj = control.getView();
        if (obj instanceof Presentation) {
            return ((Presentation) obj).getView().getView();
        } else if (obj instanceof Word) {
            return ((Word) obj).getPrintWord().getListView();
        } else {
            return null;
        }
    }

    public View getOfficeView() {
        View obj = control.getView();
        if (obj instanceof ExcelView) {
            return obj;
        } else if (obj instanceof Presentation) {
            return ((Presentation) obj).getView().getView();
        } else if (obj instanceof Word) {
            return ((Word) obj).getPrintWord().getListView();
        } else {
            return null;
        }
    }


    public void openFile(String filepath) {
        getControl().openFile(filepath);
    }

    /**
     * true: show message when zooming
     * false: not show message when zooming
     *
     * @return
     */
    public boolean isShowZoomingMsg() {
        return true;
    }

    /**
     * true: pop up dialog when throw err
     * false: not pop up dialog when throw err
     *
     * @return
     */
    public boolean isPopUpErrorDlg() {
        return true;
    }

    @Override
    public abstract Activity getActivity();

    /**
     * do action，this is method don't call <code>control.actionEvent</code> method, Easily lead to infinite loop
     *
     * @param actionID action ID
     * @param obj      acValue
     * @return True if the listener has consumed the event, false otherwise.
     */
    public boolean doActionEvent(int actionID, Object obj) {
        try {
            switch (actionID) {
                case EventConstant.SYS_UPDATE_TOOLSBAR_BUTTON_STATUS: //update toolsbar state
                    updateToolsbarStatus();
                    break;
                case EventConstant.APP_FINDING:
                    String content = ((String) obj).trim();
                    if (content.length() > 0 && control.getFind().find(content)) {
                        setFindBackForwardState(true);
                    }
                    break;
                case EventConstant.SS_CHANGE_SHEET:
                    break;
                default:
                    return false;
            }
        } catch (Exception e) {
            control.getSysKit().getErrorKit().writerLog(e);
        }
        return true;
    }

    @Override
    public void updateToolsbarStatus() {

    }

    @Override
    public void setFindBackForwardState(boolean state) {

    }

    /**
     *
     */
    public int getBottomBarHeight() {
        return 0;
    }

    /**
     *
     */
    public int getTopBarHeight() {
        return 0;
    }

    public boolean onEventMethod(View v, MotionEvent e1, MotionEvent e2, float xValue,
                                 float yValue, byte eventMethodType) {
        return false;
    }


    public void changePage() {
    }

    public MainControl getControl() {
        return this.control;
    }

    /**
     *
     */
    public abstract String getAppName();

    /**
     * 是否绘制页码
     */
    public boolean isDrawPageNumber() {
        return true;
    }

    /**
     * 是否支持zoom in / zoom out
     */
    public boolean isTouchZoom() {
        return true;
    }

    /**
     * Word application 默认视图(Normal or Page)
     *
     * @return WPViewConstant.PAGE_ROOT or WPViewConstant.NORMAL_ROOT
     */
    public byte getWordDefaultView() {
        return WPViewConstant.PRINT_ROOT;
    }

    /**
     * normal view, changed after zoom bend, you need to re-layout
     *
     * @return true   re-layout
     * false  don't re-layout
     */
    public boolean isZoomAfterLayoutForWord() {
        return true;
    }

    /**
     *
     */

    public void changeZoom() {
    }

    /**
     *
     */
    public void error(int errorCode) {
    }

    @Override
    public void showProgressBar(boolean visible) {

    }

    @Override
    public void updateViewImages(List<Integer> viewList) {

    }

    /**
     * when need destroy the office engine instance callback this method
     */
    public void destroyEngine() {
    }

    /**
     * get Internationalization resource
     *
     * @param resName Internationalization resource name
     */
    public String getLocalString(String resName) {
        return ResKit.instance().getLocalString(resName);
    }

    @Override
    public boolean isShowPasswordDlg() {
        return true;
    }

    @Override
    public boolean isShowProgressBar() {
        return true;
    }

    @Override
    public boolean isShowFindDlg() {
        return true;
    }

    @Override
    public boolean isShowTXTEncodeDlg() {
        return true;
    }

    /**
     */
    public String getTXTDefaultEncode() {
        return "GBK";
    }


    @Override
    public void completeLayout() {
    }

    @Override
    public boolean isChangePage() {
        // TODO Auto-generated method stub
        return true;
    }

    /**
     * @param saveLog
     */
    public void setWriteLog(boolean saveLog) {
        this.writeLog = saveLog;
    }

    /**
     * @return
     */
    public boolean isWriteLog() {
        return writeLog;
    }

    @Override
    public void setThumbnail(boolean isThumbnail) {

    }

    @Override
    public boolean isThumbnail() {
        return false;
    }

    /**
     * get view backgrouond
     *
     * @return
     */
    public Object getViewBackground() {
        return bg;
    }

    /**
     * set flag whether fitzoom can be larger than 100% but smaller than the max zoom
     *
     * @param ignoreOriginalSize
     */
    public void setIgnoreOriginalSize(boolean ignoreOriginalSize) {

    }

    /**
     * @return true fitzoom may be larger than 100% but smaller than the max zoom
     * false fitzoom can not larger than 100%
     */
    public boolean isIgnoreOriginalSize() {
        return false;
    }

    public byte getPageListViewMovingPosition() {
        return IPageListViewListener.Moving_Horizontal;
    }

    /**
     * @return
     */
    public abstract File getTemporaryDirectory();

    /**
     * 释放内存
     */
    public void dispose() {
        if (control != null) {
            control.dispose();
            control = null;
        }
    }
}