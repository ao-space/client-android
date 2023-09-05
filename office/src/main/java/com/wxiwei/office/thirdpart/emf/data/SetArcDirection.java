// Copyright 2002, FreeHEP.

package com.wxiwei.office.thirdpart.emf.data;

import java.io.IOException;

import com.wxiwei.office.thirdpart.emf.EMFConstants;
import com.wxiwei.office.thirdpart.emf.EMFInputStream;
import com.wxiwei.office.thirdpart.emf.EMFRenderer;
import com.wxiwei.office.thirdpart.emf.EMFTag;

/**
 * SetArcDirection TAG.
 *
 * @author Mark Donszelmann
 * @version $Id: SetArcDirection.java 10377 2007-01-23 15:44:34Z duns $
 */
public class SetArcDirection extends EMFTag implements EMFConstants
{

    private int direction;

    public SetArcDirection()
    {
        super(57, 1);
    }

    public SetArcDirection(int direction)
    {
        this();
        this.direction = direction;
    }

    public EMFTag read(int tagID, EMFInputStream emf, int len) throws IOException
    {

        return new SetArcDirection(emf.readDWORD());
    }

    public String toString()
    {
        return super.toString() + "\n  direction: " + direction;
    }

    /**
     * displays the tag using the renderer
     *
     * @param renderer EMFRenderer storing the drawing session data
     */
    public void render(EMFRenderer renderer)
    {
        // The SetArcDirection sets the drawing direction to
        // be used for arc and rectangle functions.
        renderer.setArcDirection(direction);
    }
}
