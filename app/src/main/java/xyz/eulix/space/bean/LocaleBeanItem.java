package xyz.eulix.space.bean;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/13 10:29
 */
public class LocaleBeanItem extends LocaleBean implements EulixKeep {
    private boolean isSelect;

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    @Override
    public String toString() {
        return "LocaleBeanItem{" +
                "isSelect=" + isSelect +
                ", language='" + language + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}
