package at.qe.skeleton.services;

import at.qe.skeleton.models.Userx;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateBuilder emailTemplateBuilder;

    @Async
    @Override
    public void sendUserInvitationEmail(Userx user) {

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom("noreply.climatecanary@gmail.com");
            mailMessage.setTo(user.getEmail());
            mailMessage.setSubject("ClimateCanary Invitation");
            mailMessage.setText(emailTemplateBuilder.buildInvitationEmail(user));
            mailSender.send(mailMessage);
            log.info("EmailService | Invitation email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("EmailService | Failed to send invitation email to {}", user.getEmail(), e);
        }
    }
}
