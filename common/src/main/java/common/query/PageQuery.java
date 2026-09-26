package common.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Getter;
import lombok.Setter;

/**
 * 列表接口的分页请求基类，业务筛选条件由各接口的子类加字段。
 *
 * <p>用 {@code @ModelAttribute} 从查询参数绑定，参数名固定为 {@code page} 与 {@code limit}。
 */
@Getter
@Setter
public class PageQuery {

    private static final long DEFAULT_PAGE = 1;

    private static final long DEFAULT_LIMIT = 10;

    /** 单页上限，防止 limit 传成天文数字把服务拖垮。 */
    private static final long MAX_LIMIT = 1000;

    /** 页码，从 1 开始；小于 1 时按 {@link #DEFAULT_PAGE} 处理。 */
    private long page = DEFAULT_PAGE;

    /** 每页条数；小于 1 时按 {@link #DEFAULT_LIMIT} 处理，超过 {@link #MAX_LIMIT} 时截断。 */
    private long limit = DEFAULT_LIMIT;

    /**
     * 转换为 MyBatis-Plus 的分页对象。
     *
     * <p>兜默认值与截上限只在这一处做，子类无需重复校验。
     *
     * @param <T> 行数据类型
     * @return 页码不小于 1、每页条数落在 [1, {@link #MAX_LIMIT}] 内的分页对象
     */
    public <T> IPage<T> toPage() {
        long safePage = page < 1 ? DEFAULT_PAGE : page;
        long safeLimit = limit < 1 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return new Page<>(safePage, safeLimit);
    }
}
