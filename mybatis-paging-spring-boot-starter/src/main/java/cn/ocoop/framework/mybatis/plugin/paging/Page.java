package cn.ocoop.framework.mybatis.plugin.paging;

import lombok.Data;
import org.apache.ibatis.session.RowBounds;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Created by liolay on 2017/11/10.
 */
@Data
public class Page<T> extends RowBounds implements Serializable, Iterable<T> {
    private long totalRow;
    private long pageSize = 10;
    private long totalPage;
    private long currentPage = 1;
    private Collection<T> data;

    public Page() {
    }

    public Page setTotalRow(long totalRow) {
        this.totalRow = totalRow;
        this.setTotalPage(this.totalRow / getPageSize() + (int) Math.ceil(this.totalRow % getPageSize()));
        if (getCurrentPage() > getTotalPage()) {
            setCurrentPage(getTotalPage());
        } else if (getCurrentPage() < 1) {
            setCurrentPage(1);
        }
        return this;
    }

    public long getStart() {
        return (getCurrentPage() - 1) * getPageSize();
    }

    public long getEnd() {
        return getStart() + getPageSize();
    }

    public Page setData(Collection<T> data) {
        this.data = data;
        return this;
    }


    @Override
    public Iterator<T> iterator() {
        return data.iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        data.forEach(action);
    }

    @Override
    public Spliterator<T> spliterator() {
        return data.spliterator();
    }

}


