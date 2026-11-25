package hei.school;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulatedSmsSender implements SmsSender {

    private static final Logger logger = LoggerFactory.getLogger(SimulatedSmsSender.class);

    @Override
    public void send(SmsMessage message) {
        logger.info("[SMS] {} : {}", message.recipient(), message.content());
    }
}
