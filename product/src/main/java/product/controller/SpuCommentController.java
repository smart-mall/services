package product.controller;

import common.to.CommentBriefVo;
import common.vo.PageVO;
import common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import product.service.SpuCommentService;
import product.vo.CommentAuditVo;
import product.vo.SpuCommentPageQuery;

import java.util.List;

/**
 * 商品评价的后台管理接口：审核列表与下架/恢复，以及一条供订单服务调用的服务间接口。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 *
 * <p>没有新增、修改与删除：评价只能由会员在会员端对自己买过的订单项发起；后台改一条评价会绕过
 * 审核链路（星级、内容乃至归属都能被改），删除还会连带清掉回复与订单上的评价时间，所以后台
 * 撤下一条评价做的是下架，而不是把记录抹掉。
 *
 * <p>{@link #commentedItems(List)} 是给 order 用的：由 {@code order/feign/ProductFeignService}
 * 直连调用、不经网关，返回"订单项到评价摘要"的映射，供会员订单列表决定显示「评价」还是「查看评论」。
 */
@RestController
@RequestMapping("product/spucomment")
public class SpuCommentController {
    @Autowired
    private SpuCommentService spuCommentService;

    /**
     * 分页查询评价，回复挂在各自的父评价下。
     *
     * @param query 查询条件：分页参数与 SKU/SPU/显示状态/关键字筛选，字段留空即不参与过滤
     * @return 分页结果，{@code rows} 为审核投影，每行的 {@code replies} 为该评价下的回复
     */
    @RequestMapping("/list")
    public R<PageVO<CommentAuditVo>> list(SpuCommentPageQuery query){
        return R.ok(spuCommentService.queryAuditPage(query));
    }


    /**
     * 审核一条评价或回复：下架或恢复。
     *
     * <p>传入回复的主键即可单独撤下某一条回复；下架一条直接评价则它连同下属回复一起从前台消失。</p>
     *
     * @param id 评价或回复主键
     * @param showStatus 目标显示状态，0 为下架、1 为恢复
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/audit/{id}")
    public R<Void> audit(@PathVariable("id") Long id, @RequestParam("showStatus") Integer showStatus){
        spuCommentService.auditComment(id, showStatus);

        return R.ok();
    }

    /**
     * 批量查询这些订单项里哪些已经评价过，供订单服务装配会员订单列表。
     *
     * <p>服务间接口：order 经 {@code order/feign/ProductFeignService} 直连调用，不经网关。
     * 入参只给订单项 ID、不给会员 —— 调用方给出的订单项已经属于它要展示的那张单。</p>
     *
     * @param orderItemIds 订单项 ID 列表，允许为空
     * @return 评价摘要列表；都没有评价时为空列表
     */
    @PostMapping("/commented-items")
    public R<List<CommentBriefVo>> commentedItems(@RequestBody List<Long> orderItemIds){
        return R.ok(spuCommentService.listCommentedItems(orderItemIds));
    }

}
