/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.resources.crd;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.KafkaTopicList;
import io.strimzi.api.kafka.model.DoneableKafkaTopic;
import io.strimzi.api.kafka.model.KafkaTopic;
import io.strimzi.api.kafka.model.KafkaTopicBuilder;
import io.strimzi.systemtest.utils.kafkaUtils.KafkaTopicUtils;
import io.strimzi.test.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.strimzi.systemtest.resources.ResourceManager;

import java.util.function.Consumer;

public class KafkaTopicResource {
    private static final Logger LOGGER = LogManager.getLogger(KafkaTopicResource.class);

    public static final String PATH_TO_KAFKA_TOPIC_CONFIG = "../examples/topic/kafka-topic.yaml";

    public static MixedOperation<KafkaTopic, KafkaTopicList, DoneableKafkaTopic, Resource<KafkaTopic, DoneableKafkaTopic>> kafkaTopicClient() {
        return Crds.topicOperation(ResourceManager.kubeClient().getClient());
    }

    public static DoneableKafkaTopic topic(String clusterName, String topicName) {
        return topic(defaultTopic(clusterName, topicName, 1, 1, 1).build());
    }

    public static DoneableKafkaTopic topic(String clusterName, String topicName, int partitions) {
        return topic(defaultTopic(clusterName, topicName, partitions, 1, 1).build());
    }

    public static DoneableKafkaTopic topic(String clusterName, String topicName, int partitions, int replicas) {
        return topic(defaultTopic(clusterName, topicName, partitions, replicas, replicas).build());
    }

    public static DoneableKafkaTopic topic(String clusterName, String topicName, int partitions, int replicas, int minIsr) {
        return topic(defaultTopic(clusterName, topicName, partitions, replicas, minIsr).build());
    }

    public static KafkaTopicBuilder defaultTopic(String clusterName, String topicName, int partitions, int replicas, int minIsr) {
        KafkaTopic kafkaTopic = getKafkaTopicFromYaml(PATH_TO_KAFKA_TOPIC_CONFIG);
        return new KafkaTopicBuilder(kafkaTopic)
            .withNewMetadata()
                .withName(topicName)
                .withNamespace(ResourceManager.kubeClient().getNamespace())
                .addToLabels("strimzi.io/cluster", clusterName)
            .endMetadata()
            .editSpec()
                .withPartitions(partitions)
                .withReplicas(replicas)
                .addToConfig("min.insync.replicas", minIsr)
            .endSpec();
    }

    static DoneableKafkaTopic topic(KafkaTopic topic) {
        return new DoneableKafkaTopic(topic, kt -> {
            kafkaTopicClient().inNamespace(topic.getMetadata().getNamespace()).createOrReplace(kt);
            LOGGER.info("Created KafkaTopic {}", kt.getMetadata().getName());
            return waitFor(deleteLater(kt));
        });
    }

    public static KafkaTopic topicWithoutWait(KafkaTopic kafkaTopic) {
        kafkaTopicClient().inNamespace(ResourceManager.kubeClient().getNamespace()).createOrReplace(kafkaTopic);
        return kafkaTopic;
    }

    private static KafkaTopic getKafkaTopicFromYaml(String yamlPath) {
        return TestUtils.configFromYaml(yamlPath, KafkaTopic.class);
    }

    private static KafkaTopic waitFor(KafkaTopic kafkaTopic) {
        LOGGER.info("Waiting for Kafka Topic {}", kafkaTopic.getMetadata().getName());
        KafkaTopicUtils.waitForKafkaTopicCreation(kafkaTopic.getMetadata().getName());
        LOGGER.info("Kafka Topic {} is ready", kafkaTopic.getMetadata().getName());
        return kafkaTopic;
    }

    private static KafkaTopic deleteLater(KafkaTopic kafkaTopic) {
        return ResourceManager.deleteLater(kafkaTopicClient(), kafkaTopic);
    }

    public static void replaceTopicResource(String resourceName, Consumer<KafkaTopic> editor) {
        ResourceManager.replaceCrdResource(KafkaTopic.class, KafkaTopicList.class, DoneableKafkaTopic.class, resourceName, editor);
    }
}
