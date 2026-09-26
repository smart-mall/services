package member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.ValidationException;
import common.to.LoginLogTo;
import common.vo.PageVO;
import common.utils.Query;
import member.dao.MemberLoginLogDao;
import member.entity.MemberLoginLogEntity;
import member.service.MemberLoginLogService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;


@Service("memberLoginLogService")
public class MemberLoginLogServiceImpl extends ServiceImpl<MemberLoginLogDao, MemberLoginLogEntity> implements MemberLoginLogService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public PageVO<MemberLoginLogEntity> queryPage(Map<String, Object> params) {
        IPage<MemberLoginLogEntity> page = this.page(
                new Query<MemberLoginLogEntity>().getPage(params),
                new QueryWrapper<MemberLoginLogEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    @Override
    public void record(LoginLogTo to) {
        MemberLoginLogEntity entity = new MemberLoginLogEntity();
        entity.setMemberId(to.getMemberId());
        entity.setIp(to.getIp());
        entity.setCity(to.getCity());
        entity.setLoginType(to.getLoginType());
        entity.setCreateTime(new Date());

        this.save(entity);
    }

    @Override
    public PageVO<MemberLoginLogEntity> queryMine(Long memberId, Map<String, Object> params) {
        long pageNum = parseNumber(params.get("pageNum"), 1, "pageNum");
        long pageSize = Math.min(parseNumber(params.get("pageSize"), DEFAULT_PAGE_SIZE, "pageSize"), MAX_PAGE_SIZE);

        IPage<MemberLoginLogEntity> page = this.page(
                new Page<>(pageNum, pageSize),
                new QueryWrapper<MemberLoginLogEntity>()
                        .eq("member_id", memberId)
                        .orderByDesc("create_time")
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    private long parseNumber(Object raw, long defaultValue, String name) {
        if (raw == null || raw.toString().isBlank()) {
            return defaultValue;
        }
        long value;
        try {
            value = Long.parseLong(raw.toString().trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(name, name + " 参数类型不正确");
        }
        if (value < 1) {
            throw new ValidationException(name, name + " 必须大于 0");
        }
        return value;
    }

}
