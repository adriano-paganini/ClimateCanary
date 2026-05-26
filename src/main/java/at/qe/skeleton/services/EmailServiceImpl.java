package at.qe.skeleton.services;

import at.qe.skeleton.models.Userx;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateBuilder emailTemplateBuilder;

    @Value("${app.emailServiceEnabled}")
    private boolean emailServiceEnabled;

    @Async
    @Override
    public void sendUserInvitationEmail(Userx user) {

        if (!emailServiceEnabled) {
            log.info("EmailService disabled - skipping email to {}", user.getEmail());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom("noreply.climatecanary@gmail.com");
            helper.setTo(user.getEmail());
            helper.setSubject("ClimateCanary Invitation");
            helper.setText(emailTemplateBuilder.buildInvitationEmail(user), true);
            mailSender.send(message);
            log.info("EmailService | Invitation email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("EmailService | Failed to send invitation email to {}", user.getEmail(), e);
        }
    }
}
