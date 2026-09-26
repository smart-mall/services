package product.controller;

import common.vo.PageVO;
import common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import product.entity.SpuCommentEntity;
import product.service.SpuCommentService;

import java.util.Arrays;


import common.query.PageQuery;
/**
 * 商品评价的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权；评价下的回复关系走
 * {@link CommentReplayController}。
 */
@RestController
@RequestMapping("product/spucomment")
public class SpuCommentController {
    @Autowired
    private SpuCommentService spuCommentService;

    /**
     * 分页查询商品评价。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为评价列表
     */
    @RequestMapping("/list")
    public R<PageVO<SpuCommentEntity>> list(PageQuery query){
        PageVO<SpuCommentEntity> page = spuCommentService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询商品评价详情。
     *
     * @param id 评价主键
     * @return 评价详情；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<SpuCommentEntity> info(@PathVariable("id") Long id){
		SpuCommentEntity spuComment = spuCommentService.getById(id);

        return R.ok(spuComment);
    }

    /**
     * 新增一条商品评价。
     *
     * @param spuComment 评价内容，需带 {@code spuId} 与 {@code skuId}
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody SpuCommentEntity spuComment){
		spuCommentService.save(spuComment);

        return R.ok();
    }

    /**
     * 按主键修改商品评价。
     *
     * @param spuComment 评价内容，主键必填
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody SpuCommentEntity spuComment){
		spuCommentService.updateById(spuComment);

        return R.ok();
    }

    /**
     * 按主键批量删除商品评价。
     *
     * @param ids 待删除的评价主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		spuCommentService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
