package cz.inqool.arclib;

import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.jms.ConnectionFactory;

@Service
public class Coordinator {

    private JmsTemplate template;

    @Bean
    public JmsListenerContainerFactory<?> myFactory(ConnectionFactory connectionFactory,
                                                    DefaultJmsListenerContainerFactoryConfigurer configurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        return factory;
    }

    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    public void run(Batch batch) {
        batch.getIds().forEach(id -> {
            System.out.println("Sending a message from coordinator.");
            template.convertAndSend("process", new Dto(id, batch.getId()));
        });
    }

    public void cancel(String batchId) {
    }

    public void suspend(String batchId) {
    }

    @Inject
    public void setTemplate(JmsTemplate template) {
        this.template = template;
    }
}
