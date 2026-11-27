package com.lockedin.services;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

import com.lockedin.api.dto.PersonalRecord;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/** Lays out the PDF progress report (docs/API.md, "GET /api/export/pdf"). Holds no data access. */
final class ProgressReport {

    /** A body weight reading as it appears in the report. */
    record Weight(double kg, LocalDate date) {
    }

    private static final String DASH = "—";
    private static final Color MUTED = new Color(0x6B, 0x72, 0x80);
    private static final Color RULE = new Color(0xE5, 0xE7, 0xEB);

    private static final Font TITLE = new Font(Font.HELVETICA, 20, Font.BOLD);
    private static final Font HEADING = new Font(Font.HELVETICA, 13, Font.BOLD);
    private static final Font LABEL = new Font(Font.HELVETICA, 10, Font.NORMAL, MUTED);
    private static final Font BODY = new Font(Font.HELVETICA, 10);
    private static final Font BOLD = new Font(Font.HELVETICA, 10, Font.BOLD);

    private ProgressReport() {
    }

    /** {@code firstWeight} and {@code latestWeight} are null when the user has no readings. */
    static byte[] render(String email, LocalDate generated, int totalSets, double totalVolume,
                         Weight firstWeight, Weight latestWeight, List<PersonalRecord> records) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 48, 48, 48, 48);
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph("LockedIn progress report", TITLE));

        PdfPTable summary = new PdfPTable(new float[] {1, 2});
        summary.setWidthPercentage(100);
        summary.setSpacingBefore(16);
        summaryRow(summary, "Email", email);
        summaryRow(summary, "Generated", generated.toString());
        summaryRow(summary, "Total sets", String.valueOf(totalSets));
        summaryRow(summary, "Total volume", kg(totalVolume));
        summaryRow(summary, "First body weight", weight(firstWeight));
        summaryRow(summary, "Latest body weight", weight(latestWeight));
        document.add(summary);

        Paragraph heading = new Paragraph("Personal records", HEADING);
        heading.setSpacingBefore(24);
        heading.setSpacingAfter(8);
        document.add(heading);

        if (records.isEmpty()) {
            document.add(new Paragraph("No personal records yet", LABEL));
        } else {
            document.add(recordsTable(records));
        }

        document.close();
        return out.toByteArray();
    }

    private static PdfPTable recordsTable(List<PersonalRecord> records) {
        PdfPTable table = new PdfPTable(new float[] {3, 1.4f, 1.4f, 1.6f, 1.6f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String header : List.of("Exercise", "Best weight", "Est. 1RM", "Best set volume", "Date")) {
            table.addCell(cell(header, BOLD, header.equals("Exercise") ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT));
        }
        for (PersonalRecord r : records) {
            table.addCell(cell(r.exerciseName(), BODY, Element.ALIGN_LEFT));
            table.addCell(cell(kg(r.bestWeight()), BODY, Element.ALIGN_RIGHT));
            table.addCell(cell(r.bestOneRepMax() + " kg", BODY, Element.ALIGN_RIGHT));
            table.addCell(cell(kg(r.bestVolume()), BODY, Element.ALIGN_RIGHT));
            table.addCell(cell(r.achievedAt().toString(), BODY, Element.ALIGN_RIGHT));
        }
        return table;
    }

    private static void summaryRow(PdfPTable table, String label, String value) {
        table.addCell(cell(label, LABEL, Element.ALIGN_LEFT));
        table.addCell(cell(value, BODY, Element.ALIGN_LEFT));
    }

    private static PdfPCell cell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(RULE);
        cell.setHorizontalAlignment(align);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(7);
        return cell;
    }

    private static String weight(Weight w) {
        return w == null ? DASH : kg(w.kg()) + " (" + w.date() + ")";
    }

    private static String kg(double value) {
        return ExportService.number(value) + " kg";
    }
}
