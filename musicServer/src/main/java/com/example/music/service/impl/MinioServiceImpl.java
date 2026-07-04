package com.example.music.service.impl;

import com.example.music.constant.MessageConstant;
import com.example.music.service.MinioService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;

    // 允许上传的文件扩展名白名单
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "mp3", "flac", "wav"));

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    public MinioServiceImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * 校验文件扩展名是否在白名单内，返回小写扩展名
     */
    private String validateExtension(String originalFileName) {
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new RuntimeException(MessageConstant.FILE_UPLOAD + MessageConstant.FAILED + "：文件名为空");
        }
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFileName.length() - 1) {
            throw new RuntimeException(MessageConstant.FILE_UPLOAD + MessageConstant.FAILED + "：文件扩展名非法");
        }
        String ext = originalFileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new RuntimeException(MessageConstant.FILE_UPLOAD + MessageConstant.FAILED + "：不支持的文件类型");
        }
        return ext;
    }

    /**
     * 上传文件到 Minio
     *
     * @param file   文件
     * @param folder 文件夹
     * @return 可访问的 URL
     */
    @Override
    public String uploadFile(MultipartFile file, String folder) {
        String ext = validateExtension(file.getOriginalFilename());
        // 使用 UUID 作为存储文件名，避免覆盖和路径穿越
        String fileName = folder + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        // 使用 try-with-resources 确保输入流关闭
        try (InputStream inputStream = file.getInputStream()) {
            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 返回可访问的 URL
            return endpoint + "/" + bucketName + "/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException(MessageConstant.FILE_UPLOAD + MessageConstant.FAILED + "：" + e.getMessage());
        }
    }

    /**
     * 通过输入流上传文件
     *
     * @param inputStream     文件输入流
     * @param originalFileName 原始文件名
     * @param contentType     文件内容类型
     * @param folder          文件夹
     * @return 可访问的 URL
     */
    @Override
    public String uploadFile(InputStream inputStream, String originalFileName, String contentType, String folder) {
        String ext = validateExtension(originalFileName);
        String fileName = folder + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try (InputStream is = inputStream) {
            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(is, -1, 10485760) // Part size 10MB
                            .contentType(contentType)
                            .build()
            );

            // 返回可访问的 URL
            return endpoint + "/" + bucketName + "/" + fileName;

        } catch (Exception e) {
            log.error("文件上传失败：{}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param fileUrl 文件 URL
     */
    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        try {
            String filePath = fileUrl;
            // 尝试通过 bucket name 定位 path，更健壮地处理不同的 host (localhost vs 127.0.0.1)
            String separator = "/" + bucketName + "/";
            int index = fileUrl.indexOf(separator);

            if (index != -1) {
                // 截取 bucketName 之后的部分作为 object key
                filePath = fileUrl.substring(index + separator.length());
            } else {
                // 兼容旧逻辑：如果 fileUrl 严格以前缀开头
                String prefix = endpoint + "/" + bucketName + "/";
                if (fileUrl.startsWith(prefix)) {
                    filePath = fileUrl.substring(prefix.length());
                }
            }

            // URL 解码，防止文件名中有特殊字符被编码
            filePath = java.net.URLDecoder.decode(filePath, java.nio.charset.StandardCharsets.UTF_8.name());

            // 删除文件
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filePath)
                            .build()
            );

        } catch (Exception e) {
            throw new RuntimeException("文件删除失败: " + e.getMessage());
        }
    }
}
