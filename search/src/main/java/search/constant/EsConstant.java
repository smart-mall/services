package search.constant;

public class EsConstant {
    public static final String PRODUCT_INDEX = "product";

    /**
     * 默认每页条数。前端不传 pageSize 时用这个值。
     *
     * 原来是写死的 2（调试遗留），而且前端不能传 —— 一页两个商品卡片。
     */
    public static final Integer DEFAULT_PAGE_SIZE = 20;

    /**
     * 每页条数上限。必须设，否则前端一个 pageSize=100000 就能把 ES 拉爆。
     */
    public static final Integer MAX_PAGE_SIZE = 100;

    /**
     * ES 的 index.max_result_window 默认值。from + size 超过它 ES 会直接报错，
     * 而那个错返回的是没有 code 的 500，所以提前按这个值拦一道。
     */
    public static final Integer MAX_RESULT_WINDOW = 10000;
}
