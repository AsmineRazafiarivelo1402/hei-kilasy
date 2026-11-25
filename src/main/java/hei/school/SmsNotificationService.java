package hei.school;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationService.class);
    private final SmsSender smsSender;

    public SmsNotificationService(SmsSender smsSender) {
        this.smsSender = smsSender;
    }

    public void notifyGrade(String phone, String exerciseTitle, BigDecimal finalGrade) {
        if (!isValidPhone(phone)) {
            logger.warn("SMS non envoyé : numéro de téléphone absent ou invalide");
            return;
        }

        String safeTitle = exerciseTitle.replace('\r', ' ').replace('\n', ' ');
        String formattedGrade = finalGrade.toPlainString().replace('.', ',');
        SmsMessage message = new SmsMessage(phone.trim(),
                "Votre note pour « " + safeTitle + " » est disponible : " + formattedGrade + "/20.");
        try {
            smsSender.send(message);
        } catch (RuntimeException ex) {
            logger.warn("Échec de l'envoi du SMS au numéro {}", phone.trim(), ex);
        }
    }

    private boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }

        String normalized = phone.replaceAll("[\\s()-]", "");
        return normalized.matches("\\+?[0-9]{7,15}");
    }
}
