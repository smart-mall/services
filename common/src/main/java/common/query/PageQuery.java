package common.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Getter;
import lombok.Setter;

/**
 * 列表接口的分页请求。业务筛选条件由各接口的子类加字段。
 *
 * <p>用 {@code @ModelAttribute} 从查询参数绑定，参数名仍是 {@code page} / {@code limit}，
 * 和原来的 {@code Map<String, Object> params} 一致，管理端不用改。</p>
 */
@Getter
@Setter
public class PageQuery {

    private static final long DEFAULT_PAGE = 1;

    private static final long DEFAULT_LIMIT = 10;

    /** 单页上限。原来 admin 侧没有任何上限，limit 传个天文数字就能把服务拖垮 */
    private static final long MAX_LIMIT = 1000;

    private long page = DEFAULT_PAGE;

    private long limit = DEFAULT_LIMIT;

    /** 转成 MyBatis-Plus 的分页对象。兜默认值和截上限都只在这一处 */
    public <T> IPage<T> toPage() {
        long safePage = page < 1 ? DEFAULT_PAGE : page;
        long safeLimit = limit < 1 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return new Page<>(safePage, safeLimit);
    }
}
