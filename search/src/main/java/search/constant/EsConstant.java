package search.constant;

/** search 模块的常量：ES 索引名与分页边界。 */
public class EsConstant {
    public static final String PRODUCT_INDEX = "product";

    /**
     * 默认每页条数：前端不传 {@code pageSize} 时用它。
     */
    public static final Integer DEFAULT_PAGE_SIZE = 20;

    /**
     * 每页条数上限。必须限制，否则前端传一个极大的 {@code pageSize} 会把 ES 拉爆。
     */
    public static final Integer MAX_PAGE_SIZE = 100;

    /**
     * ES 的 {@code index.max_result_window} 默认值。from + size 超过它 ES 会直接报错，
     * 而那个错返回的是没有 code 的 500，所以提前按这个值拦一道。
     */
    public static final Integer MAX_RESULT_WINDOW = 10000;
}
