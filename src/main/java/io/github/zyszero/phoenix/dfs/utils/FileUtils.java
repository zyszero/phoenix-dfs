package io.github.zyszero.phoenix.dfs.utils;

import com.alibaba.fastjson2.JSON;
import io.github.zyszero.phoenix.dfs.meta.FileMeta;
import lombok.SneakyThrows;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * utils for file.
 *
 * @Author: zyszero
 * @Date: 2024/8/7 20:41
 */
public class FileUtils {
    static String DEFAULT_MIME_TYPE = "application/octet-stream";

    public static String getMimeType(String fileName) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String content = fileNameMap.getContentTypeFor(fileName);
        return content == null ? DEFAULT_MIME_TYPE : content;
    }

    public static void init(String uploadPath) {

        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdir();
        }

        for (int i = 0; i < 256; i++) {
            String subDir = java.lang.String.format("%02x", i);
            File file = new File(uploadPath + "/" + subDir);
            if (!file.exists()) {
                file.mkdir();
            }
        }
    }


    public static String getUUIDFilename(String file) {
        return UUID.randomUUID() + getExt(file);
    }

    public static String getSubDir(String file) {
        return file.substring(0, 2);
    }

    public static String getExt(String originalFilename) {
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    @SneakyThrows
    public static void write(File metaFile, FileMeta meta) {
        String json = JSON.toJSONString(meta);
        Files.writeString(Paths.get(metaFile.getAbsolutePath()), json,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
    }

    @SneakyThrows
    public static void writeString(File file, String content) {
        Files.writeString(Paths.get(file.getAbsolutePath()), content,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
    }

    @SneakyThrows
    public static void download(String downloadUrl, File file) {
        System.out.println(" ===>>> download file: " + file.getAbsolutePath());
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<Resource> exchange = restTemplate.exchange(downloadUrl, HttpMethod.GET, entity, Resource.class);
        InputStream fis = new BufferedInputStream(exchange.getBody().getInputStream());
        byte[] buffer = new byte[16 * 1024];

        // 读取文件信息，并逐段输出
        OutputStream outputStream = new FileOutputStream(file);
        while (fis.read(buffer) != -1) {
            outputStream.write(buffer);
        }
        outputStream.flush();
        outputStream.close();
        fis.close();
    }
}
