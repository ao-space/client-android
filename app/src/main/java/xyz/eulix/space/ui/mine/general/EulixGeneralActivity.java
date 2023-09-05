package xyz.eulix.space.ui.mine.general;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.LocaleBean;
import xyz.eulix.space.presenter.EulixGeneralPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ToastUtil;
import xyz.eulix.space.view.dialog.EulixDialogUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/12 16:56
 */
public class EulixGeneralActivity extends AbsActivity<EulixGeneralPresenter.IEulixGeneral, EulixGeneralPresenter> implements EulixGeneralPresenter.IEulixGeneral, View.OnClickListener {
    private ImageButton back;
    private TextView title;
    private LinearLayout multiLanguageContainer;
    private TextView languageContent;
    private LinearLayout cacheManageContainer;
    private TextView cacheManageContent;
    private EulixGeneralHandler mHandler;

    static class EulixGeneralHandler extends Handler {
        private WeakReference<EulixGeneralActivity> eulixGeneralActivityWeakReference;

        public EulixGeneralHandler(EulixGeneralActivity activity) {
            eulixGeneralActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            EulixGeneralActivity activity = eulixGeneralActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_eulix_general);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        multiLanguageContainer = findViewById(R.id.multi_language_container);
        languageContent = findViewById(R.id.language_content);
        cacheManageContainer = findViewById(R.id.cache_manage_container);
        cacheManageContent = findViewById(R.id.cache_manage_content);
    }

    @Override
    public void initData() {
        mHandler = new EulixGeneralHandler(this);
    }

    @Override
    public void initViewData() {
        title.setText(R.string.general);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        multiLanguageContainer.setOnClickListener(this);
        cacheManageContainer.setOnClickListener(this);
    }


    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocaleBean localeBean = null;
        if (presenter != null) {
            localeBean = presenter.getLocaleBean();
            presenter.calculateCacheSize();
        }
        if (languageContent != null) {
            if (localeBean == null) {
                languageContent.setText(R.string.follow_up_system);
            } else {
                languageContent.setText(StringUtil.nullToEmpty(localeBean.formatLocale()));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @NotNull
    @Override
    public EulixGeneralPresenter createPresenter() {
        return new EulixGeneralPresenter();
    }

    @Override
    public void onCacheSizeRefresh(long totalSize) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (cacheManageContent != null) {
                    cacheManageContent.setText(FormatUtil.formatSimpleSize(totalSize, ConstantField.SizeUnit.FORMAT_1F));
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    finish();
                    break;
                case R.id.multi_language_container:
                    Intent languageSettingsIntent = new Intent(EulixGeneralActivity.this, EulixLanguageSettingsActivity.class);
                    startActivity(languageSettingsIntent);
                    break;
                case R.id.cache_manage_container:
                    clearCache();
                    break;
                default:
                    break;
            }
        }
    }

    private void clearCache() {
        if (presenter.cacheFileSize > 0) {
            String cacheSizeStr = FormatUtil.formatSize(presenter.cacheFileSize, ConstantField.SizeUnit.FORMAT_1F);
            String showStr = getResources().getString(R.string.clear_cache_data_notice).replace("%$", cacheSizeStr);
            EulixDialogUtil.showBottomDeleteDialog(this, showStr, getResources().getString(R.string.clear),
                    (dialog, which) -> {
                        presenter.clearCache(this, (result, extraMsg) -> {
                            if (result) {
                                ToastUtil.showToast(this.getResources().getString(R.string.cache_clear));
                                cacheManageContent.setText(FormatUtil.formatSimpleSize(0, ConstantField.SizeUnit.FORMAT_1F));
                            }
                        });
                    }, null);
        } else {
            ToastUtil.showToast(this.getResources().getString(R.string.no_cache_data));
        }

    }
}
