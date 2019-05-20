package com.example.batchkafka.consumer;

import com.example.batchkafka.entities.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.kafka.KafkaItemReader;
import org.springframework.batch.item.kafka.builder.KafkaItemReaderBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Properties;

@EnableBatchProcessing
@SpringBootApplication
@RequiredArgsConstructor
@Log4j2
public class ConsumerApplicaton {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplicaton.class, args);
    }

    private final KafkaProperties properties;
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final KafkaTemplate<Long, Customer> template;

    @Bean
    Job job() {
        return this.jobBuilderFactory
                .get("job")
                .start(start())
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    KafkaItemReader<Long, Customer> kafkaItemReader(){
        var props = new Properties();
        props.putAll(this.properties.buildConsumerProperties());

        return new KafkaItemReaderBuilder<Long, Customer>()
                .partitions(0)
                .consumerProperties(props)
                .name("customers-reader")
                .saveState(true)
                .topic("customers")
                .build();
    }

    @Bean
    Step start() {

        var writer = new ItemWriter<Customer>() {
            @Override
            public void write(List<? extends Customer> items) throws Exception {
                items.forEach(customer -> log.info("new customer: "+customer.toString()));
            }
        };

        return this.stepBuilderFactory
                .get("step")
                .<Customer, Customer>chunk((10))
                .reader(kafkaItemReader())
                .writer(writer)
                .build();
    }

}
