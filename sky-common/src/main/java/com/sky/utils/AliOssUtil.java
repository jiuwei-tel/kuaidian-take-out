package com.sky.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 文件上传
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
/*            // 创建PutObject请求。
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));*/

            // 创建PutObjectRequest对象
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, new ByteArrayInputStream(bytes));

            // 设置文件的Content-Type
            ObjectMetadata metadata = new ObjectMetadata();
            String contentType = getContentType(objectName.substring(objectName.lastIndexOf(".")));
            metadata.setContentType(contentType);
            putObjectRequest.setMetadata(metadata);

            // 使用PutObjectRequest上传文件
            ossClient.putObject(putObjectRequest);


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

        //文件访问路径规则 https://BucketName.Endpoint/ObjectName
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)
                .append(".")
                .append(endpoint)
                .append("/")
                .append(objectName);

        log.info("文件上传到:{}", stringBuilder.toString());

        return stringBuilder.toString();
    }


    // 根据文件扩展名获取对应的Content-Type
    private String getContentType(String fileExtension) {
        fileExtension = fileExtension.toLowerCase();
        Map<String, String> contentTypeMap = new HashMap<>();
        contentTypeMap.put(".jpg", "image/jpeg");
        contentTypeMap.put(".jpeg", "image/jpeg");
        contentTypeMap.put(".png", "image/png");
        contentTypeMap.put(".gif", "image/gif");
        contentTypeMap.put(".bmp", "image/bmp");
        contentTypeMap.put(".webp", "image/webp");

        return contentTypeMap.getOrDefault(fileExtension, "application/octet-stream");
    }
// ... existing code ...

}
