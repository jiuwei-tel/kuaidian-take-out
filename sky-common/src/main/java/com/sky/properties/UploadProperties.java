package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地文件上传配置。
 * 图片不再传阿里云 OSS，直接落到本机磁盘，由 WebMvcConfiguration 做静态资源映射对外访问，
 * 省掉 AccessKey 过期、欠费、CORS 这些跟业务无关的麻烦。
 */
@Component
@ConfigurationProperties(prefix = "sky.upload")
@Data
public class UploadProperties {

    /** 文件落盘目录，支持绝对路径；相对路径按「后端进程的工作目录」解析 */
    private String dir = "upload";

    /** 对外访问前缀，要和 WebMvcConfiguration 里注册的 resourceHandler 保持一致 */
    private String urlPrefix = "/images";

}
