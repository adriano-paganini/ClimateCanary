package at.qe.skeleton.services;

import at.qe.skeleton.models.Userx;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {

    public String buildInvitationEmail(Userx user) {

        return """
                Hello %s,

                You have been invited to join ClimateCanary.

                Your account has been created by an administrator.

                Please use the following link to access the platform:
                <Link>

                This is an automatically generated email.
                Please do not reply.

                If you received this email in error, please ignore it.
                
                Kind regards,
                G5T4 - Software Engineering
                University of Innsbruck
                
                DISCLAIMER: This is a mock email created solely for a University 
                Software Engineering project at the University of Innsbruck. 
                This email and its contents do not represent a real company, 
                organization, or commercial entity. If you have received this 
                email unintentionally, please disregard it.
                """.formatted(user.getUsername());
    }
}

