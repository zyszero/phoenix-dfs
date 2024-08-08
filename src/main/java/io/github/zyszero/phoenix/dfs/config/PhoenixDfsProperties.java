package io.github.zyszero.phoenix.dfs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author: zyszero
 * @Date: 2024/8/9 0:15
 */
@ConfigurationProperties(prefix = "phoenix-dfs")
@Data
public class PhoenixDfsProperties {
    private String uploadPath;
    private String backupUrl;
    private String downloadUrl;
    private String group;
    private boolean autoMd5;
    private boolean syncBackup;
}
