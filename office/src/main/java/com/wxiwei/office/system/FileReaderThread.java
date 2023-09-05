/*
 * 文件名称:          FileReaderThread.java
 *  
 * 编译器:            android2.2
 * 时间:              下午8:11:02
 */
package com.wxiwei.office.system;

import com.wxiwei.office.constant.MainConstant;
import com.wxiwei.office.fc.doc.DOCReader;
import com.wxiwei.office.fc.doc.DOCXReader;
import com.wxiwei.office.fc.doc.TXTReader;
import com.wxiwei.office.fc.pdf.PDFReader;
import com.wxiwei.office.fc.ppt.PPTReader;
import com.wxiwei.office.fc.ppt.PPTXReader;
import com.wxiwei.office.fc.xls.XLSReader;
import com.wxiwei.office.fc.xls.XLSXReader;

import android.os.Handler;
import android.os.Message;
/**
 * 读取文档线程
 * <p>
 * <p>
 * Read版本:        Read V1.0
 * <p>
 * 作者:            ljj8494
 * <p>
 * 日期:            2012-3-12
 * <p>
 * 负责人:          ljj8494
 * <p>
 * 负责小组:
 * <p>
 * <p>
 */
public class FileReaderThread extends Thread
{

    /**
     *
     */
    public FileReaderThread(IControl control, Handler handler, String filePath, String encoding)
    {
        this.control = control;
        this.handler = handler;
        this.filePath = filePath;
        this.encoding = encoding;
    }

    /**
     *
     *
     */
    public void run()
    {
        // show progress
        Message msg = new Message();
        msg.what = MainConstant.HANDLER_MESSAGE_SHOW_PROGRESS;
        handler.handleMessage(msg);

        msg = new Message();
        msg.what = MainConstant.HANDLER_MESSAGE_DISMISS_PROGRESS;
        try
        {
            IReader reader = null;
            String fileName = filePath.toLowerCase();
            // doc
            if (fileName.endsWith(MainConstant.FILE_TYPE_DOC)
                || fileName.endsWith(MainConstant.FILE_TYPE_DOT))
            {
                reader = new DOCReader(control, filePath);
            }
            // docx
            else if (fileName.endsWith(MainConstant.FILE_TYPE_DOCX)
                     || fileName.endsWith(MainConstant.FILE_TYPE_DOTX)
                     || fileName.endsWith(MainConstant.FILE_TYPE_DOTM))
            {
                reader = new DOCXReader(control, filePath);;
            }
            //
            else if (fileName.endsWith(MainConstant.FILE_TYPE_TXT))
            {
                reader = new TXTReader(control, filePath, encoding);
            }
            // xls
            else if (fileName.endsWith(MainConstant.FILE_TYPE_XLS)
                     || fileName.endsWith(MainConstant.FILE_TYPE_XLT))
            {
                reader = new XLSReader(control, filePath);
            }
            // xlsx
            else if (fileName.endsWith(MainConstant.FILE_TYPE_XLSX)
                     || fileName.endsWith(MainConstant.FILE_TYPE_XLTX)
                     || fileName.endsWith(MainConstant.FILE_TYPE_XLTM)
                     || fileName.endsWith(MainConstant.FILE_TYPE_XLSM))
            {
                reader = new XLSXReader(control, filePath);
            }
            // ppt
            else if (fileName.endsWith(MainConstant.FILE_TYPE_PPT)
                     || fileName.endsWith(MainConstant.FILE_TYPE_POT))
            {
                reader = new PPTReader(control, filePath);
            }
            // pptx
            else if (fileName.endsWith(MainConstant.FILE_TYPE_PPTX)
                     || fileName.endsWith(MainConstant.FILE_TYPE_PPTM)
                     || fileName.endsWith(MainConstant.FILE_TYPE_POTX)
                     || fileName.endsWith(MainConstant.FILE_TYPE_POTM))
            {
                reader = new PPTXReader(control, filePath);
            }
            // PDF document
            else if (fileName.endsWith(MainConstant.FILE_TYPE_PDF))
            {
                reader = new PDFReader(control, filePath);
            }
            // other
            else
            {
                reader = new TXTReader(control, filePath, encoding);
            }
            // 把IReader实例传出
            Message mesReader = new Message();
            mesReader.obj = reader;
            // Then we get the reader in MainControl
            mesReader.what = MainConstant.HANDLER_MESSAGE_SEND_READER_INSTANCE;
            handler.handleMessage(mesReader);
            msg.obj = reader.getModel();
//            reader.dispose();
            // success, remove the progressDialog
            msg.what = MainConstant.HANDLER_MESSAGE_SUCCESS;
        }
        catch (OutOfMemoryError eee)
        {
            msg.what = MainConstant.HANDLER_MESSAGE_ERROR;
            msg.obj = eee;
        }
        catch (Exception ee)
        {
            msg.what = MainConstant.HANDLER_MESSAGE_ERROR;
            msg.obj = ee;
        }
        catch (AbortReaderError ee)
        {
            msg.what = MainConstant.HANDLER_MESSAGE_ERROR;
            msg.obj = ee;
        }
        finally
        {
            handler.handleMessage(msg);
            control = null;
            handler= null;
            encoding = null;
            filePath = null;
        }
    }

    //
    private String encoding;
    //
    private String filePath;
    //
    private Handler handler;
    //
    private IControl control;
}
