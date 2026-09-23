package io.github.andis382.installbook.cards;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import io.github.andis382.installbook.common.Texts;
import io.github.andis382.installbook.notify.MessageTexts;
import io.github.andis382.installbook.units.ServiceSchedule;
import io.github.andis382.installbook.units.Unit;
import io.github.andis382.installbook.visits.ServiceVisit;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The one-page installation and warranty certificate a customer can download from the card.
 * Built-in PDF fonts (Helvetica, Courier) in Windows-1252, which covers ë and ç.
 */
@Component
public class CertificatePdf {

    private static final Color INK = new Color(0x0b, 0x22, 0x30);
    private static final Color PETROL = new Color(0x1b, 0x6f, 0x99);
    private static final Color COPPER = new Color(0xd9, 0x82, 0x2b);
    private static final Color MUTED = new Color(0x5a, 0x6b, 0x78);
    private static final Color PLATE = new Color(0xec, 0xf1, 0xf4);
    private static final Color RULE = new Color(0xd5, 0xde, 0xe4);
    private static final Color OK_BG = new Color(0xe3, 0xf4, 0xea);
    private static final Color OK_TEXT = new Color(0x13, 0x6c, 0x45);
    private static final Color OFF_BG = new Color(0xf6, 0xe9, 0xe7);
    private static final Color OFF_TEXT = new Color(0x9e, 0x2f, 0x25);
    /** Keeps the certificate on one page; the live card has the full history. */
    private static final int MAX_HISTORY_ROWS = 5;
    private static final float MARGIN = 54;

    private final Texts texts;
    private final MessageTexts messageTexts;

    public CertificatePdf(Texts texts, MessageTexts messageTexts) {
        this.texts = texts;
        this.messageTexts = messageTexts;
    }

    /** The business the certificate is issued by, and who fitted the unit. */
    public record Issuer(String name, String phoneDisplay, String installerName, String link) {}

