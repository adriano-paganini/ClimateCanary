package at.qe.skeleton.services;

import at.qe.skeleton.helper.EmailTemplateBuilder;
import at.qe.skeleton.models.Userx;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final String FROM_ADDRESS = "noreply.climatecanary@gmail.com";

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

            helper.setFrom(FROM_ADDRESS);
            helper.setTo(user.getEmail());
            helper.setSubject(getSubject(type));
            helper.setText(emailTemplateBuilder.buildEmail(type, user), true);

            mailSender.send(message);
            log.info("EmailService | {} email sent to {}", type, user.getEmail());

        } catch (Exception e) {
            log.error("EmailService | Failed sending {} email to {}", type, user.getEmail(), e);
        }
    }

    @Async
    @Override
    @PreAuthorize("hasAuthority('BUILDING_ADMIN')")
    public void sendReportEmail(String recipientEmail, byte[] pdfBytes, String filename) {
        if (!emailServiceEnabled) {
            log.info("EmailService disabled - skipping report email to {}", recipientEmail);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(FROM_ADDRESS);
            helper.setTo(recipientEmail);
            helper.setSubject("ClimateCanary — Room Climate Report");
            helper.setText(emailTemplateBuilder.buildReportEmail(), true);
            helper.addAttachment(filename, new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            log.info("EmailService | REPORT_PDF sent with attachment '{}' to {}", filename, recipientEmail);

        } catch (Exception e) {
            log.error("EmailService | Failed sending REPORT_PDF to {}", recipientEmail, e);
        }
    }

    private String getSubject(EmailType type) {
        return switch (type) {
            case USER_INVITATION -> "ClimateCanary Invitation";
            case USER_DELETED -> "ClimateCanary Account Notice";
            case REPORT_PDF -> "ClimateCanary — Room Climate Report";
        };
    }
}