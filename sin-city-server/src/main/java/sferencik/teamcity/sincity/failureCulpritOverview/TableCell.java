package sferencik.teamcity.sincity.failureCulpritOverview;

import org.jetbrains.annotations.Nullable;

public class TableCell<T> {
    private final int colspan;
    @Nullable
    private final T content;

    public TableCell(@Nullable T content, int colspan) {
        this.colspan = colspan;
        this.content = content;
    }

    public int getColspan() {
        return colspan;
    }

    @Nullable
    public T getContent() {
        return content;
    }
}