    public byte[] render(Unit unit, List<ServiceVisit> visits, Issuer issuer, String locale, LocalDate today) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, MARGIN, MARGIN, 92, 48);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.addTitle(t(locale, "pdf.title"));
            doc.addAuthor(issuer.name());
            doc.open();
            drawFrame(writer.getDirectContentUnder(), doc.getPageSize());
            header(doc, unit, issuer, locale);
            unitBlock(doc, unit, locale);
            facts(doc, unit, locale, today);
            history(doc, visits, locale);
            signature(doc, writer, issuer, locale, unit.getInstalledOn().getYear());
            footer(doc, issuer, locale);
            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not build the certificate", e);
        }
        return out.toByteArray();
    }

    /** Double frame and a band of fine interlaced waves along the top: the look of a printed certificate. */
    private static void drawFrame(PdfContentByte canvas, Rectangle page) {
        float w = page.getWidth();
        float h = page.getHeight();
        canvas.saveState();
        canvas.setColorStroke(PETROL);
        canvas.setLineWidth(1.4f);
        canvas.roundRectangle(22, 22, w - 44, h - 44, 10);
        canvas.stroke();
        canvas.setColorStroke(COPPER);
        canvas.setLineWidth(0.6f);
        canvas.roundRectangle(28, 28, w - 56, h - 56, 7);
        canvas.stroke();

        PdfGState faint = new PdfGState();
        faint.setStrokeOpacity(0.28f);
        canvas.setGState(faint);
        canvas.setColorStroke(PETROL);
        canvas.setLineWidth(0.35f);
        for (int k = 0; k < 14; k++) {
            float base = h - 66 + k * 1.3f;
            double phase = k * 0.42;
            canvas.moveTo(36, base);
            for (float x = 36; x <= w - 36; x += 2.5f) {
                canvas.lineTo(x, (float) (base + 6 * Math.sin(x / 21.0 + phase) + 2.5 * Math.sin(x / 7.5 - phase)));
            }
            canvas.stroke();
        }
        canvas.restoreState();
    }

    private void header(Document doc, Unit unit, Issuer issuer, String locale) throws DocumentException {
        PdfPTable top = table(new float[] {3, 1.4f});
        PdfPCell who = cell();
        who.addElement(new Paragraph(issuer.name(), font(FontFactory.HELVETICA_BOLD, 17, INK)));
        if (!issuer.phoneDisplay().isBlank()) {
            who.addElement(new Paragraph(t(locale, "pdf.phone") + "   " + issuer.phoneDisplay(), font(FontFactory.HELVETICA, 10, MUTED)));
        }
        top.addCell(who);
        PdfPCell card = cell();
        Paragraph label = new Paragraph(t(locale, "pdf.card_number").toUpperCase(), font(FontFactory.HELVETICA_BOLD, 7.5f, MUTED));
        label.setAlignment(Element.ALIGN_RIGHT);
        Paragraph number = new Paragraph(cardNumber(unit), font(FontFactory.COURIER_BOLD, 13, PETROL));
        number.setAlignment(Element.ALIGN_RIGHT);
        card.addElement(label);
        card.addElement(number);
        top.addCell(card);
        doc.add(top);

        Paragraph title = new Paragraph(t(locale, "pdf.title"), font(FontFactory.HELVETICA_BOLD, 23, INK));
        title.setSpacingBefore(30);
        doc.add(title);
        PdfPTable rule = table(new float[] {1});
        PdfPCell line = cell();
        line.setFixedHeight(3);
        line.setBackgroundColor(COPPER);
        rule.setWidthPercentage(18);
        rule.setHorizontalAlignment(Element.ALIGN_LEFT);
        rule.addCell(line);
        rule.setSpacingBefore(8);
        doc.add(rule);
    }

    private void unitBlock(Document doc, Unit unit, String locale) throws DocumentException {
        String name = unit.getBrand() + (unit.getModel() == null ? "" : " " + unit.getModel());
        Paragraph type = new Paragraph(t(locale, "unit.type." + unit.getType().name()).toUpperCase(),
            font(FontFactory.HELVETICA_BOLD, 8.5f, COPPER));
        type.setSpacingBefore(20);
        doc.add(type);
        doc.add(new Paragraph(name, font(FontFactory.HELVETICA_BOLD, 16, INK)));

        PdfPTable plate = table(new float[] {1});
        plate.setWidthPercentage(62);
        plate.setHorizontalAlignment(Element.ALIGN_LEFT);
        plate.setSpacingBefore(10);
        PdfPCell p = new PdfPCell();
        p.setBackgroundColor(PLATE);
        p.setBorderColor(RULE);
        p.setBorderWidth(0.8f);
        p.setPadding(9);
        p.setPaddingLeft(12);
        p.addElement(new Paragraph(t(locale, "pdf.serial").toUpperCase(), font(FontFactory.HELVETICA_BOLD, 7, MUTED)));
        String serial = unit.getSerialNumber() == null ? "—" : unit.getSerialNumber();
        p.addElement(new Paragraph(serial, font(FontFactory.COURIER_BOLD, 14, INK)));
        plate.addCell(p);
        doc.add(plate);
    }

    private void facts(Document doc, Unit unit, String locale, LocalDate today) throws DocumentException {
        boolean active = ServiceSchedule.warrantyActive(unit.getWarrantyUntil(), today);
        PdfPTable grid = table(new float[] {1, 1});
        grid.setSpacingBefore(18);
        grid.addCell(fact(t(locale, "pdf.customer"), unit.getCustomer().getName()));
        grid.addCell(fact(t(locale, "pdf.address"), unit.getAddress()));
        grid.addCell(fact(t(locale, "pdf.installed_on"), messageTexts.day(unit.getInstalledOn(), locale)));
        grid.addCell(fact(t(locale, "pdf.warranty"), months(locale, unit.getWarrantyMonths())));
        PdfPCell until = fact(t(locale, "pdf.warranty_until"), messageTexts.day(unit.getWarrantyUntil(), locale));
        Chunk pill = new Chunk("  " + t(locale, active ? "pdf.active" : "pdf.expired") + "  ",
            font(FontFactory.HELVETICA_BOLD, 8, active ? OK_TEXT : OFF_TEXT));
        pill.setBackground(active ? OK_BG : OFF_BG, 1, 2, 1, 2);
        Paragraph status = new Paragraph();
        status.add(pill);
        status.setSpacingBefore(4);
        until.addElement(status);
        grid.addCell(until);
        grid.addCell(fact(t(locale, "pdf.service_every"), months(locale, unit.getServiceIntervalMonths())));
        grid.addCell(fact(t(locale, "pdf.last_service"),
            unit.getLastServiceOn() == null ? t(locale, "pdf.never") : messageTexts.day(unit.getLastServiceOn(), locale)));
        grid.addCell(fact(t(locale, "pdf.next_service"),
            unit.isActive() ? messageTexts.day(unit.getNextServiceDue(), locale) : "—"));
        doc.add(grid);
        if (!unit.isActive() && unit.getRemovedOn() != null) {
            Paragraph removed = new Paragraph(t(locale, "pdf.removed").replace("{date}", messageTexts.day(unit.getRemovedOn(), locale)),
                font(FontFactory.HELVETICA_BOLD, 10, OFF_TEXT));
            removed.setSpacingBefore(8);
            doc.add(removed);
        }
    }

    private void history(Document doc, List<ServiceVisit> visits, String locale) throws DocumentException {
        Paragraph heading = new Paragraph(t(locale, "pdf.history"), font(FontFactory.HELVETICA_BOLD, 12, INK));
        heading.setSpacingBefore(18);
        heading.setSpacingAfter(6);
        doc.add(heading);
        if (visits.isEmpty()) {
            doc.add(new Paragraph(t(locale, "pdf.no_history"), font(FontFactory.HELVETICA, 10, MUTED)));
            return;
        }
        PdfPTable rows = table(new float[] {1.2f, 1.5f, 3.3f});
        for (ServiceVisit v : visits.stream().limit(MAX_HISTORY_ROWS).toList()) {
            rows.addCell(row(messageTexts.day(v.getVisitedOn(), locale), FontFactory.HELVETICA_BOLD));
            rows.addCell(row(t(locale, "visit.kind." + v.getKind().name()), FontFactory.HELVETICA));
            rows.addCell(row(v.getParts() == null ? "" : v.getParts(), FontFactory.HELVETICA));
        }
        doc.add(rows);
        if (visits.size() > MAX_HISTORY_ROWS) {
            Paragraph more = new Paragraph(t(locale, "pdf.more_visits").replace("{count}", String.valueOf(visits.size() - MAX_HISTORY_ROWS)),
                font(FontFactory.HELVETICA, 9, MUTED));
            more.setSpacingBefore(4);
            doc.add(more);
        }
    }

    /** "Installed by" with a signature line, and a round stamp beside it. */
    private void signature(Document doc, PdfWriter writer, Issuer issuer, String locale, int year) throws DocumentException {
        PdfPTable block = table(new float[] {1.3f, 1});
        block.setSpacingBefore(26);
        PdfPCell left = cell();
        left.addElement(new Paragraph(t(locale, "pdf.installed_by").toUpperCase(), font(FontFactory.HELVETICA_BOLD, 7, MUTED)));
        Paragraph who = new Paragraph(issuer.installerName() == null ? issuer.name() : issuer.installerName() + ", " + issuer.name(),
            font(FontFactory.HELVETICA, 11, INK));
        who.setSpacingBefore(2);
        left.addElement(who);
        PdfPTable line = table(new float[] {1});
        line.setWidthPercentage(80);
        line.setHorizontalAlignment(Element.ALIGN_LEFT);
        line.setSpacingBefore(30);
        PdfPCell sign = new PdfPCell(new Phrase(t(locale, "pdf.signature"), font(FontFactory.HELVETICA, 7.5f, MUTED)));
        sign.setBorder(Rectangle.TOP);
        sign.setBorderColor(MUTED);
        sign.setBorderWidth(0.6f);
        sign.setPaddingTop(4);
        line.addCell(sign);
        left.addElement(line);
        block.addCell(left);
        block.addCell(cell());
        doc.add(block);
        float bottom = writer.getVerticalPosition(true);
        stamp(writer.getDirectContent(), doc.getPageSize().getWidth() - MARGIN - 70, bottom + 42, issuer.name(), year);
    }

    private static void stamp(PdfContentByte canvas, float cx, float cy, String business, int year) {
        canvas.saveState();
        PdfGState ink = new PdfGState();
        ink.setStrokeOpacity(0.7f);
        ink.setFillOpacity(0.7f);
        canvas.setGState(ink);
        canvas.setColorStroke(PETROL);
        canvas.setColorFill(PETROL);
        canvas.setLineWidth(1.6f);
        canvas.circle(cx, cy, 38);
        canvas.stroke();
        canvas.setLineWidth(0.6f);
        canvas.circle(cx, cy, 33);
        canvas.stroke();
        canvas.setLineWidth(2.2f);
        canvas.moveTo(cx - 10, cy + 6);
        canvas.lineTo(cx - 3, cy - 1);
        canvas.lineTo(cx + 11, cy + 13);
        canvas.stroke();
        try {
            BaseFont bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            canvas.beginText();
            canvas.setFontAndSize(bold, fitSize(bold, business.toUpperCase(), 54, 7.5f));
            canvas.showTextAligned(Element.ALIGN_CENTER, business.toUpperCase(), cx, cy - 13, 0);
            canvas.setFontAndSize(bold, 7);
            canvas.showTextAligned(Element.ALIGN_CENTER, String.valueOf(year), cx, cy - 22, 0);
            canvas.endText();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (DocumentException e) {
            throw new IllegalStateException(e);
        }
        canvas.restoreState();
    }

    private static float fitSize(BaseFont font, String text, float width, float max) {
        float size = max;
        while (size > 4 && font.getWidthPoint(text, size) > width) {
            size -= 0.25f;
        }
        return size;
    }

    private void footer(Document doc, Issuer issuer, String locale) throws DocumentException {
        Paragraph foot = new Paragraph(t(locale, "pdf.footer").replace("{business}", issuer.name()).replace("{link}", issuer.link()),
            font(FontFactory.HELVETICA, 8.5f, MUTED));
        foot.setSpacingBefore(22);
        doc.add(foot);
    }

    private static PdfPCell fact(String label, String value) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.TOP);
        c.setBorderColor(RULE);
        c.setBorderWidth(0.8f);
        c.setPaddingTop(7);
        c.setPaddingBottom(9);
        c.setPaddingRight(14);
        c.addElement(new Paragraph(label.toUpperCase(), font(FontFactory.HELVETICA_BOLD, 7, MUTED)));
        Paragraph v = new Paragraph(value, font(FontFactory.HELVETICA, 11, INK));
        v.setSpacingBefore(2);
        c.addElement(v);
        return c;
    }

    private static PdfPCell row(String text, String fontName) {
        PdfPCell c = new PdfPCell(new Phrase(text, font(fontName, 9.5f, INK)));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(RULE);
        c.setPaddingTop(5);
        c.setPaddingBottom(6);
        c.setPaddingRight(10);
        return c;
    }

    private static PdfPTable table(float[] widths) {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        return table;
    }

    private static PdfPCell cell() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(0);
        return c;
    }

    private static Font font(String name, float size, Color color) {
        return FontFactory.getFont(name, BaseFont.CP1252, BaseFont.NOT_EMBEDDED, size, Font.NORMAL, color);
    }

    private String months(String locale, int months) {
        return t(locale, "pdf.months").replace("{months}", String.valueOf(months));
    }

    private String t(String locale, String key) {
        return texts.in(locale, key);
    }

    /**
     * A short reference to quote on the phone. Derived from the card token (stable, not
     * sequential, reveals nothing about the register) but not the token itself.
     */
    public static String cardNumber(Unit unit) {
        String code = Integer.toUnsignedString(unit.getCardToken().hashCode(), 36).toUpperCase();
        return "IB-" + "0".repeat(Math.max(0, 7 - code.length())) + code;
    }
}
