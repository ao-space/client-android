/*
 * 文件名称:          PGEditor.java
 *  
 * 编译器:            android2.2
 * 时间:              上午11:42:04
 */
package com.wxiwei.office.pg.control;

import java.util.Map;

import com.wxiwei.office.common.shape.AbstractShape;
import com.wxiwei.office.common.shape.IShape;
import com.wxiwei.office.common.shape.TextBox;
import com.wxiwei.office.constant.MainConstant;
import com.wxiwei.office.java.awt.Rectangle;
import com.wxiwei.office.pg.animate.IAnimation;
import com.wxiwei.office.pg.animate.ShapeAnimation;
import com.wxiwei.office.simpletext.control.Highlight;
import com.wxiwei.office.simpletext.control.IHighlight;
import com.wxiwei.office.simpletext.control.IWord;
import com.wxiwei.office.simpletext.model.IDocument;
import com.wxiwei.office.simpletext.model.SectionElement;
import com.wxiwei.office.simpletext.view.STRoot;
import com.wxiwei.office.system.IControl;

/**
 * <p>
 * <p>
 * Read版本:        Read V1.0
 * <p>
 * 作者:            ljj8494
 * <p>o
 * 日期:            2012-7-27
 * <p>
 * 负责人:          ljj8494
 * <p>
 * 负责小组:
 * <p>
 * <p>
 */
public class PGEditor implements IWord {

    /**
     *
     */
    public PGEditor(Presentation pgView) {
        this.pgView = pgView;
        highlight = new Highlight(this);
    }

    /**
     *
     *
     */
    public IHighlight getHighlight() {
        return highlight;
    }

    /**
     *
     */
    public Rectangle modelToView(long offset, Rectangle rect, boolean isBack) {
        if (editorTextBox != null) {
            STRoot root = editorTextBox.getRootView();
            if (root != null) {
                root.modelToView(offset, rect, isBack);
            }
            rect.x += editorTextBox.getBounds().x;
            rect.y += editorTextBox.getBounds().y;
        }
        return rect;
    }

    /**
     *
     *
     */
    public IDocument getDocument() {
        return null;
    }

    /**
     *
     *
     */
    public String getText(long start, long end) {
        if (editorTextBox != null) {
            SectionElement elem = editorTextBox.getElement();
            if (elem.getEndOffset() - elem.getStartOffset() > 0) {
                String str = elem.getText(null);
                if (str != null) {
                    return str.substring((int) Math.max(start, elem.getStartOffset()),
                            (int) Math.min(end, elem.getEndOffset()));
                }
            }
        }
        return null;
    }

    /**
     *
     *
     */
    public long viewToModel(int x, int y, boolean isBack) {
        if (pgView == null) {
            return -1;
        }
        IShape shape = pgView.getCurrentSlide().getShape(x, y);
        if (shape != null) {
            if (shape.getType() == AbstractShape.SHAPE_TEXTBOX) // 文本框
            {
                STRoot root = ((TextBox) shape).getRootView();
                if (root != null) {
                    x -= shape.getBounds().x;
                    y -= shape.getBounds().y;
                    return root.viewToModel(x, y, isBack);
                }
            }
        }
        return -1;
    }

    /**
     * @return Returns the editorBox.
     */
    public TextBox getEditorTextBox() {
        return editorTextBox;
    }

    /**
     * @param editorBox The editorBox to set.
     */
    public void setEditorTextBox(TextBox editorBox) {
        this.editorTextBox = editorBox;
    }

    /**
     *
     */
    public byte getEditType() {
        return MainConstant.APPLICATION_TYPE_PPT;
    }

    public void setShapeAnimation(Map<Integer, IAnimation> paraAnimation) {
        this.paraAnimation = paraAnimation;
    }


    /**
     * @param para
     * @return
     */
    public IAnimation getParagraphAnimation(int paragraphID) {
        if (pgView != null /*&& pgView.isSlideShow()*/ && paraAnimation != null) {
            IAnimation animation = paraAnimation.get(paragraphID);
            if (animation == null) {
                animation = paraAnimation.get(ShapeAnimation.Para_All);
            }
            if (animation == null) {
                animation = paraAnimation.get(ShapeAnimation.Para_BG);
            }
            return animation;

        }

        return null;
    }

    /**
     *
     */
    public IShape getTextBox() {
        return this.editorTextBox;
    }

    public void clearAnimation() {
        if (paraAnimation != null) {
            paraAnimation.clear();
        }
    }

    /**
     *
     */
    public IControl getControl() {
        if (pgView != null) {
            return pgView.getControl();
        }
        return null;
    }

    /**
     * @return
     */
    public Presentation getPGView() {
        return pgView;
    }

    /**
     *
     */
    public void dispose() {
        editorTextBox = null;
        if (highlight != null) {
            highlight.dispose();
            highlight = null;
        }
        pgView = null;
        if (paraAnimation != null) {
            paraAnimation.clear();
            paraAnimation = null;
        }

    }


    //
    private TextBox editorTextBox;
    //
    private IHighlight highlight;
    //
    private Presentation pgView;

    private Map<Integer, IAnimation> paraAnimation;
}
