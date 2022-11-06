package com.icloud.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Branched;

import java.util.Properties;

import static org.apache.kafka.streams.StreamsConfig.*;

@Slf4j
public class MetricStreams {

    private static KafkaStreams streams;
    private static final String APPLICATION_ID = "metric-streams-application";
    private static final String BOOTSTRAP_SERVERS = "10.211.55.17:9092";
    private static final String METRIC_ALL_TOPIC_NAME = "metric.all";
    private static final String METRIC_CPU_TOPIC_NAME = "metric.cpu";
    private static final String METRIC_MEMORY_TOPIC_NAME = "metric.memory";
    private static final String METRIC_CPU_ALERT_TOPIC_NAME = "metric.cpu.alert";

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> streams.close()));
        var properties = createProperties();
        var builder = new StreamsBuilder();
        var metrics = builder.<String, String>stream(METRIC_ALL_TOPIC_NAME);
        metrics.split()
                .branch(MetricJsonUtils.getPredicateByResource("cpu"),
                        Branched.withFunction(c -> {
                            c.to(METRIC_CPU_TOPIC_NAME);
                            c.filter(MetricJsonUtils.getPredicateByTotalCPUUsage(0.5))
                                    .mapValues(MetricJsonUtils::getHostTimestamp)
                                    .to(METRIC_CPU_ALERT_TOPIC_NAME);
                            return c;
                        }))
                .branch(MetricJsonUtils.getPredicateByResource("memory"),
                        Branched.withConsumer(c -> c.to(METRIC_MEMORY_TOPIC_NAME)));

        streams = new KafkaStreams(builder.build(), properties);
        streams.start();
    }


    private static Properties createProperties() {
        var properties = new Properties();
        properties.put(APPLICATION_ID_CONFIG, APPLICATION_ID);
        properties.put(BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        return properties;
    }

}
