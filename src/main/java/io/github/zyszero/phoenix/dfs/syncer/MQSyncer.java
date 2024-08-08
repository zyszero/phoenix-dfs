package io.github.zyszero.phoenix.dfs.syncer;

import com.alibaba.fastjson.JSON;
import io.github.zyszero.phoenix.dfs.config.PhoenixDfsProperties;
import io.github.zyszero.phoenix.dfs.meta.FileMeta;
import io.github.zyszero.phoenix.dfs.utils.FileUtils;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * mq syncer
 *
 * @Author: zyszero
 * @Date: 2024/8/8 20:41
 */
@Component
public class MQSyncer {
    private final static String TOPIC = "phoenix-dfs";


    @Autowired
    private PhoenixDfsProperties properties;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;


    public void sync(FileMeta meta) {
        Message<String> message = MessageBuilder
                .withPayload(JSON.toJSONString(meta))
                .build();
        rocketMQTemplate.send(TOPIC, message);
        System.out.println(" ===> send topic/message: " + TOPIC + "/" + message);
    }

    @Service
    @RocketMQMessageListener(topic = "phoenix-dfs", consumerGroup = "${phoenix-dfs.group}")
    public class FileMQSyncer implements RocketMQListener<MessageExt> {
        @Override
        public void onMessage(MessageExt message) {
            // 1. 从消息里获取 meta 数据
            System.out.println(" ==> onMessage ID = " + message.getMsgId());
            String json = new String(message.getBody());
            System.out.println(" ==> message json = " + json);
            FileMeta meta = JSON.parseObject(json, FileMeta.class);
            String downloadUrl = meta.getDownloadUrl();
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                System.out.println(" ==> downloadUrl is empty.");
                return;
            }

            // 去重本机操作
            if (properties.getDownloadUrl().equals(downloadUrl)) {
                System.out.println(" ==> the same file server, ignore sync task.");
                return;
            }
            System.out.println(" ==> the other file server, process mq sync task.");


            // 2. 写 meta 文件
            String dir = properties.getUploadPath() + "/" + FileUtils.getSubDir(meta.getName());
            File metaFile = new File(dir, meta.getName() + ".meta");
            if (metaFile.exists()) {
                System.out.println(" ==> meta file exists and ignore save: " + metaFile.getAbsolutePath());
            } else {
                System.out.println(" ==> meta file save: " + metaFile.getAbsolutePath());
                FileUtils.writeString(metaFile, json);
            }

            // 3. 下载文件
            File file = new File(dir, meta.getName());
            if (file.exists() && file.length() == meta.getSize()) {
                System.out.println(" ==> file exists and ignore download: " + file.getAbsolutePath());
            }

            downloadUrl = downloadUrl + "?name=" + file.getName();
            FileUtils.download(downloadUrl, file);
        }

    }
}
