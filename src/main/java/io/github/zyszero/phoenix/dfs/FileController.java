package io.github.zyszero.phoenix.dfs;

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
import java.util.UUID;

/**
 * file download and upload controller
 *
 * @Author: zyszero
 * @Date: 2024/8/5 20:42
 */
@RestController
public class FileController {

    @Value("${phoenix-dfs.path}")
    private String uploadPath;


    @Value("${phoenix-dfs.backup-url}")
    private String backupUrl;

    @Value("${phoenix-dfs.auto-md5}")
    private boolean autoMd5;


    @Autowired
    private HttpSyncer httpSyncer;


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
        String originalFilename =  file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            needSync = true;
            filename = FileUtils.getUUIDFilename(originalFilename);
        } else {
           String xof = request.getHeader(HttpSyncer.X_ORIGINAL_FILENAME);
           if (xof != null && !xof.isEmpty()) {
               originalFilename = xof;
           }
        }

        String subDir = FileUtils.getSubDir(filename);
        File destFile = new File(uploadPath + "/" + subDir + "/" + filename);
        file.transferTo(destFile);


        // 2. 处理 meta
        FileMeta meta = new FileMeta();
        meta.setName(filename);
        meta.setOriginalName(originalFilename);
        meta.setSize(file.getSize());
        meta.getTags().put("md5", DigestUtils.md5DigestAsHex(new FileInputStream(destFile)));

        // 2.1 存放到本地文件
        // todo: 存在 bug，同步 backup 会丢失 meta 数据。待解决
        String metaName = filename + ".meta";
        File metaFile = new File(uploadPath + "/" + subDir + "/" + metaName);
        FileUtils.write(metaFile, meta);

        // 2.2 存放到数据库

        // 2.3 存放到配置中心或注册中心，比如 zk

        // 3. 同步文件到备份服务器
        if (needSync) {
            httpSyncer.sync(destFile, originalFilename, backupUrl);
        }

        return filename;
    }


    @RequestMapping("/download")
    public void download(String name, HttpServletResponse response) {
        String subDir = FileUtils.getSubDir(name);
        String path = uploadPath + "/" + subDir + "/" + name;
        File file = new File(path);
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
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/meta")
    public String meta(String name) {
        String subDir = FileUtils.getSubDir(name);
        String path = uploadPath + "/" + subDir + "/" + name + ".meta";
        File file = new File(path);
        try {
            return FileCopyUtils.copyToString(new FileReader(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
