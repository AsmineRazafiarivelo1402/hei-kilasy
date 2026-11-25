package hei.school;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class SmsNotificationServiceTest {

    @Test
    void sendsFormattedGradeToValidPhone() {
        AtomicReference<SmsMessage> sentMessage = new AtomicReference<>();
        SmsNotificationService service = new SmsNotificationService(sentMessage::set);

        service.notifyGrade("+261 34 00 000 01", "Tri à bulles", new BigDecimal("14.4"));

        assertEquals("+261 34 00 000 01", sentMessage.get().recipient());
        assertEquals("Votre note pour « Tri à bulles » est disponible : 14,4/20.",
                sentMessage.get().content());
    }

    @Test
    void doesNotAttemptToSendToMissingOrInvalidPhone() {
        SmsSender sender = org.mockito.Mockito.mock(SmsSender.class);
        SmsNotificationService service = new SmsNotificationService(sender);

        service.notifyGrade(null, "Tri à bulles", new BigDecimal("14.4"));
        service.notifyGrade("abc", "Tri à bulles", new BigDecimal("14.4"));

        verify(sender, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotPropagateSmsProviderFailure() {
        SmsNotificationService service = new SmsNotificationService(message -> {
            throw new IllegalStateException("SMS provider unavailable");
        });

        assertDoesNotThrow(() ->
                service.notifyGrade("+261 34 00 000 01", "Tri à bulles", new BigDecimal("14.4")));
    }
}
