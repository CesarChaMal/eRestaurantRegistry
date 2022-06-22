package com.erestaurant.registry.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.testcontainers.containers.KafkaContainer;

public class TestContainersSpringContextCustomizerFactory implements ContextCustomizerFactory {

    private Logger log = LoggerFactory.getLogger(TestContainersSpringContextCustomizerFactory.class);
    private static KafkaTestContainer kafkaBean;

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
        return (context, mergedConfig) -> {
            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
            TestPropertyValues testValues = TestPropertyValues.empty();

            EmbeddedKafka kafkaAnnotation = AnnotatedElementUtils.findMergedAnnotation(testClass, EmbeddedKafka.class);
            if (null != kafkaAnnotation) {
                log.debug("detected the EmbeddedKafka annotation on class {}", testClass.getName());
                if (kafkaBean == null) {
                    log.info("Warming up the kafka broker");
                    kafkaBean = new KafkaTestContainer();
                    beanFactory.initializeBean(kafkaBean, KafkaTestContainer.class.getName().toLowerCase());
                    beanFactory.registerSingleton(KafkaTestContainer.class.getName().toLowerCase(), kafkaBean);
                    ((DefaultSingletonBeanRegistry) beanFactory).registerDisposableBean(
                            KafkaTestContainer.class.getName().toLowerCase(),
                            kafkaBean
                        );
                }
                testValues =
                    testValues.and(
                        "spring.cloud.stream.kafka.binder.brokers=" +
                        kafkaBean.getKafkaContainer().getHost() +
                        ':' +
                        kafkaBean.getKafkaContainer().getMappedPort(KafkaContainer.KAFKA_PORT)
                    );
            }
            testValues.applyTo(context);
        };
    }
}
