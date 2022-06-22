package com.erestaurant.registry.web.rest;

import com.erestaurant.registry.config.KafkaSseConsumer;
import com.erestaurant.registry.config.KafkaSseProducer;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@RestController
@RequestMapping("/api/e-restaurant-registry-kafka")
public class ERestaurantRegistryKafkaResource {

    private final Logger log = LoggerFactory.getLogger(ERestaurantRegistryKafkaResource.class);

    private final MessageChannel output;
    private Sinks.Many<Message<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

    public ERestaurantRegistryKafkaResource(@Qualifier(KafkaSseProducer.CHANNELNAME) MessageChannel output) {
        this.output = output;
    }

    @PostMapping("/publish")
    public Mono<ResponseEntity<Void>> publish(@RequestParam String message) {
        log.debug("REST request the message : {} to send to Kafka topic", message);
        Map<String, Object> map = new HashMap<>();
        map.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE);
        MessageHeaders headers = new MessageHeaders(map);
        output.send(new GenericMessage<>(message, headers));
        return Mono.just(ResponseEntity.noContent().build());
    }

    @GetMapping("/consume")
    public Flux<String> consume() {
        log.debug("REST request to consume records from Kafka topics");
        return sink.asFlux().map(m -> m.getPayload());
    }

    @StreamListener(value = KafkaSseConsumer.CHANNELNAME, copyHeaders = "false")
    public void consume(Message<String> message) {
        log.debug("Got message from kafka stream: {}", message.getPayload());
        sink.emitNext(message, Sinks.EmitFailureHandler.FAIL_FAST);
    }
}
