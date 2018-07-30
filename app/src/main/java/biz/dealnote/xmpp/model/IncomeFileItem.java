package biz.dealnote.xmpp.model;

import java.io.File;

public class IncomeFileItem {

    public File file;
    public int icon;

    public IncomeFileItem(File file, int icon) {
        this.file = file;
        this.icon = icon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IncomeFileItem that = (IncomeFileItem) o;

        return file.equals(that.file);
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }
}
