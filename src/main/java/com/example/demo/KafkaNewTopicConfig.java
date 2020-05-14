package com.example.demo;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaNewTopicConfig {


	@Value("${kafka.topic}")
	private String TOPIC_NAME;

	@Bean
	public NewTopic mySpringKafkaMessageTopic() {
	  return TopicBuilder.name(TOPIC_NAME)
	    .partitions(1)
	    .replicas(1)
	    .compact()
	    .build();
	}
}
