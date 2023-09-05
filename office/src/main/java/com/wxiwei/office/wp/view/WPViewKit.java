/*
 * 文件名称:          WPViewKit.java
 *  
 * 编译器:            android2.2
 * 时间:              上午9:05:16
 */

package com.wxiwei.office.wp.view;

import com.wxiwei.office.constant.wp.WPModelConstant;
import com.wxiwei.office.constant.wp.WPViewConstant;
import com.wxiwei.office.java.awt.Rectangle;
import com.wxiwei.office.simpletext.view.IView;
import com.wxiwei.office.simpletext.view.ViewKit;
import com.wxiwei.office.wp.control.Word;


/**
 */
public class WPViewKit extends ViewKit
{
    //
    private static WPViewKit kit = new WPViewKit();

    /**
     * 
     * @return
     */
    public static WPViewKit instance()
    {
        return kit;
    }

    /**
     * 给定坐标，得到页面视图
     * @param root
     * @param x  x值是100%的值
     * @param y  y值是100%的值
     * @return
     */
    public PageView getPageView(IView root, int x, int y)
    {
        if (root == null)
        {
            return null;
        }
        IView view = root.getChildView();
        while (view != null)
        {
            if (y > view.getY() && y < view.getY() + view.getHeight() + WPViewConstant.PAGE_SPACE)
            {
                break;
            }
            view = view.getNextView();
        }
        // 没有就返回第一页
        if (view == null)
        {
            view = root.getChildView();
        }
        return view == null ?  null : (PageView)view;
    }

    /**
     * get the view of the specified Offset and viewType
     */
    public IView getView(Word word, long offset, int type, boolean isBack)
    {
        return word.getRoot(word.getCurrentRootType()).getView(offset, type, isBack);
    }
    
    /**
     * get the view of the specified X, Y and viewType
     */
    public IView getView(Word word, int x, int y, int type, boolean isBack)
    {
        return word.getRoot(word.getCurrentRootType()).getView(x, y, type, isBack);
    }
    
    /**
     * 得到指定视图到指定视图的类型的绝对坐标
     */
    public Rectangle getAbsoluteCoordinate(IView view, int type, Rectangle rect)
    {
        rect.setBounds(0, 0, 0, 0);        
        while (view != null && view.getType() != type)
        {
            rect.x += view.getX();
            rect.y += view.getY();
            view = view.getParentView();
        }
        return rect;
    }
    
    /**
     * 
     * @param offset
     * @return
     */
    public long getArea(long offset)
    {
        return offset & WPModelConstant.AREA_MASK;
    }

}
