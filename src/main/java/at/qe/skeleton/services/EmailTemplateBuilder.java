package at.qe.skeleton.services;

import at.qe.skeleton.models.Userx;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {

    public String buildEmail(EmailType type, Userx user) {
        return switch (type) {
            case USER_INVITATION -> buildInvitationEmail(user);
            case USER_DELETED -> buildAccountDeletedEmail(user);
        };
    }

    private String buildInvitationEmail(Userx user) {

        String body = """
                <h2>ClimateCanary Invitation</h2>

                <p>Hello <strong>%s</strong>,</p>

                <p>You have been invited to join ClimateCanary.</p>

                <p>Please use the following link:</p>

                <p><a href="<Link>"><Link></a></p>
                """.formatted(user.getUsername());

        return wrap(body);
    }

    private String buildAccountDeletedEmail(Userx user) {

        String body = """
                <h2>Account Deletion</h2>

                <p>Hello <strong>%s</strong>,</p>

                <p>This is to inform you that your account has been successfully deleted. You will no longer have
                access to the platform and its services.</p>
                """.formatted(user.getUsername());

        return wrap(body);
    }

    private String wrap(String body) {
        return """
                <div style="font-family: Arial, sans-serif; line-height: 1.6;">
                    %s
                    %s
                </div>
                """.formatted(body, buildFooter());
    }

    private String buildFooter() {
        return """
                <hr>
                <p style="font-size: 11px; color: gray;">
                    This is an automatically generated email.
                </p>
                <p>
                    Kind regards,<br>
                    <strong>G5T4 - Software Engineering</strong><br>
                    University of Innsbruck
                </p>
                <hr>
                <p style="font-size: 11px; color: #777;">
                    DISCLAIMER: This is a mock email created solely for a Software Engineering project at 
                    the University of Innsbruck. This email and its contents do not represent a real company, 
                    organization, or commercial entity.
                </p>
                """;
    }
}