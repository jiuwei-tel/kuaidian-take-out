package com.sky.utils;

import com.sky.properties.UploadProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地磁盘文件存储：把上传的文件写进 sky.upload.dir，返回 urlPrefix + 文件名 的相对地址。
 * 相对地址交给浏览器按当前站点解析，前端走 Vite / Nginx 代理就能访问到，不用关心端口。
 */
@Component
@Slf4j
public class LocalFileUtil {

    private final UploadProperties uploadProperties;

    /** 落盘目录的绝对路径，启动时解析好 */
    private Path baseDir;

    public LocalFileUtil(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @PostConstruct
    public void init() throws IOException {
        baseDir = Paths.get(uploadProperties.getDir()).toAbsolutePath().normalize();
        Files.createDirectories(baseDir);
        log.info("本地上传目录: {}", baseDir);
    }

    /**
     * @param bytes      文件内容
     * @param objectName 新文件名，调用方已经拼好 UUID + 扩展名
     * @return 可访问的相对地址，例如 /images/3f2a....webp
     */
    public String upload(byte[] bytes, String objectName) throws IOException {
        Path target = baseDir.resolve(objectName).normalize();

        // 文件名由后端拼的，正常不会有 .. ；这里再确认一次落点没跑出目录
        if (!target.startsWith(baseDir)) {
            throw new IOException("非法的文件名: " + objectName);
        }

        Files.write(target, bytes);
        log.info("文件已保存: {}", target);

        String prefix = uploadProperties.getUrlPrefix();
        return (prefix.endsWith("/") ? prefix : prefix + "/") + objectName;
    }

    /** 供静态资源映射使用 */
    public File getDirFile() {
        return baseDir.toFile();
    }

}
