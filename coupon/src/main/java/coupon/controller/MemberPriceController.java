package coupon.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import coupon.entity.MemberPriceEntity;
import coupon.service.MemberPriceService;
import common.vo.PageVO;
import common.utils.R;



import common.query.KeyPageQuery;
/**
 * 商品会员价的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("coupon/memberprice")
public class MemberPriceController {
    @Autowired
    private MemberPriceService memberPriceService;

    /**
     * 分页查询商品会员价。
     *
     * <p>{@code key} 的过滤在内存中完成：先按分页取数，再用 Feign 回填的 SKU 名称筛，
     * 因此 {@code total} 是过滤前的总行数，{@code rows} 可能少于 {@code limit}。
     *
     * @param query 分页参数，{@code key} 全等匹配记录 ID 或模糊匹配 SKU 名称，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为会员价列表，SKU 名称由 product 服务回填
     */
    @RequestMapping("/list")
    public R<PageVO<MemberPriceEntity>> list(KeyPageQuery query){
        PageVO<MemberPriceEntity> page = memberPriceService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条商品会员价。
     *
     * @param id 会员价记录主键
     * @return 会员价详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<MemberPriceEntity> info(@PathVariable("id") Long id){
		MemberPriceEntity memberPrice = memberPriceService.getById(id);

        return R.ok(memberPrice);
    }

    /**
     * 新增一条商品会员价。
     *
     * @param memberPrice 会员价内容，主键留空时由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody MemberPriceEntity memberPrice){
		memberPriceService.save(memberPrice);

        return R.ok();
    }

    /**
     * 按主键修改一条商品会员价。
     *
     * @param memberPrice 会员价内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody MemberPriceEntity memberPrice){
		memberPriceService.updateById(memberPrice);

        return R.ok();
    }

    /**
     * 按主键批量删除商品会员价。
     *
     * @param ids 待删除的会员价记录主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		memberPriceService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
