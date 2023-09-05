package xyz.eulix.space.ui.mine.general;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.mine.general.EulixLanguageAdapter;
import xyz.eulix.space.bean.LocaleBean;
import xyz.eulix.space.bean.LocaleBeanItem;
import xyz.eulix.space.presenter.EulixLanguageSettingsPresenter;
import xyz.eulix.space.ui.EulixMainActivity;
import xyz.eulix.space.util.StatusBarUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/13 9:46
 */
public class EulixLanguageSettingsActivity extends AbsActivity<EulixLanguageSettingsPresenter.IEulixLanguageSettings, EulixLanguageSettingsPresenter> implements EulixLanguageSettingsPresenter.IEulixLanguageSettings
        , View.OnClickListener, EulixLanguageAdapter.OnItemClickListener {
    private ImageButton back;
    private TextView title;
    private Button functionText;
    private RecyclerView languageList;
    private List<LocaleBeanItem> mLocaleBeanItemList;
    private EulixLanguageAdapter mAdapter;

    @Override
    public void initView() {
        setContentView(R.layout.activity_eulix_language_settings);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        functionText = findViewById(R.id.function_text);
        languageList = findViewById(R.id.language_list);
    }

    @Override
    public void initData() {
        mLocaleBeanItemList = new ArrayList<>();
    }

    @Override
    public void initViewData() {
        title.setText(R.string.language_settings);
        functionText.setVisibility(View.VISIBLE);
        functionText.setText(R.string.done);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        functionText.setOnClickListener(this);
        generateDataList();
        setFunctionTextPattern(false);
        mAdapter = new EulixLanguageAdapter(this, mLocaleBeanItemList);
        mAdapter.setOnItemClickListener(this);
        languageList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        languageList.addItemDecoration(new EulixLanguageAdapter.ItemDecoration(RecyclerView.VERTICAL
                , Math.round(getResources().getDimension(R.dimen.dp_1)), getResources().getColor(R.color.white_fff7f7f9)));
        languageList.setAdapter(mAdapter);
    }

    private void generateDataList() {
        LocaleBean currentLocaleBean = null;
        if (presenter != null) {
            currentLocaleBean = presenter.getLocaleBean();
        }
        if (mLocaleBeanItemList == null) {
            mLocaleBeanItemList = new ArrayList<>();
        } else {
            mLocaleBeanItemList.clear();
        }
        LocaleBeanItem localeBeanItemAuto = new LocaleBeanItem();
        localeBeanItemAuto.setSelect(currentLocaleBean == null);
        mLocaleBeanItemList.add(localeBeanItemAuto);
        LocaleBeanItem localeBeanItemSimplifiedChinese = new LocaleBeanItem();
        localeBeanItemSimplifiedChinese.setLanguage("zh");
        localeBeanItemSimplifiedChinese.setCountry("CN");
        localeBeanItemSimplifiedChinese.setSelect(LocaleBean.compare(currentLocaleBean, localeBeanItemSimplifiedChinese));
        mLocaleBeanItemList.add(localeBeanItemSimplifiedChinese);
        LocaleBeanItem localeBeanItemEnglish = new LocaleBeanItem();
        localeBeanItemEnglish.setLanguage("en");
        localeBeanItemEnglish.setSelect(LocaleBean.compare(currentLocaleBean, localeBeanItemEnglish));
        mLocaleBeanItemList.add(localeBeanItemEnglish);
    }

    private void setFunctionTextPattern(boolean isClickable) {
        if (functionText != null) {
            functionText.setClickable(isClickable);
            functionText.setTextColor(getResources().getColor(isClickable ? R.color.blue_ff337aff : R.color.gray_ffbcbfcd));
        }
    }

    private void handleChangeLanguageEvent() {
        LocaleBeanItem selectLocaleBeanItem = null;
        if (mLocaleBeanItemList != null) {
            for (LocaleBeanItem localeBeanItem : mLocaleBeanItemList) {
                if (localeBeanItem != null && localeBeanItem.isSelect()) {
                    selectLocaleBeanItem = localeBeanItem;
                    break;
                }
            }
        }
        if (selectLocaleBeanItem != null) {
            LocaleBean localeBean = null;
            String selectLanguage = selectLocaleBeanItem.getLanguage();
            if (selectLanguage != null) {
                localeBean = new LocaleBean();
                localeBean.setLanguage(selectLanguage);
                localeBean.setCountry(selectLocaleBeanItem.getCountry());
            }
            if (presenter != null) {
                presenter.setLocaleBean(localeBean);
            }
            Locale locale = selectLocaleBeanItem.parseLocale();
            if (locale == null && localeBean == null) {
                locale = Locale.getDefault();
            }
            Resources resources = getResources();
            if (resources != null && locale != null) {
                Configuration configuration = resources.getConfiguration();
                if (configuration != null) {
                    configuration.setLocale(locale);
                    //createConfigurationContext(configuration);
                    resources.updateConfiguration(configuration, resources.getDisplayMetrics());
                    // 更改application语言会导致的default语言出问题
                    //EulixSpaceApplication.updateLocale();
                    popAllAndGoMain();
                }
            }
        }
    }

    private void popAllAndGoMain() {
        EulixSpaceApplication.popAllOldActivity(this);
        Intent intent = new Intent(EulixLanguageSettingsActivity.this, EulixMainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @NotNull
    @Override
    public EulixLanguageSettingsPresenter createPresenter() {
        return new EulixLanguageSettingsPresenter();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    finish();
                    break;
                case R.id.function_text:
                    handleChangeLanguageEvent();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        boolean isUpdate = false;
        LocaleBeanItem selectLocaleBeanItem = null;
        if (mLocaleBeanItemList != null && position >= 0) {
            int size = mLocaleBeanItemList.size();
            if (size > position) {
                for (int i = 0; i < size; i++) {
                    LocaleBeanItem localeBeanItem = mLocaleBeanItemList.get(i);
                    if (localeBeanItem != null) {
                        boolean isSelect = (i == position);
                        if (!isUpdate) {
                            isUpdate = (isSelect != localeBeanItem.isSelect());
                        }
                        localeBeanItem.setSelect(isSelect);
                        if (isSelect) {
                            selectLocaleBeanItem = localeBeanItem;
                        }
                    }
                }
            }
        }
        if (isUpdate) {
            if (mAdapter != null) {
                mAdapter.updateData(mLocaleBeanItemList);
            }
            LocaleBean localeBean = null;
            LocaleBeanItem currentLocaleBeanItem = new LocaleBeanItem();
            if (presenter != null) {
                localeBean = presenter.getLocaleBean();
            }
            if (localeBean != null) {
                currentLocaleBeanItem.setLanguage(localeBean.getLanguage());
                currentLocaleBeanItem.setCountry(localeBean.getCountry());
            }
            if (selectLocaleBeanItem != null) {
                setFunctionTextPattern(!LocaleBean.compare(selectLocaleBeanItem, currentLocaleBeanItem));
            }
        }
    }
}
