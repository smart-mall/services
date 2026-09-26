package coupon.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import coupon.entity.HomeAdvEntity;
import coupon.service.HomeAdvService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 首页轮播广告的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("coupon/homeadv")
public class HomeAdvController {
    @Autowired
    private HomeAdvService homeAdvService;

    /**
     * 分页查询首页轮播广告。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为轮播广告列表
     */
    @RequestMapping("/list")
    public R<PageVO<HomeAdvEntity>> list(PageQuery query){
        PageVO<HomeAdvEntity> page = homeAdvService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条首页轮播广告。
     *
     * @param id 轮播广告主键
     * @return 轮播广告详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<HomeAdvEntity> info(@PathVariable("id") Long id){
		HomeAdvEntity homeAdv = homeAdvService.getById(id);

        return R.ok(homeAdv);
    }

    /**
     * 新增一条首页轮播广告。
     *
     * @param homeAdv 轮播广告内容，主键留空时由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody HomeAdvEntity homeAdv){
		homeAdvService.save(homeAdv);

        return R.ok();
    }

    /**
     * 按主键修改一条首页轮播广告。
     *
     * @param homeAdv 轮播广告内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody HomeAdvEntity homeAdv){
		homeAdvService.updateById(homeAdv);

        return R.ok();
    }

    /**
     * 按主键批量删除首页轮播广告。
     *
     * @param ids 待删除的轮播广告主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		homeAdvService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
