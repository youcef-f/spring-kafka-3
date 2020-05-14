package com.example.demo;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MyConsumer {

	/*
	  @Value("${kafka.topic}")
	  private  String TOPIC_NAME;
	 */

	private static final Logger logger = LoggerFactory.getLogger(MyConsumer.class);
	private static final String TOPIC_NAME = "mytopic";

	//@KafkaListener(groupId = "mykafkagroup", topics = TOPIC_NAME, properties = { "enable.auto.commit=true", "auto.commit.interval.ms=1000", "poll-interval=100"})
	@KafkaListener(topics = TOPIC_NAME, groupId = "group_id")
	public void consumer(ConsumerRecord<?,?> record) {
		logger.info(String.format("$$ -> Consuming message --> %s", record));
		  System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());

	}
}
