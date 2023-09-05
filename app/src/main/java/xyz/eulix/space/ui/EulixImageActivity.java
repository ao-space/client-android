package xyz.eulix.space.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.presenter.EulixImagePresenter;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ViewUtils;
import xyz.eulix.space.view.BigImageView;

public class EulixImageActivity extends AbsActivity<EulixImagePresenter.IEulixImage, EulixImagePresenter> implements EulixImagePresenter.IEulixImage, View.OnClickListener {
    private BigImageView eulixImage;
    private FrameLayout statusBarContainer;
    private TextView title;
    private ImageButton back;
    private String imageAssetName;
    private String titleText;
    private boolean showBack;
    private boolean showTitle;

    @Override
    public void initView() {
        setContentView(R.layout.activity_eulix_image);
        eulixImage = findViewById(R.id.eulix_image);
        statusBarContainer = findViewById(R.id.status_bar_container);
        title = findViewById(R.id.title);
        back = findViewById(R.id.back);
    }

    @Override
    public void initData() {
        Intent intent = getIntent();
        if (intent != null) {
            showTitle = intent.getBooleanExtra("showTitle", false);
            showBack = intent.getBooleanExtra("showBack", true);
            if (intent.hasExtra("title")) {
                titleText = intent.getStringExtra("title");
            }
            if (intent.hasExtra("imageAssetName")) {
                imageAssetName = intent.getStringExtra("imageAssetName");
            }
        }
    }

    @Override
    public void initViewData() {
        if (showTitle) {
            title.setText(StringUtil.nullToEmpty(titleText));
        }
        back.setVisibility(showBack ? View.VISIBLE : View.GONE);
        statusBarContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.getStatusBarHeight(this)));
        if (StringUtil.isNonBlankString(imageAssetName)) {
            AssetManager assetManager = getAssets();
            if (assetManager != null) {
                try (InputStream inputStream = assetManager.open(imageAssetName)) {
                    eulixImage.setImage(inputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        }
    }

    @Override
    public void initEvent() {
        if (showBack) {
            back.setOnClickListener(this);
        }
    }

    public static void startImage(@NonNull Context context, @Nullable String title, String imageAssetName, boolean showBack) {
        Intent intent = new Intent(context, EulixImageActivity.class);
        if (title != null) {
            intent.putExtra("title", title);
        }
        intent.putExtra("imageAssetName", imageAssetName);
        intent.putExtra("showTitle", (title != null));
        intent.putExtra("showBack", showBack);
        context.startActivity(intent);
    }

    @NotNull
    @Override
    public EulixImagePresenter createPresenter() {
        return new EulixImagePresenter();
    }

    @Override
    public boolean isImmersion() {
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    finish();
                    break;
                default:
                    break;
            }
        }
    }
}
