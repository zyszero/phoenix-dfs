package io.github.zyszero.phoenix.dfs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * file meta data.
 *
 * @Author: zyszero
 * @Date: 2024/8/7 21:23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileMeta {
    private String name;
    private String originalName;
    private long size;
    private String downloadUrl;
    private Map<String, String> tags = new HashMap<>();

    public FileMeta(String name, String originalName, long size, String downloadUrl) {
        this.name = name;
        this.originalName = originalName;
        this.size = size;
        this.downloadUrl = downloadUrl;
    }
}
