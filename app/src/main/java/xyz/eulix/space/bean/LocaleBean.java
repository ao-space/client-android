package xyz.eulix.space.bean;

import android.text.TextUtils;

import java.util.Locale;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/12 18:05
 */
public class LocaleBean implements EulixKeep {
    protected String language;
    protected String country;

    public static boolean compare(LocaleBean localeBean1, LocaleBean localeBean2) {
        boolean isEqual = false;
        if (localeBean1 == null && localeBean2 == null) {
            isEqual = true;
        } else if (localeBean1 != null && localeBean2 != null) {
            boolean isCountryEqual = StringUtil.compare(localeBean1.country, localeBean2.country);
            isEqual = StringUtil.compare(localeBean1.language, localeBean2.language)
                    && (isCountryEqual || !(localeBean1.country != null && localeBean2.country != null));
        }
        return isEqual;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String formatLocale() {
        String name = "";
        if (language != null) {
            switch (language.toLowerCase()) {
                case "zh":
                    if (country != null) {
                        switch (country.toUpperCase()) {
                            case "CN":
                                name = "简体中文";
                                break;
                            case "TW":
                                name = "繁體中文";
                                break;
                            default:
                                break;
                        }
                    }
                    if (TextUtils.isEmpty(name)) {
                        name = "中文";
                    }
                    break;
                case "en":
                    if (country != null) {
                        switch (country.toUpperCase()) {
                            case "GB":
                            case "US":
                            case "CA":
                                name = "English";
                                break;
                            default:
                                break;
                        }
                    }
                    if (TextUtils.isEmpty(name)) {
                        name = "English";
                    }
                    break;
                default:
                    break;
            }
        }
        return name;
    }

    public Locale parseLocale() {
        Locale locale = null;
        if (language != null) {
            switch (language.toLowerCase()) {
                case "zh":
                    if (country != null) {
                        switch (country.toUpperCase()) {
                            case "CN":
                                locale = Locale.SIMPLIFIED_CHINESE;
                                break;
                            case "TW":
                                locale = Locale.TRADITIONAL_CHINESE;
                                break;
                            default:
                                break;
                        }
                    }
                    if (locale == null) {
                        locale = Locale.CHINESE;
                    }
                    break;
                case "en":
                    if (country != null) {
                        switch (country.toUpperCase()) {
                            case "GB":
                                locale = Locale.UK;
                                break;
                            case "US":
                                locale = Locale.US;
                                break;
                            case "CA":
                                locale = Locale.CANADA;
                                break;
                            default:
                                break;
                        }
                    }
                    if (locale == null) {
                        locale = Locale.ENGLISH;
                    }
                    break;
                default:
                    break;
            }
        }
        return locale;
    }

    @Override
    public String toString() {
        return "LocaleBean{" +
                "language='" + language + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}
