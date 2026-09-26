package common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页负载，统一放在 {@code R.data} 里，替代原来的 {@code PageUtils}。
 *
 * <p>只保留调用方真正会用到的两个字段；{@code total} 用 long，原来 PageUtils 的 int 会溢出。</p>
 *
 * @param <T> 行数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVO<T> {

    private long total;

    private List<T> rows;
}
