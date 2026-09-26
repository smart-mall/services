package coupon.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import coupon.entity.HomeSubjectEntity;
import coupon.service.HomeSubjectService;
import common.vo.PageVO;
import common.utils.R;



import common.query.KeyPageQuery;
/**
 * 首页专题的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("coupon/homesubject")
public class HomeSubjectController {
    @Autowired
    private HomeSubjectService homeSubjectService;

    /**
     * 分页查询首页专题。
     *
     * @param query 分页参数，{@code key} 模糊匹配专题名或专题 ID，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为专题列表
     */
    @RequestMapping("/list")
    public R<PageVO<HomeSubjectEntity>> list(KeyPageQuery query){
        PageVO<HomeSubjectEntity> page = homeSubjectService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条首页专题。
     *
     * @param id 专题主键
     * @return 专题详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<HomeSubjectEntity> info(@PathVariable("id") Long id){
		HomeSubjectEntity homeSubject = homeSubjectService.getById(id);

        return R.ok(homeSubject);
    }

    /**
     * 新增一条首页专题。
     *
     * @param homeSubject 专题内容，主键留空时由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody HomeSubjectEntity homeSubject){
		homeSubjectService.save(homeSubject);

        return R.ok();
    }

    /**
     * 按主键修改一条首页专题。
     *
     * @param homeSubject 专题内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody HomeSubjectEntity homeSubject){
		homeSubjectService.updateById(homeSubject);

        return R.ok();
    }

    /**
     * 按主键批量删除首页专题。
     *
     * @param ids 待删除的专题主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		homeSubjectService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
