package thirdParty.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("address")
public class AddressEntity {

    @TableId(type = IdType.INPUT)
    @TableField("NODE_CODE")
    private String nodeCode; // 地址编码

    @TableField("NODE_NAME")
    private String nodeName; // 地区名称

    @TableField("NODE_SNAME")
    private String nodeSname; // 地区全称

    @TableField("CODE_PARENT")
    private String codeParent; // 父级编码

    @TableField("NODE_INITIALITION")
    private String nodeInitialition; // 默认值(预留)

    @TableField("NODE_SPELL")
    private String nodeSpell; // 首字母

    @TableField("NODE_TYPE")
    private BigDecimal nodeType; // 类型：1省会，2直辖市，3港澳台，4其它

    @TableField("NODER_ORDER")
    private BigDecimal norderOrder; // 同级下排序

    @TableField("NODE_LEVEL")
    private BigDecimal nodeLevel; // 级别：0全国、1省、2市区、3郊县、4街道、5居委会

    @TableField("NODE_REMARK")
    private String nodeRemark; // 备注

    @TableField("VILLAGE_TYPE")
    private String villageType; // 城乡分类代码

    @TableField("NATION_NAME")
    private String nationName; // 所属国家名

    @TableField("PROVINCE_NAME")
    private String provinceName; // 所属省名称

    @TableField("CITY_NAME")
    private String cityName; // 所属市名称

    @TableField("COUNTY_NAME")
    private String countyName; // 所属区县名称

    @TableField("TOWN_NAME")
    private String townName; // 所属街道名称

    @TableField("LNG")
    private String lng; // 经度

    @TableField("LAT")
    private String lat; // 纬度

    @TableField("MAPTYPE")
    private BigDecimal maptype; // 来源地图：1百度，2高德
}