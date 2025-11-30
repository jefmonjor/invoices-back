package com.invoices.invoice.infrastructure.services;

import com.invoices.invoice.dto.BatchSummary;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Service for sending emails (batch summaries, notifications, etc.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService {

    private final JavaMailSender mailSender;

    @Value("${verifactu.email.from:noreply@invoices.com}")
    private String fromEmail;

    @Value("${verifactu.email.to:admin@invoices.com}")
    private String toEmail;

    @Value("${verifactu.email.enabled:false}")
    private boolean emailEnabled;

    /**
     * Sends a batch summary email after VERI*FACTU batch processing.
     *
     * @param summary The batch processing summary
     */
    public void sendBatchSummaryEmail(BatchSummary summary) {
        if (!emailEnabled) {
            log.debug("Email is disabled, skipping batch summary email");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("VERI*FACTU Batch Report - %s",
                    summary.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));

            String htmlContent = buildEmailHtml(summary);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Batch summary email sent successfully to {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send batch summary email", e);
        }
    }

    /**
     * Builds the HTML content for the batch summary email.
     */
    private String buildEmailHtml(BatchSummary summary) {
        String statusClass = summary.getSuccessRate() > 80 ? "success"
                : summary.getSuccessRate() > 50 ? "warning" : "error";

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            background-color: #f5f5f5;
                            padding: 20px;
                        }
                        .container {
                            max-width: 600px;
                            margin: 0 auto;
                            background-color: white;
                            border-radius: 8px;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                            overflow: hidden;
                        }
                        .header {
                            background-color: #2c3e50;
                            color: white;
                            padding: 20px;
                            text-align: center;
                        }
                        .summary {
                            padding: 20px;
                        }
                        .metric {
                            margin: 15px 0;
                            padding: 12px;
                            background-color: #f8f9fa;
                            border-left: 4px solid #3498db;
                            border-radius: 4px;
                        }
                        .label {
                            font-weight: bold;
                            color: #333;
                            display: inline-block;
                            min-width: 200px;
                        }
                        .value {
                            color: #2980b9;
                            font-size: 1.1em;
                            font-weight: bold;
                        }
                        .success { color: #27ae60; }
                        .warning { color: #f39c12; }
                        .error { color: #e74c3c; }
                        .footer {
                            padding: 15px 20px;
                            background-color: #ecf0f1;
                            text-align: center;
                            color: #7f8c8d;
                            font-size: 0.9em;
                        }
                        .alert {
                            padding: 15px;
                            margin: 15px 0;
                            border-radius: 4px;
                            background-color: #fff3cd;
                            border-left: 4px solid #ffc107;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>📊 Resumen Batch VERI*FACTU</h2>
                            <p>%s</p>
                        </div>

                        <div class="summary">
                            <div class="metric">
                                <span class="label">Total procesadas:</span>
                                <span class="value">%d</span>
                            </div>
                            <div class="metric">
                                <span class="label">Exitosas:</span>
                                <span class="value success">%d</span>
                            </div>
                            <div class="metric">
                                <span class="label">Fallidas:</span>
                                <span class="value error">%d</span>
                            </div>
                            <div class="metric">
                                <span class="label">Tasa de éxito:</span>
                                <span class="value %s">%.2f%%</span>
                            </div>

                            %s
                        </div>

                        <div class="footer">
                            <p>Este email fue generado automáticamente por el sistema VERI*FACTU.</p>
                            <p>Para más información, acceda al panel de administración.</p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                summary.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                summary.getTotalProcessed(),
                summary.getSuccessful(),
                summary.getFailed(),
                statusClass,
                summary.getSuccessRate(),
                summary.getCriticalPending() > 0 ? String.format(
                        "<div class=\"alert\"><strong>⚠️ ATENCIÓN:</strong> Hay %d facturas pendientes críticas (>48h)</div>",
                        summary.getCriticalPending()) : "");
    }
}
