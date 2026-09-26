package common.valid;

import jakarta.validation.groups.Default;

/**
 * 更新场景的校验分组。
 *
 * <p>继承 {@link Default}，未标注分组的约束在更新时同样生效；标注了 {@code UpdateGroup}
 * 的约束只在更新时校验。
 */
public interface UpdateGroup extends Default {
}
