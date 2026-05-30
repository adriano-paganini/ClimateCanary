package at.qe.skeleton.services;

import at.qe.skeleton.models.Userx;

public interface EmailService {

    void sendEmail(EmailType type, Userx user);
    void sendReportEmail(String recipientEmail, byte[] pdfBytes, String filename);

}
