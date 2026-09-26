package product.controller;

import common.vo.PageVO;
import common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import product.entity.CommentReplayEntity;
import product.service.CommentReplayService;

import java.util.Arrays;
import java.util.Map;


import common.query.PageQuery;
/**
 * 商品评价回复关系的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("product/commentreplay")
public class CommentReplayController {
    @Autowired
    private CommentReplayService commentReplayService;

    /**
     * 分页查询商品评价回复关系。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为回复关系列表
     */
    @RequestMapping("/list")
    public R<PageVO<CommentReplayEntity>> list(PageQuery query){
        PageVO<CommentReplayEntity> page = commentReplayService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询回复关系详情。
     *
     * @param id 回复关系主键
     * @return 回复关系详情；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<CommentReplayEntity> info(@PathVariable("id") Long id){
		CommentReplayEntity commentReplay = commentReplayService.getById(id);

        return R.ok(commentReplay);
    }

    /**
     * 新增一条商品评价回复关系。
     *
     * @param commentReplay 回复关系内容，主键由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody CommentReplayEntity commentReplay){
		commentReplayService.save(commentReplay);

        return R.ok();
    }

    /**
     * 按主键修改回复关系。
     *
     * @param commentReplay 回复关系内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody CommentReplayEntity commentReplay){
		commentReplayService.updateById(commentReplay);

        return R.ok();
    }

    /**
     * 按主键批量删除回复关系。
     *
     * @param ids 待删除的回复关系主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		commentReplayService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
