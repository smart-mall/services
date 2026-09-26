package common.valid;

import jakarta.validation.groups.Default;

/**
 * 新增场景的校验分组。
 *
 * <p>继承 {@link Default}，未标注分组的约束在新增时同样生效；标注了 {@code AddGroup}
 * 的约束只在新增时校验。
 */
public interface AddGroup extends Default {
}
