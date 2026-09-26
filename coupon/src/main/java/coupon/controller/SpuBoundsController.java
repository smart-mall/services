package coupon.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.vo.PageVO;
import common.utils.R;
import coupon.entity.SpuBoundsEntity;
import coupon.service.SpuBoundsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;



import common.query.KeyPageQuery;
/**
 * SPU 积分设置的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("coupon/spubounds")
@Slf4j
public class SpuBoundsController {
    private final SpuBoundsService spuBoundsService;

    /**
     * 创建 SPU 积分设置管理接口实例。
     *
     * @param spuBoundsService 积分设置业务服务，由容器注入
     */
    public SpuBoundsController(SpuBoundsService spuBoundsService) {
        this.spuBoundsService = spuBoundsService;
    }

    /**
     * 分页查询 SPU 积分设置。
     *
     * <p>{@code key} 的过滤在内存中完成：先按分页取数，再用 Feign 回填的 SPU 名称筛，
     * 因此 {@code total} 是过滤前的总行数，{@code rows} 可能少于 {@code limit}。
     *
     * @param query 分页参数，{@code key} 全等匹配记录 ID 或模糊匹配 SPU 名称，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为积分设置列表，SPU 名称由 product 服务回填
     */
    @RequestMapping("/list")
    public R<PageVO<SpuBoundsEntity>> list(KeyPageQuery query){
        log.info("列表查询spuBounds：{}", JSON.toJSONString( query, SerializerFeature.PrettyFormat));
        PageVO<SpuBoundsEntity> page = spuBoundsService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条 SPU 积分设置。
     *
     * @param id 积分设置记录主键
     * @return 积分设置详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<SpuBoundsEntity> info(@PathVariable("id") Long id){
		SpuBoundsEntity spuBounds = spuBoundsService.getById(id);
        log.info("根据id查询spuBounds：{}", id);

        return R.ok(spuBounds);
    }

    /**
     * 新增一条 SPU 积分设置。
     *
     * @param spuBounds 积分设置内容，主键留空时由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody SpuBoundsEntity spuBounds){
        log.info("保存spuBounds：{}", JSON.toJSONString(spuBounds, SerializerFeature.PrettyFormat));
		spuBoundsService.save(spuBounds);

        return R.ok();
    }

    /**
     * 按主键修改一条 SPU 积分设置。
     *
     * @param spuBounds 积分设置内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody SpuBoundsEntity spuBounds){
        log.info("修改spuBounds：{}", JSON.toJSONString(spuBounds, SerializerFeature.PrettyFormat));
		spuBoundsService.updateById(spuBounds);

        return R.ok();
    }

    /**
     * 按主键批量删除 SPU 积分设置。
     *
     * @param ids 待删除的积分设置记录主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
        log.info("删除spuBounds：{}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
		spuBoundsService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
