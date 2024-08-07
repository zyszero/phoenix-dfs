package io.github.zyszero.phoenix.dfs;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;
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
        return UUID.randomUUID() + "." + getExt(file);
    }

    public static String getSubDir(String file) {
        return file.substring(0, 2);
    }

    public static String getExt(String originalFilename) {
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}
