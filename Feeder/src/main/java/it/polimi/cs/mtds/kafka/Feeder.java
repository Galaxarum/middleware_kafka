package it.polimi.cs.mtds.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class Feeder {

	public static void main(String[] args) throws IOException {
		final Properties properties = new Properties();
		final String propFile = args.length>0?args[0]:"config.properties";
		final InputStreamReader propertiesIn = new InputStreamReader(new FileInputStream(propFile), StandardCharsets.UTF_8);
		try {
			properties.load(propertiesIn);
		}catch ( IOException e ){ throw new IOException("Cannot read property file",e); }
		properties.put("key.serializer", StringSerializer.class);
		properties.put("key.deserializer", StringDeserializer.class);
		properties.put("value.serializer", StringSerializer.class);
		properties.put("value.deserializer", StringDeserializer.class);
		final KafkaProducer<String,String> producer = new KafkaProducer<>(properties);
		final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		final DateTimeFormatter clock = DateTimeFormatter.ofPattern("HH:mm:ss");
		int key=0;
		String cmd;
		do{
			cmd=in.readLine();
			final ProducerRecord<String,String> record = new ProducerRecord<>("topic_1", String.valueOf(key), cmd);
			key++;
			producer.send(record);
			System.out.println(clock.format(LocalDateTime.now())+": sent '"+cmd+"'. type 'end' to quit");
		}while ( !"end".equals(cmd) );
	}
	
}
