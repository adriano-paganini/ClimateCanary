package at.qe.skeleton.services;

import at.qe.skeleton.models.Userx;

/**
 * Service interface for sending system emails to users.
 * <p>
 * Supports both:
 * - transactional user notification emails (invites, account updates, etc.)
 * - report emails with PDF attachments
 */
public interface EmailService {

    /**
     * Sends a predefined system email to a user based on email type.
     *
     * @param type type of email template to send
     * @param user recipient user
     */
    void sendEmail(EmailType type, Userx user);

    /**
     * Sends a report email with a PDF attachment.
     *
     * @param recipientEmail target email address
     * @param pdfBytes report file content
     * @param filename attachment filename
     */
    void sendReportEmail(String recipientEmail, byte[] pdfBytes, String filename);
}
