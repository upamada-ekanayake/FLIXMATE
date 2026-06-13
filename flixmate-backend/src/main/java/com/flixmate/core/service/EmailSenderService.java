package com.flixmate.core.service;

import com.flixmate.core.model.Booking;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailSenderService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void sendBookingConfirmationEmail(String toEmail, Booking booking, byte[] pdfAttachment) {
        log.info("Preparing booking confirmation email for {} (Booking ID: {})", toEmail, booking.getId());

        if (mailSender == null) {
            log.warn("JavaMailSender is not configured. E-mail confirmation skipped. Local logger backup - Email content summary: User={}, Movie={}, Total={}",
                    toEmail, booking.getShowtime().getMovie().getTitle(), booking.getTotalPrice());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("Your FlixMate Booking Confirmation - Ticket #" + booking.getId().toString().substring(0, 8));
            
            String htmlBody = String.format(
                    "<h3>Thank you for booking with FlixMate!</h3>" +
                    "<p>Your tickets have been confirmed. Below are your booking details:</p>" +
                    "<ul>" +
                    "<li><strong>Movie:</strong> %s</li>" +
                    "<li><strong>Time:</strong> %s</li>" +
                    "<li><strong>Total Price:</strong> $%s</li>" +
                    "</ul>" +
                    "<p>Please present the attached PDF ticket at the cinema gate.</p>" +
                    "<br/>" +
                    "<p>Best regards,<br/>The FlixMate Team</p>",
                    booking.getShowtime().getMovie().getTitle(),
                    booking.getShowtime().getStartTime().toString(),
                    booking.getTotalPrice()
            );
            
            helper.setText(htmlBody, true);

            if (pdfAttachment != null) {
                helper.addAttachment("FlixMate_Ticket_" + booking.getId().toString().substring(0, 8) + ".pdf",
                        new ByteArrayResource(pdfAttachment));
            }

            mailSender.send(message);
            log.info("Confirmation email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.warn("SMTP email dispatch failed (Check credentials in application.yml). Local logger backup - Email content summary: User={}, Movie={}, Total={}", 
                    toEmail, booking.getShowtime().getMovie().getTitle(), booking.getTotalPrice());
        }
    }
}
