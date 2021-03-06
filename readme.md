# spring kafka


## Classes de configuration

ici nous creer 3 classes de configuration pour le **consumer** et le **producer** et **newTopic** 

Dans ce projet nous utilsons les callbacks pour recuperer le message du retour de kafka

**KafkaConsumerConfig**  

Configure les parametres du consumer en lieu et place du fichier application.properties puis renvoi un objet **ConcurrentKafkaListenerContainerFactory**.

```java

@Configuration
public class KafkaConsumerConfig {

	@Value("${kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Bean
	public Map<String, Object> consumerConfigs() {

		Map<String, Object> configProps = new HashMap<String, Object>();
		// list of host:port pairs used for establishing the initial connections to the Kafka cluster
		configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		// allows a pool of processes to divide the work of consuming and processing  records
		configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "group-id");

		return configProps;
	}

	@Bean
	public ConsumerFactory<String, String> consumerFactory() {
		return new DefaultKafkaConsumerFactory<String, String>(consumerConfigs());
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> concurrentKafkaListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
		factory.setConsumerFactory(consumerFactory());
		return factory;
	}
}

```



**KafkaProducerConfig**  

Configure les parametres du producer en lieu et place du fichier application.properties puis renvoi un objet **KafkaTemplate**.

```java

	  @Value("${kafka.bootstrap-servers}")
	  private String bootstrapServers;
	  
	  @Bean
	  public Map<String, Object> producerConfigs() {

			Map<String, Object> configProps = new HashMap<String, Object>();
			configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG , bootstrapServers );
			configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG , StringSerializer.class);
			configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG , StringSerializer.class);

	    return configProps;
	  }
	  
	  
	@Bean
	public ProducerFactory<String, String> producerFactory(){
		return new DefaultKafkaProducerFactory<String, String>(producerConfigs());
	}
	
	@Bean
	public KafkaTemplate<String, String> kafkaTemplate(){
		return new KafkaTemplate<String, String>(producerFactory());
	}
	
```

**newTopic**

```java
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
```


## Creation d'un restcontroller, consumer et un producer


**restcontroller**  

```java

@RestController
@RequestMapping(value = "/kafka")
public class KafkaController {

	@Autowired
	private MyProducer myProducer;

	
	@GetMapping("/simple/{message}")
	public String sendMessage(@PathVariable String message){
	
		ListenableFuture<SendResult<String, String>> future = myProducer.sendMessageString(message);
		
		return "Messages sent";
	}
	
	
	@GetMapping(value = "/publish")
	public String sendMessageToKafkaTopic(@RequestParam("message") String message) {
		ListenableFuture<SendResult<String, String>> future = myProducer.sendMessageValue(message);

		future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

			@Override
			public void onSuccess(SendResult<String, String> result) {
				System.out.println("----------------------------------------------");
				System.out.println("offset: " + result.getRecordMetadata().offset() + " topic: "
						+ result.getRecordMetadata().topic() + " partition: " + result.getRecordMetadata().partition()
						+ " key: " + result.getProducerRecord().key() + " value : "
						+ result.getProducerRecord().value());
			}

			@Override
			public void onFailure(Throwable ex) {
				ex.printStackTrace();
			}

		});
		
		return "Messages sent";
	}

	@GetMapping("/sendMessages/{counter}")
	public String sendMessages(@PathVariable int counter) {

		for (int i = 0; i < counter; i++) {
			ListenableFuture<SendResult<String, String>> future = myProducer.sendMessageKeyValue(i);

			future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

				@Override
				public void onSuccess(SendResult<String, String> result) {
					System.out.println("offset: " + result.getRecordMetadata().offset() + " topic: "
							+ result.getRecordMetadata().topic() + " partition: "
							+ result.getRecordMetadata().partition() + " key: " + result.getProducerRecord().key()
							+ " value : " + result.getProducerRecord().value());
				}

				@Override
				public void onFailure(Throwable ex) {
					ex.printStackTrace();
				}

			});

		}

		return "Messages sent";

	}
}
```

**consumer**

```java
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
```

**producer**

```java
@Service
public class MyProducer {

	private static final Logger logger = LoggerFactory.getLogger(MyProducer.class);
	//private static final String TOPIC_NAME = "users";
	
	
	  @Value("${kafka.topic}")
	  private  String TOPIC_NAME;
	 
	
	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;
	
	public ListenableFuture<SendResult<String, String>> sendMessageString(String message) {
	 return kafkaTemplate.send(TOPIC_NAME,String.valueOf(Math.random()*1000),message);
	}

	
	public  ListenableFuture<SendResult<String, String>> sendMessageValue(String message) {
		logger.info(String.format("$$ -> Producing message --> %s", message));
		return kafkaTemplate.send(new ProducerRecord<String, String>(TOPIC_NAME, message,message));
	}
		
	
	public ListenableFuture<SendResult<String, String>> sendMessageKeyValue(int counter) {
		logger.info(String.format("$$ -> Producing message --> %s", counter));
		//kafkaTemplate.send(TOPIC_NAME,message);
		return kafkaTemplate.send(new ProducerRecord<String, String>(TOPIC_NAME, Integer.toString(counter), Integer.toString(counter)));
		
	}		
}

```




## Démarrage de zookepeer
```shell
λ .\bin\windows\zookeeper-server-start.bat  .\config\zookeeper.properties
```

## Démarrage de server kafka

```shell
λ .\bin\windows\kafka-server-start.bat .\config\server.properties
```

## test

**postman**  
![](doc/testKafka.jpg)

 
**curl command line**
 
```shell
λ curl -d "message=Bonjour tout le monde" -H "Content-Type: application/x-www-form-urlencoded" -X POST http://localhost:8080/kafka/publish
```


