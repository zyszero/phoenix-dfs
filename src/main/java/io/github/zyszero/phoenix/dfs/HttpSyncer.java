package io.github.zyszero.phoenix.dfs;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;

/**
 * sync file to backup server.
 *
 * @Author: zyszero
 * @Date: 2024/8/6 20:57
 */
@Component
public class HttpSyncer {

    public final static String X_FILENAME = "X-Filename";


    public String sync(File file, String url) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add(X_FILENAME, file.getName());

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", new FileSystemResource(file));

        HttpEntity<MultiValueMap<String, HttpEntity<?>>> httpEntity = new HttpEntity<>(bodyBuilder.build(), headers);

        ResponseEntity<String> result = restTemplate.postForEntity(url, httpEntity, String.class);
        System.out.println(" sync result = " + result);
        return result.getBody();
    }
}
