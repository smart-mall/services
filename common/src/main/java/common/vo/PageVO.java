package common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页负载，统一放在 {@code R.data} 里。
 *
 * <p>{@code total} 用 long：订单、日志这类表的行数会超过 int 上限。
 *
 * @param <T> 行数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVO<T> {

    /** 总行数 */
    private long total;

    /** 当前页的行数据 */
    private List<T> rows;
}
