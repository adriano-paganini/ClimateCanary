package at.qe.skeleton.services;

import at.qe.skeleton.models.Userx;

public interface EmailService {

    void sendUserInvitationEmail(Userx user);
}
