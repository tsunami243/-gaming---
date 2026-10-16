package org.example.bigevent.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 摘要工具：用于生成字符串校验码与校验口令。
 *
 * <p>注意：MD5 已不适合作为密码存储算法（易被彩虹表破解），
 * 本项目新链路统一使用 BCrypt（见 {@code PasswordConfig}），本类服务于既有历史数据。</p>
 */
public class Md5Util {

    /**
     * 字节转 16 进制字符的映射表，下标即半字节值（0~15）。
     */
    protected static char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /** 复用的摘要器实例；注意 {@link MessageDigest} 非线程安全，仅限单线程场景使用。 */
    protected static MessageDigest messagedigest = null;

    static {
        try {
            messagedigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsaex) {
            System.err.println(Md5Util.class.getName() + "初始化失败，MessageDigest不支持MD5Util。");
            nsaex.printStackTrace();
        }
    }

    /**
     * 生成字符串的 md5 校验值。
     *
     * @param s 待摘要字符串
     * @return 32 位小写十六进制摘要
     */
    public static String getMD5String(String s) {
        return getMD5String(s.getBytes());
    }

    /**
     * 校验字符串的 md5 摘要是否与已知摘要一致。
     *
     * @param password  待校验的明文
     * @param md5PwdStr 已知的 md5 摘要
     * @return 一致时返回 {@code true}
     */
    public static boolean checkPassword(String password, String md5PwdStr) {
        String s = getMD5String(password);
        return s.equals(md5PwdStr);
    }

    /**
     * 生成字节数组的 md5 校验值。
     *
     * @param bytes 待摘要字节
     * @return 32 位小写十六进制摘要
     */
    public static String getMD5String(byte[] bytes) {
        messagedigest.update(bytes);
        return bufferToHex(messagedigest.digest());
    }

    /**
     * 把整个字节数组转换为十六进制字符串。
     *
     * @param bytes 待转换字节
     * @return 十六进制字符串
     */
    private static String bufferToHex(byte bytes[]) {
        return bufferToHex(bytes, 0, bytes.length);
    }

    /**
     * 把字节数组的指定区间转换为十六进制字符串。
     *
     * @param bytes 源字节数组
     * @param m     起始下标
     * @param n     长度
     * @return 十六进制字符串
     */
    private static String bufferToHex(byte bytes[], int m, int n) {
        StringBuffer stringbuffer = new StringBuffer(2 * n);
        int k = m + n;
        for (int l = m; l < k; l++) {
            appendHexPair(bytes[l], stringbuffer);
        }
        return stringbuffer.toString();
    }

    /**
     * 把单个字节拆成高/低两个半字节并追加为十六进制字符。
     *
     * @param bt           字节
     * @param stringbuffer 目标缓冲区
     */
    private static void appendHexPair(byte bt, StringBuffer stringbuffer) {
        // 取字节中高 4 位的数字转换
        char c0 = hexDigits[(bt & 0xf0) >> 4];
        // 取字节中低 4 位的数字转换
        char c1 = hexDigits[bt & 0xf];
        stringbuffer.append(c0);
        stringbuffer.append(c1);
    }
}
