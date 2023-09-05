package xyz.eulix.space.presenter;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.Locale;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.LocaleBean;
import xyz.eulix.space.util.DataUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/13 9:47
 */
public class EulixLanguageSettingsPresenter extends AbsPresenter<EulixLanguageSettingsPresenter.IEulixLanguageSettings> {
    public interface IEulixLanguageSettings extends IBaseView {}

    public LocaleBean getLocaleBean() {
        LocaleBean localeBean = null;
        String localeValue = DataUtil.getApplicationLocale(context);
        if (localeValue != null) {
            try {
                localeBean = new Gson().fromJson(localeValue, LocaleBean.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        return localeBean;
    }

    public void setLocaleBean(LocaleBean localeBean) {
        String localeValue = null;
        if (localeBean != null) {
            localeValue = new Gson().toJson(localeBean, LocaleBean.class);
        }
        DataUtil.setApplicationLocale(context, localeValue, true);
    }
}
