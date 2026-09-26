package thirdParty.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 行政区划节点，对应 {@code address} 表。
 *
 * <p>全国、省、市、区县、街道等各层级共用一张表，靠 {@code codeParent} 指向父节点编码串成树，
 * 顶层节点的父编码为 {@code "0"}。
 */
@Data
@TableName("address")
public class AddressEntity {

    /** 地址编码，主键；由导入的数据自带，不使用数据库自增 */
    @TableId(type = IdType.INPUT)
    @TableField("NODE_CODE")
    private String nodeCode;

    /** 地区名称，如「朝阳区」 */
    @TableField("NODE_NAME")
    private String nodeName;

    /** 地区全称，如「北京市市辖区朝阳区」 */
    @TableField("NODE_SNAME")
    private String nodeSname;

    /** 父级地址编码，顶层节点为 "0" */
    @TableField("CODE_PARENT")
    private String codeParent;

    /** 预留字段，当前代码未读写 */
    @TableField("NODE_INITIALITION")
    private String nodeInitialition;

    /** 名称首字母，如「朝阳区」为 C */
    @TableField("NODE_SPELL")
    private String nodeSpell;

    /** 类型：1 省会，2 直辖市，3 港澳台，4 其它 */
    @TableField("NODE_TYPE")
    private BigDecimal nodeType;

    /** 同级节点内的排序序号 */
    @TableField("NODER_ORDER")
    private BigDecimal norderOrder;

    /** 级别：0 全国、1 省、2 市区、3 郊县、4 街道、5 居委会 */
    @TableField("NODE_LEVEL")
    private BigDecimal nodeLevel;

    /** 备注 */
    @TableField("NODE_REMARK")
    private String nodeRemark;

    /** 城乡分类代码 */
    @TableField("VILLAGE_TYPE")
    private String villageType;

    /** 所属国家名 */
    @TableField("NATION_NAME")
    private String nationName;

    /** 所属省名称 */
    @TableField("PROVINCE_NAME")
    private String provinceName;

    /** 所属市名称 */
    @TableField("CITY_NAME")
    private String cityName;

    /** 所属区县名称 */
    @TableField("COUNTY_NAME")
    private String countyName;

    /** 所属街道名称 */
    @TableField("TOWN_NAME")
    private String townName;

    /** 经度 */
    @TableField("LNG")
    private String lng;

    /** 纬度 */
    @TableField("LAT")
    private String lat;

    /** 来源地图：1 百度，2 高德 */
    @TableField("MAPTYPE")
    private BigDecimal maptype;
}