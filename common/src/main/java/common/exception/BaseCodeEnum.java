package common.exception;

import lombok.Getter;

@Getter
public enum BaseCodeEnum {
    UNKNOWN_EXCEPTION(10000, "未知异常"),
    VALID_EXCEPTION(10001, "参数格式校验失败"),
    JSON_EXCEPTION(10002, "JSON格式化异常"),
    PRODUCT_UP_EXCEPTION(11000, "商品上架异常"),
    // 上架中的商品不允许删除：它的 sku 还在 ES 里，删了库搜索结果里就会留下
    // 点进去打不开的商品。等"下架"接口补上之后，先下架再删。
    // 和 ORDER_STATUS_INVALID(17003) 一样，这是"状态不允许"而不是"参数格式不对"，
    // 所以给独立码，不走 10001 那套 errors 结构。
    PRODUCT_UP_SHELVED_CANNOT_DELETE(11001, "商品已上架，请先下架再删除"),
    // 规格参数在上架时被快照进 ES，改完 ES 不会跟着变：详情页读库、搜索的筛选面板读 ES，两边会分叉
    PRODUCT_UP_SHELVED_CANNOT_UPDATE(11002, "商品已上架，请先下架再修改规格"),

    // 仓库/采购。状态流转失败要和"参数格式不对"分开，否则前端只能笼统提示"操作失败"，
    // 用户不知道是这张单已经被领取了、还是需求单已经并到别的单里了。
    PURCHASE_NOT_FOUND(11100, "采购单不存在"),
    PURCHASE_STATUS_INVALID(11101, "采购单当前状态不支持该操作"),
    PURCHASE_DETAIL_NOT_FOUND(11102, "采购需求单不存在"),
    PURCHASE_DETAIL_STATUS_INVALID(11103, "采购需求单当前状态不支持该操作"),
    WARE_NOT_FOUND(11104, "仓库不存在"),
    WARE_IN_USE(11105, "仓库还有库存或关联单据，不能删除"),
    PURCHASE_DETAIL_EMPTY(11107, "采购单下没有采购需求，不能领取"),
    PURCHASE_WARE_MISMATCH(11108, "采购单只能包含同一个仓库的采购需求"),
    PURCHASE_NOT_ASSIGNEE(11109, "这张采购单不是分配给当前登录用户的"),
    TO_MANY_REQUEST(10003, "请求流量过大，请稍后再试"),

    SMS_CODE_EXCEPTION(10004,"验证码获取频率太高，请稍后再试"),
    USER_EXIST_EXCEPTION(15001,"存在相同的用户"),
    NO_STOCK_EXCEPTION(21000,"商品库存不足"),
    USERNAME_PASSWORD_EXCEPTION(15003,"账号或密码错误"),

    // 购物车。两件事分开给码：排查时一个要看商品服务（商品被删了）、
    // 一个要看前端状态（拿着已经不在车里的 skuId 来改数量），合成一个码会把这两条路都堵死。
    CART_ITEM_NOT_FOUND(16000,"购物车中没有该商品"),
    CART_SKU_NOT_FOUND(16001,"商品不存在或已下架"),

    // 订单。归属校验失败和"订单真的不存在"合并成同一个码和文案：
    // 区分开等于告诉调用方"这个订单号存在，只是不属于你"，那是个用户枚举点。
    ORDER_NOT_FOUND(17000,"订单不存在"),
    ORDER_TOKEN_INVALID(17001,"订单令牌已失效，请刷新页面重试"),
    ORDER_PRICE_CHANGED(17002,"商品价格已变动，请重新确认"),
    ORDER_STATUS_INVALID(17003,"订单当前状态不支持该操作"),
    ADDRESS_NOT_FOUND(17004,"收货地址不存在"),

    // 秒杀。老代码里 kill() 无论哪种失败都 return null，前端只能显示"手气不好"，
    // 用户不知道是没开始、抢完了还是已经抢过了。这里按失败原因分开给码。
    // 只有第一个是"这次秒杀根本不成立"，后面四个都是"秒杀成立但你这次没抢到"。
    SECKILL_NOT_FOUND(18000,"秒杀活动不存在或已结束"),
    // 随机码对不上。随机码是上架时生成的 UUID，只有秒杀进行中才会下发给页面，
    // 所以拿到过期页面的随机码来抢会落到这里，文案引导刷新而不是说"没抢到"。
    SECKILL_TOKEN_INVALID(18001,"秒杀请求无效，请刷新页面后重试"),
    SECKILL_SOLD_OUT(18002,"手慢了，该商品已被抢完"),
    SECKILL_LIMIT_EXCEEDED(18003,"数量超出每人限购"),
    SECKILL_ALREADY_BOUGHT(18004,"您已参与过本次秒杀，把机会留给别人吧"),

    // 文件/媒体。改造前所有失败都走 R.error(e.getMessage())，把 MinIO 的 endpoint、
    // bucket 名和原始 S3 错误文本一起回给了调用方——既泄露内网信息，又让前端无法区分
    // "我把参数传错了"和"存储挂了"。这里按调用方能采取的动作分开给码：
    // 前四个是"改参数"，最后一个是"重试"。
    MEDIA_FILE_EMPTY(19000,"上传文件不能为空"),
    MEDIA_FILE_TYPE_NOT_SUPPORTED(19001,"只支持 jpg/png/gif/webp 格式的图片"),
    MEDIA_FILE_TOO_LARGE(19002,"图片大小超出限制"),
    MEDIA_URL_INVALID(19003,"文件地址不合法"),
    MEDIA_STORAGE_ERROR(19004,"文件存储失败，请稍后重试"),

    // 下面两个配合 HTTP 401 一起用。前端 request.ts 是看 HTTP 状态码 401 去清 token 的，
    // 不看 body 里的 code，所以状态码必须是真 401，body 里的 code 只是给人看日志用的。
    NOT_LOGIN_EXCEPTION(15004,"请先登录"),
    LOGIN_EXPIRED_EXCEPTION(15005,"登录已过期，请重新登录"),
    ;


    private final int code;
    private final String msg;
    BaseCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

}
