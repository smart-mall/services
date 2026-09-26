/** Copyright 2020 bejson.com。 */
package common.to;

import lombok.Data;

import java.math.BigDecimal;

/** 会员价条目：会员等级 ID、等级名称与对应的价格。 */

@Data
public class MemberPrice {

  /** 会员等级 ID。 */
  private Long id;
  /** 会员等级名称。 */
  private String name;
  /** 该等级的会员价。 */
  private BigDecimal price;

}
