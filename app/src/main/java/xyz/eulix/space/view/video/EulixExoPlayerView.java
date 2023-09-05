package xyz.eulix.space.view.video;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ui.StyledPlayerView;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2023/1/16
 */
public class EulixExoPlayerView extends StyledPlayerView {

    public EulixExoPlayerView(Context context) {
        super(context);
    }

    public EulixExoPlayerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public EulixExoPlayerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

}
