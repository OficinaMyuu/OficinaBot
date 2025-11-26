package ofc.bot.handlers.paginations;

import java.util.List;

public class PageItem<T>{
    private final List<T> entities;
    private final int pageIndex;
    private final int offset;
    private final int pageCount;
    private final int rowCount;

    protected PageItem(List<T> entities, int pageIndex, int offset, int pageCount, int rowCount) {
        this.entities = entities;
        this.pageIndex = pageIndex;
        this.offset = offset;
        this.pageCount = pageCount;
        this.rowCount = rowCount;
    }

    public List<T> getEntities() {
        return this.entities;
    }

    public T get(int index) {
        return this.entities.get(index);
    }

    public int getPageIndex() {
        return this.pageIndex;
    }

    public int getPage() {
        return this.pageIndex + 1;
    }

    public int getOffset() {
        return this.offset;
    }

    public int getPageCount() {
        return this.pageCount;
    }

    public int getRowCount() {
        return this.rowCount;
    }

    /**
     * Checks if there are more pages available.
     * <p>
     * Useful when creating
     * {@link net.dv8tion.jda.api.components.buttons.Button Button}s
     * for interactive pagination.
     *
     * @return {@code true} if there are more pages available,
     *         {@code false} otherwise (if this is the last page).
     */
    public boolean hasMore() {
        return this.pageIndex < lastPageIndex();
    }

    public boolean isEmpty() {
        return this.entities.isEmpty();
    }

    public int lastPageIndex() {
        return this.pageCount - 1;
    }

    public int getLastPage() {
        return this.pageCount;
    }

    public boolean exists(int pageIndex) {
        return pageIndex <= lastPageIndex();
    }
}