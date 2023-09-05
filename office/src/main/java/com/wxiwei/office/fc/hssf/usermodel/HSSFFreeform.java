/*
 * 文件名称:           HSSFFreeform.java
 *  
 * 编译器:             android2.2
 * 时间:               下午2:18:18
 */
package com.wxiwei.office.fc.hssf.usermodel;

import com.wxiwei.office.common.autoshape.pathbuilder.ArrowPathAndTail;
import com.wxiwei.office.fc.ShapeKit;
import com.wxiwei.office.fc.ddf.EscherContainerRecord;
import com.wxiwei.office.java.awt.Rectangle;
import com.wxiwei.office.ss.model.XLSModel.AWorkbook;

import android.graphics.Path;
import android.graphics.PointF;

/**
 * TODO: 文件注释
 * <p>
 * <p>
 * Read版本:       Read V1.0
 * <p>
 * 作者:           jhy1790
 * <p>
 * 日期:           2013-4-2
 * <p>
 * 负责人:         jhy1790
 * <p>
 * 负责小组:         
 * <p>
 * <p>
 */
public class HSSFFreeform extends HSSFAutoShape
{   
    public HSSFFreeform(AWorkbook workbook, EscherContainerRecord escherContainer,
        HSSFShape parent, HSSFAnchor anchor, int shapeType)
    {
        super(workbook, escherContainer, parent, anchor, shapeType);
        processLineWidth();
        processArrow(escherContainer);
    }

    /**
     * Gets the freeform path
     *
     * @return the freeform path
     */
    public Path[] getFreeformPath(Rectangle rect, PointF startArrowTailCenter, byte startArrowType,  PointF endArrowTailCenter, byte endArrowType)
    {
        return ShapeKit.getFreeformPath(escherContainer, rect, startArrowTailCenter, startArrowType, endArrowTailCenter, endArrowType);
    }
    
    public ArrowPathAndTail getStartArrowPath(Rectangle rect)
    {
        return ShapeKit.getStartArrowPathAndTail(escherContainer, rect);
    }
    
    public ArrowPathAndTail getEndArrowPath(Rectangle rect)
    {
        return ShapeKit.getEndArrowPathAndTail(escherContainer, rect);
    }    
}
