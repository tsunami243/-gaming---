package org.example.bigevent.utils;
import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.*;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.*;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;


/**
 * 阿里云 OSS 上传工具（legacy 通道）。
 *
 * <p>凭据从环境变量 {@code OSS_ACCESS_KEY_ID} / {@code OSS_ACCESS_KEY_SECRET} 读取，
 * 桶名与地域为硬编码常量。当前采集链路已不再依赖 OSS（图片上传移出首期流程），
 * 本类仅服务于尚未下线的 legacy 上传接口。</p>
 */
public class AliOssUtil {
    // Endpoint以华北1（北京）为例，其它Region请按实际情况填写。
    private static final String ENDPOINT = "https://oss-cn-beijing.aliyuncs.com";
    // 从环境变量中获取访问凭证。运行本代码示例之前，请确保已设置环境变量OSS_ACCESS_KEY_ID和OSS_ACCESS_KEY_SECRET。
    private static final EnvironmentVariableCredentialsProvider CREDENTIALS_PROVIDER;
    static {
        try {
            CREDENTIALS_PROVIDER = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
        } catch (com.aliyuncs.exceptions.ClientException e) {
            throw new RuntimeException(e);
        }
    }
    // 填写Bucket名称，例如big-event-by-tsunami。
    private static final String BUCKET_NAME = "big-event-by-tsunami";
    // 填写Bucket所在地域。以华北1（北京）为例，Region填写为cn-beijing。
    private static final String REGION = "cn-beijing";

    /**
     * 上传一个流对象到 OSS。
     *
     * <p>异常被就地捕获并打印，失败时返回空串而非抛出——调用方需自行判断返回值是否为空。
     * 无论成败都会关闭 OSS 客户端以释放连接资源。</p>
     *
     * @param ObjectName 对象键（即上传后在桶内的路径/文件名）
     * @param in         待上传的输入流
     * @return 上传成功返回可访问的 HTTPS 地址，失败返回空串
     * @throws Exception 参数非法等未在方法内捕获的异常
     */
    public static String uploadFile(String ObjectName, InputStream in) throws Exception {
        String url = "";
        // 创建OSSClient实例。
        // 当OSSClient实例不再使用时，调用shutdown方法以释放资源。
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(ENDPOINT)
                .credentialsProvider(CREDENTIALS_PROVIDER)
                .clientConfiguration(clientBuilderConfiguration)
                .region(REGION)
                .build();

        try {
            // 创建PutObjectRequest对象。
            PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME, ObjectName, in);
            // 如果需要上传时设置存储类型和访问权限，请参考以下示例代码。
            // ObjectMetadata metadata = new ObjectMetadata();
            // metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
            // metadata.setObjectAcl(CannedAccessControlList.Private);
            // putObjectRequest.setMetadata(metadata);
            // 上传文件。
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            url = "https://" + BUCKET_NAME + "." + REGION + ".aliyuncs.com/" + ObjectName;
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return url;
    }

}
