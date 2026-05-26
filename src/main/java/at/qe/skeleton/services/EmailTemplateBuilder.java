package at.qe.skeleton.services;

import at.qe.skeleton.models.Userx;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {

    private String buildFooter(){
        return """
                <hr>

                    <p style="font-size: 13px; color: gray;">
                        This is an automatically generated email. Please do not reply.
                    </p>

                    <p>
                        Kind regards,<br>
                        <strong>G5T4 - Software Engineering</strong><br>
                        University of Innsbruck
                    </p>

                    <hr>

                    <p style="font-size: 11px; color: #777;">
                        DISCLAIMER: This is a mock email created solely for a University 
                        Software Engineering project at the University of Innsbruck. 
                        This email and its contents do not represent a real company, 
                        organization, or commercial entity.
                    </p>
                """;
    }

    public String buildInvitationEmail(Userx user) {

        return """
                <div style="font-family: Arial, sans-serif; line-height: 1.6;">
                
                    <h2>ClimateCanary Invitation</h2>

                    <p>Hello <strong>%s</strong>,</p>

                    <p>You have been invited to join <strong>ClimateCanary</strong>.</p>

                    <p>Your account has been created by an administrator.</p>

                    <p>Please use the following link to access the platform:</p>

                    <p>
                        <a href="<Link>">...<Link></a>
                    </p>
                    
                    %s
         
                </div>
                """.formatted(user.getUsername(), buildFooter());
    }
}

