package com.flixmate.core.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.flixmate.core.model.Booking;
import com.flixmate.core.model.BookedSeat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PDFGeneratorService {

    private final QRGeneratorService qrGeneratorService;

    public byte[] generateTicketPDF(Booking booking, List<BookedSeat> seats) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try (Document document = new Document()) {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts configuration
            Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, new Color(229, 9, 20)); // Red logo color
            Font headerFont = new Font(Font.HELVETICA, 14, Font.BOLD, Color.BLACK);
            Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
            Font boldBodyFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);

            // 1. Title
            Paragraph platformTitle = new Paragraph("FLIXMATE E-TICKET", titleFont);
            platformTitle.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(platformTitle);
            document.add(new Paragraph(" ")); // Spacer

            // 2. Movie Information
            document.add(new Paragraph("Movie details:", headerFont));
            document.add(new Paragraph("Movie: " + booking.getShowtime().getMovie().getTitle(), boldBodyFont));
            document.add(new Paragraph("Theater: " + booking.getShowtime().getScreen().getTheater().getName(), bodyFont));
            document.add(new Paragraph("Screen: " + booking.getShowtime().getScreen().getName(), bodyFont));
            document.add(new Paragraph("Time: " + booking.getShowtime().getStartTime().toString(), bodyFont));
            document.add(new Paragraph(" ")); // Spacer

            // 3. Seating & Price details
            document.add(new Paragraph("Reservation Details:", headerFont));
            document.add(new Paragraph("Booking ID: " + booking.getId().toString(), bodyFont));
            
            StringBuilder seatLabels = new StringBuilder();
            for (BookedSeat bs : seats) {
                seatLabels.append(bs.getSeat().getRowName()).append("-").append(bs.getSeat().getSeatNumber()).append(" ");
            }
            document.add(new Paragraph("Seats booked: " + seatLabels.toString(), boldBodyFont));
            document.add(new Paragraph("Total Price: $" + booking.getTotalPrice().toString(), boldBodyFont));
            document.add(new Paragraph("Status: CONFIRMED", bodyFont));
            document.add(new Paragraph(" ")); // Spacer

            // 4. Embedded QR code
            document.add(new Paragraph("Scan at Gate:", headerFont));
            String qrBase64 = qrGeneratorService.generateQRCodeBase64(booking.getId().toString(), 150, 150);
            if (qrBase64 != null) {
                byte[] imgBytes = Base64.getDecoder().decode(qrBase64);
                Image qrImage = Image.getInstance(imgBytes);
                qrImage.setAlignment(Image.ALIGN_LEFT);
                document.add(qrImage);
            }

            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Thank you for choosing FlixMate! Enjoy your movie.", bodyFont);
            footer.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(footer);

        } catch (Exception e) {
            log.error("Error creating ticket PDF.", e);
        }

        return out.toByteArray();
    }
}
