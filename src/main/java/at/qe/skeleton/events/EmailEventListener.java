package at.qe.skeleton.events;

import at.qe.skeleton.services.EmailService;
import at.qe.skeleton.services.EmailType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventListener {

    private final EmailService emailService;

    @Async
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        emailService.sendEmail(EmailType.USER_INVITATION, event.user());
        log.info("Handled UserCreatedEvent for {}", event.user().getEmail());
    }

    @Async
    @EventListener
    public void handleUserDeleted(UserDeletedEvent event) {
        emailService.sendEmail(EmailType.USER_DELETED, event.user());
        log.info("Handled UserDeletedEvent for {}", event.user().getEmail());
    }

    @Async
    @EventListener
    public void onReportEmailRequested(ReportEmailRequestedEvent event) {
        emailService.sendReportEmail(event.recipientEmail(), event.pdfBytes(), event.filename());
    }
}