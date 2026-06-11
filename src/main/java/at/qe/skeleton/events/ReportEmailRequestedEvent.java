package at.qe.skeleton.events;

public record ReportEmailRequestedEvent(
        String recipientEmail,
        byte[] pdfBytes,
        String filename
) {}
