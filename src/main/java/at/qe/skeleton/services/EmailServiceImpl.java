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
    public void sendEmail(EmailType type, Userx user) {

        if (!emailServiceEnabled) {
            log.info("EmailService disabled - skipping email {}", type);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom("noreply.climatecanary@gmail.com");
            helper.setTo(user.getEmail());
            helper.setSubject(getSubject(type));
            helper.setText(emailTemplateBuilder.buildEmail(type, user), true);

            mailSender.send(message);

            log.info("EmailService | {} email sent to {}", type, user.getEmail());

        } catch (Exception e) {
            log.error("EmailService | Failed sending {} email to {}", type, user.getEmail(), e);
        }
    }

    private String getSubject(EmailType type) {
        return switch (type) {
            case USER_INVITATION -> "ClimateCanary Invitation";
            case USER_DELETED -> "ClimateCanary Account Notice";
        };
    }
}