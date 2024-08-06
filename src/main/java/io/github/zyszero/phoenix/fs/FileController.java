package io.github.zyszero.phoenix.fs;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${phoenix-fs.path}")
    private String uploadPath;


    @Value("${phoenix-fs.backup-url}")
    private String backupUrl;


    @Autowired
    private HttpSyncer httpSyncer;


    @SneakyThrows
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         HttpServletRequest request) {
        if (file == null) {
            return "file is null";
        }

        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdir();
        }

        boolean needSync = false;

        String filename = request.getHeader(HttpSyncer.X_FILENAME);
        if (filename == null || filename.isEmpty()) {
            needSync = true;
            filename = file.getOriginalFilename();
        } else {
            filename = new String(filename.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        }
        System.out.println(" filename: " + filename);
        File destFile = new File(uploadPath + "/", filename);
        file.transferTo(destFile);

        if (needSync) {
            httpSyncer.sync(destFile, backupUrl);
        }

        return filename;
    }


    @RequestMapping("/download")
    public void download(String name, HttpServletResponse response) {
        String path = uploadPath + "/" + name;
        File file = new File(path);
        try {
            FileInputStream fis = new FileInputStream(file);
            InputStream inputStream = new BufferedInputStream(fis);
            byte[] buffer = new byte[16 * 1024];

            // 加一些 responses 的头
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + name);
            response.setHeader("Content-Length", String.valueOf(file.length()));


            // 读取文件信息，并逐段输出
            ServletOutputStream outputStream = response.getOutputStream();
            while (inputStream.read(buffer) != -1) {
                outputStream.write(buffer);
            }

            outputStream.flush();
            inputStream.close();
        } catch (Exception e) {

        }


    }
}
