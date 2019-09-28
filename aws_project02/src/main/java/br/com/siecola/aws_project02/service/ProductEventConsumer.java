package br.com.siecola.aws_project02.service;

import br.com.siecola.aws_project02.model.Envelope;
import br.com.siecola.aws_project02.model.ProductEvent;
import br.com.siecola.aws_project02.model.ProductEventLog;
import br.com.siecola.aws_project02.model.SnsMessage;
import br.com.siecola.aws_project02.repository.ProductEventLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Service
public class ProductEventConsumer {
  private static final Logger log = LoggerFactory.getLogger(
          ProductEventConsumer.class);
  private ObjectMapper objectMapper;
  private ProductEventLogRepository productEventLogRepository;

  @Autowired
  public ProductEventConsumer(ObjectMapper objectMapper,
                              ProductEventLogRepository productEventLogRepository) {
    this.objectMapper = objectMapper;
    this.productEventLogRepository = productEventLogRepository;
  }

  @JmsListener(destination = "${aws.sqs.queue.product.events.name}")
  public void receiveProductEvent(TextMessage textMessage)
          throws JMSException, IOException {

    SnsMessage snsMessage = objectMapper.readValue(textMessage.getText(),
            SnsMessage.class);
    Envelope envelope = objectMapper.readValue(snsMessage.getMessage(),
            Envelope.class);
    ProductEvent productEvent = objectMapper.readValue(
            envelope.getData(), ProductEvent.class);
    ProductEventLog productEventLog = buildProductEventLog(envelope,
            productEvent);
    productEventLogRepository.save(productEventLog);
    log.info("Product event received - Event: {} - ProductId: {} - " +
                    "MessageId: {}", envelope.getEventType(),
            productEvent.getProductId(), snsMessage.getMessageId());
  }

  ProductEventLog buildProductEventLog(Envelope envelope,
                                       ProductEvent productEvent) {
    ProductEventLog productEventLog = new ProductEventLog();
    productEventLog.setEventType(envelope.getEventType());
    productEventLog.setProductId(productEvent.getProductId());
    productEventLog.setCode(productEvent.getCode());
    productEventLog.setUsername(productEvent.getUsername());
    productEventLog.setTimestamp(Instant.now().toEpochMilli());
    productEventLog.setTtl(Instant.now().plus(
            Duration.ofMinutes(10)).toEpochMilli());
    return productEventLog;
  }
}
