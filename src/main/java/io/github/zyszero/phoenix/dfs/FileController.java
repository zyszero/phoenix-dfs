package io.github.zyszero.phoenix.dfs;

import io.github.zyszero.phoenix.dfs.config.PhoenixDfsProperties;
import io.github.zyszero.phoenix.dfs.meta.FileMeta;
import io.github.zyszero.phoenix.dfs.syncer.HttpSyncer;
import io.github.zyszero.phoenix.dfs.syncer.MQSyncer;
import io.github.zyszero.phoenix.dfs.utils.FileUtils;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * file download and upload controller
 *
 * @Author: zyszero
 * @Date: 2024/8/5 20:42
 */
@RestController
public class FileController {


    @Autowired
    private PhoenixDfsProperties properties;

    @Autowired
    private HttpSyncer httpSyncer;


    @Autowired
    private MQSyncer mqSyncer;

    @SneakyThrows
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         HttpServletRequest request) {
        if (file == null) {
            return "file is null";
        }

        // 1. 处理文件
        boolean needSync = false;
        String filename = request.getHeader(HttpSyncer.X_FILENAME);
        String originalFilename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) { // 如果这个为空则是正常上传
            needSync = true;
            filename = FileUtils.getUUIDFilename(originalFilename);
        } else { // 如果走到这里，说明是主从同步文件
            String xof = request.getHeader(HttpSyncer.X_ORIGINAL_FILENAME);
            if (xof != null && !xof.isEmpty()) {
                originalFilename = xof;
            }
        }

        File destFile = getFile(FileUtils.getSubDir(filename), filename);
        file.transferTo(destFile); // 复制文件到制定位置
        System.out.println(" ==> save file: " + destFile.getAbsolutePath());

        // 2. 处理 meta
        FileMeta meta = new FileMeta(filename, originalFilename, file.getSize(), properties.getDownloadUrl());
        if (properties.isAutoMd5()) {
            meta.getTags().put("md5", DigestUtils.md5DigestAsHex(new FileInputStream(destFile)));
        }
        // 2.1 存放到本地文件
        File metaFile = new File(destFile.getAbsolutePath() + ".meta");
        FileUtils.write(metaFile, meta);
        System.out.println(" ==> meta file save: " + metaFile.getAbsolutePath());

        // 2.2  TODO 存放到数据库

        // 2.3  TODO 存放到配置中心或注册中心，比如 zk

        // 3. 同步备份文件到备份服务器
        if (needSync) {
            if (properties.isSyncBackup()) {
                try {
                    httpSyncer.sync(destFile, originalFilename, properties.getBackupUrl());
                } catch (Exception ex) {
                    // log ex
                    ex.printStackTrace();
                    mqSyncer.sync(meta); // 同步失败则转异步处理
                }
            } else {
                // 异步备份文件到备份服务器
                mqSyncer.sync(meta);
            }
        }
        return filename;
    }


    @RequestMapping("/download")
    public void download(String name, HttpServletResponse response) {
        File file = getFile(FileUtils.getSubDir(name), name);
        try {
            FileInputStream fis = new FileInputStream(file);
            InputStream inputStream = new BufferedInputStream(fis);
            byte[] buffer = new byte[16 * 1024];

            // 加一些 responses 的头
            response.setCharacterEncoding("UTF-8");
//            response.setContentType("application/octet-stream");
            response.setContentType(FileUtils.getMimeType(name));
//            response.setHeader("Content-Disposition", "attachment;filename=" + name);
            response.setHeader("Content-Length", String.valueOf(file.length()));


            // 读取文件信息，并逐段输出
            ServletOutputStream outputStream = response.getOutputStream();
            while (inputStream.read(buffer) != -1) {
                outputStream.write(buffer);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/meta")
    public String meta(String name) {
        File file = getFile(FileUtils.getSubDir(name), name);
        try {
            return FileCopyUtils.copyToString(new FileReader(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File getFile(String subDir, String filename) {
        return new File(properties.getUploadPath() + "/" + subDir + "/" + filename);
    }
}
