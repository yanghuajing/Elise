package site.zido.elise.distributed;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import site.zido.elise.task.DefaultTask;
import site.zido.elise.task.Task;
import site.zido.elise.distributed.pojo.Seed;
import site.zido.elise.downloader.Downloader;
import site.zido.elise.http.Request;
import site.zido.elise.processor.ResponseHandler;
import site.zido.elise.scheduler.AbstractScheduler;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Kafka Communication Manager
 *
 * @author zido
 */
public class SpringKafkaTaskScheduler extends AbstractScheduler {
    private String bootstrapServers;
    private String groupId = "Elise";
    private String topicAnalyzer = "__analyzer__";
    private String topicDownload = "__download__";

    private KafkaTemplate<Long, Seed> template;


    private KafkaMessageListenerContainer<Long, Seed> analyzerContainer;
    private KafkaMessageListenerContainer<Long, Seed> downloaderContainer;

    public SpringKafkaTaskScheduler setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
        this.template = createTemplate();
        return this;
    }

    public SpringKafkaTaskScheduler setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public SpringKafkaTaskScheduler setTopicAnalyzer(String topicAnalyzer) {
        this.topicAnalyzer = topicAnalyzer;
        return this;
    }

    public SpringKafkaTaskScheduler setTopicDownload(String topicDownload) {
        this.topicDownload = topicDownload;
        return this;
    }

    @Override
    public synchronized void setResponseHandler(ResponseHandler responseHandler) {
//        super.setResponseHandler(responseHandler);
        if (this.analyzerContainer == null) {
            this.analyzerContainer = runContainer(topicAnalyzer, message -> {
                Seed seed = message.value();
                super.onProcess(seed.getTask(), seed.getRequest(), seed.getResponse());
            });
        }
        if (!this.analyzerContainer.isRunning()) {
            this.analyzerContainer.start();
        }
    }

    public synchronized void setDownloader(Downloader downloader) {
        //super.setDownloader(downloader);
        if (this.downloaderContainer == null) {
            this.downloaderContainer = runContainer(topicDownload, message -> {
                Seed seed = message.value();
                super.onDownload(seed.getTask(), seed.getRequest());
            });
        }
        if (!this.downloaderContainer.isRunning()) {
            this.downloaderContainer.start();
        }
    }

    private KafkaMessageListenerContainer<Long, Seed> runContainer(String topic, MessageListener<Long, Seed> listener) {
        ContainerProperties containerProps = new ContainerProperties(topic);
        containerProps.setMessageListener(listener);
        KafkaMessageListenerContainer<Long, Seed> tmp = createContainer(containerProps);
        tmp.setBeanName(topic + "message-listener");
        return tmp;
    }

    @Override
    protected void pushWhenNoDuplicate(Task task, Request request) {
        template.send(topicDownload, new Seed().setTask((DefaultTask) task).setRequest(request));
    }

    private KafkaMessageListenerContainer<Long, Seed> createContainer(
            ContainerProperties containerProps) {
        Map<String, Object> props = consumerProps();
        DefaultKafkaConsumerFactory<Long, Seed> cf =
                new DefaultKafkaConsumerFactory<>(props);
        return new KafkaMessageListenerContainer<>(cf, containerProps);
    }

    private KafkaTemplate<Long, Seed> createTemplate() {
        Map<String, Object> senderProps = senderProps();
        ProducerFactory<Long, Seed> pf =
                new DefaultKafkaProducerFactory<>(senderProps);
        return new KafkaTemplate<>(pf, true);
    }

    private Map<String, Object> consumerProps() {
        if (bootstrapServers == null) {
            throw new IllegalArgumentException("bootstrapServers can not be null");
        }
        Map<String, Object> props = new HashMap<>(7);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "100");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "15000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "site.zido.elise.distributed.pojo");
        return props;
    }

    private Map<String, Object> senderProps() {
        if (bootstrapServers == null) {
            throw new IllegalArgumentException("bootstrapServers can not be null");
        }
        Map<String, Object> props = new HashMap<>(7);
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        return props;
    }

    @Override
    public void cancel(boolean ifRunning) {
        if (this.analyzerContainer != null && this.analyzerContainer.isRunning()) {
            this.analyzerContainer.stop();
        }
        if (this.downloaderContainer != null && this.downloaderContainer.isRunning()) {
            this.downloaderContainer.stop();
        }
    }
}
