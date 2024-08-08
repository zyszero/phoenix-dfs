package io.github.zyszero.phoenix.dfs;

import io.github.zyszero.phoenix.dfs.config.PhoenixDfsProperties;
import io.github.zyszero.phoenix.dfs.utils.FileUtils;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({RocketMQAutoConfiguration.class})
public class PhoenixDfsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhoenixDfsApplication.class, args);
    }


    // 1. 基于文件存储的分布式文件系统
    // 2. 块存储 ==> 最常见，效率最高 ==> 改造成这个。
    // 3. 对象存储
    @Autowired
    private PhoenixDfsProperties properties;

    @Bean
    ApplicationRunner runner() {
        return args -> {
            FileUtils.init(properties.getUploadPath());
            System.out.println("Phoenix DFS started...");
        };
    }
}
