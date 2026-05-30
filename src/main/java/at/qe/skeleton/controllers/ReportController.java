package at.qe.skeleton.controllers;

import at.qe.skeleton.events.ReportEmailRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final long MAX_PDF_BYTES = 20 * 1024 * 1024; // 20 MB
    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/send")
    public ResponseEntity<Void> sendReport(
            @RequestParam("to")         String to,
            @RequestParam("attachment") MultipartFile attachment) {

        String recipient = to == null ? "" : to.trim();
        if (!EMAIL_PATTERN.matcher(recipient).matches()) {
            log.warn("ReportController | Invalid recipient address: '{}'", recipient);
            return ResponseEntity.badRequest().build();
        }

        if (attachment == null || attachment.isEmpty()) {
            log.warn("ReportController | Empty PDF attachment for recipient={}", recipient);
            return ResponseEntity.badRequest().build();
        }

        if (attachment.getSize() > MAX_PDF_BYTES) {
            log.warn("ReportController | PDF too large ({} bytes) for recipient={}", attachment.getSize(), recipient);
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        byte[] pdfBytes;
        try {
            pdfBytes = attachment.getBytes();
        } catch (IOException e) {
            log.error("ReportController | Failed to read PDF attachment for recipient={}", recipient, e);
            return ResponseEntity.internalServerError().build();
        }

        String filename = "room-climate-report-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"))
                + ".pdf";

        log.info("ReportController | Publishing ReportEmailRequestedEvent → recipient={}, file={}, {}B",
                recipient, filename, pdfBytes.length);

        log.info("Publishing report email event");

        eventPublisher.publishEvent(
                new ReportEmailRequestedEvent(to, pdfBytes, filename)
        );

        log.info("Published report email event");

        return ResponseEntity.accepted().build();
    }
}
