package org.example.bigevent.utils;

/**
 * ThreadLocal 工具类：把当前请求的登录信息（JWT 载荷）绑定到处理线程上，
 * 使 Service 层无需层层传参即可拿到当前用户。
 *
 * <p>必须在请求结束前调用 {@link #remove()}，否则线程池复用会导致用户信息串号与内存泄漏。</p>
 */
@SuppressWarnings("all")
public class ThreadLocalUtil {

    /** 承载当前线程登录信息的容器。 */
    private static final ThreadLocal THREAD_LOCAL = new ThreadLocal();

    /**
     * 读取当前线程绑定的值。
     *
     * @param <T> 期望类型
     * @return 绑定值，未设置时返回 {@code null}
     */
    public static <T> T get() {
        return (T) THREAD_LOCAL.get();
    }

    /**
     * 绑定值到当前线程。
     *
     * @param value 待绑定对象（通常为 JWT 载荷 Map）
     */
    public static void set(Object value) {
        THREAD_LOCAL.set(value);
    }

    /** 清除当前线程绑定，防止内存泄漏与用户信息串号。 */
    public static void remove() {
        THREAD_LOCAL.remove();
    }
}
