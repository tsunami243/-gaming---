package org.example.bigevent.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.example.bigevent.anno.State;

/**
 * {@code @State} 注解的校验器：限制文章发布状态只能是「草稿」或「已发布」。
 *
 * <p>泛型第一个参数是注解类型，第二个是被校验字段的类型（String）。</p>
 */
public class StateValidation implements ConstraintValidator<State, String> {

    /**
     * 校验状态值是否合法。
     *
     * @param value   待校验的状态字符串
     * @param context 校验上下文（本实现未使用）
     * @return 值为「草稿」或「已发布」时返回 {@code true}；为 null 或其它值返回 {@code false}
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return value.equals("草稿") || value.equals("已发布");
    }
}
