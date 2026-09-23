package io.github.andis382.installbook.demo;

import io.github.andis382.installbook.units.UnitType;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * Draws a photo-like picture of a unit's data plate (brushed metal label, rivets, rating
 * table, barcode) so the demo register's unit pages have something real-looking to show.
 */
final class PlateImages {

    private static final int W = 720;
    private static final int H = 470;

    private PlateImages() {}

    static byte[] jpeg(UnitType type, String brand, String model, String serial, LocalDate made, long seed) {
        Random r = new Random(seed);
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        casing(g, r);
        AffineTransform base = g.getTransform();
        g.rotate(Math.toRadians(r.nextDouble() * 5 - 2.5), W / 2.0, H / 2.0);
        plate(g, r, type, brand, model, serial, made);
        g.setTransform(base);
        glare(g, r);
        g.dispose();
        return encode(img);
    }

    /** The painted steel the label is stuck on, lit from one side. */
    private static void casing(Graphics2D g, Random r) {
        int tone = 222 + r.nextInt(18);
        g.setPaint(new GradientPaint(0, 0, new Color(tone, tone, tone - 4), W, H, new Color(tone - 38, tone - 36, tone - 40)));
        g.fillRect(0, 0, W, H);
        for (int i = 0; i < 2600; i++) {
            int v = r.nextInt(30);
            g.setColor(new Color(v, v, v, 10));
            g.fillRect(r.nextInt(W), r.nextInt(H), 2, 2);
        }
    }

    private static void plate(Graphics2D g, Random r, UnitType type, String brand, String model, String serial, LocalDate made) {
        int px = 80;
        int py = 62;
        int pw = W - 160;
        int ph = H - 124;
        RoundRectangle2D shape = new RoundRectangle2D.Double(px, py, pw, ph, 18, 18);

        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new RoundRectangle2D.Double(px + 5, py + 7, pw, ph, 18, 18));
        g.setPaint(new LinearGradientPaint(px, py, px + pw, py + ph, new float[] {0f, 0.45f, 0.55f, 1f},
            new Color[] {new Color(226, 230, 233), new Color(190, 197, 202), new Color(214, 219, 223), new Color(174, 181, 187)}));
        g.fill(shape);
        g.setClip(shape);
        for (int y = py; y < py + ph; y += 2) {
            g.setColor(new Color(255, 255, 255, 8 + r.nextInt(22)));
            g.drawLine(px, y, px + pw, y + r.nextInt(3) - 1);
        }
        g.setClip(null);
        g.setColor(new Color(92, 101, 108));
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(px + 10, py + 10, pw - 20, ph - 20, 10, 10));
        for (int[] c : new int[][] {{px + 22, py + 22}, {px + pw - 22, py + 22}, {px + 22, py + ph - 22}, {px + pw - 22, py + ph - 22}}) {
            rivet(g, c[0], c[1]);
        }

        Color ink = new Color(28, 34, 40);
        int x = px + 44;
        g.setColor(ink);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        g.drawString(brand, x, py + 66);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        g.setColor(new Color(58, 66, 74));
        g.drawString(description(type), x, py + 90);

        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(70, 78, 86));
        g.drawLine(x, py + 104, px + pw - 44, py + 104);

        List<String[]> rows = List.of(
            new String[] {"Model / Type", model},
            new String[] {"Serial No.", serial},
            new String[] {"Mfg.", made.getYear() + "/" + String.format("%02d", made.getMonthValue())},
            new String[] {"Rating", rating(type)},
            new String[] {"Supply", supply(type)});
        int y = py + 132;
        for (String[] row : rows) {
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            g.setColor(new Color(64, 72, 80));
            g.drawString(row[0], x, y);
            boolean isSerial = row[0].startsWith("Serial");
            g.setFont(isSerial ? new Font(Font.MONOSPACED, Font.BOLD, 19) : new Font(Font.SANS_SERIF, Font.BOLD, 16));
            g.setColor(ink);
            g.drawString(row[1], x + 128, y);
            y += 34;
        }

        barcode(g, r, px + pw - 190, py + 262, 146, 36);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        g.setColor(ink);
        g.drawString("CE", px + pw - 96, py + 136);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.drawString(ipRating(type), px + pw - 96, py + 156);
    }

    private static void rivet(Graphics2D g, int cx, int cy) {
        float radius = 7f;
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx - 2, cy - 2), radius,
            new float[] {0f, 1f}, new Color[] {new Color(245, 246, 247), new Color(120, 128, 134)}));
        g.fill(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2));
        g.setColor(new Color(80, 86, 92));
        g.draw(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2));
    }

    private static void barcode(Graphics2D g, Random r, int x, int y, int w, int h) {
        g.setColor(new Color(24, 28, 32));
        int cursor = x;
        while (cursor < x + w) {
            int bar = 1 + r.nextInt(3);
            if (r.nextBoolean()) {
                g.fillRect(cursor, y, bar, h);
            }
            cursor += bar + 1;
        }
    }

    /** A soft diagonal reflection, as a phone flash leaves on a metal label. */
    private static void glare(Graphics2D g, Random r) {
        int gx = 120 + r.nextInt(360);
        g.setPaint(new GradientPaint(gx, 0, new Color(255, 255, 255, 0), gx + 140, H, new Color(255, 255, 255, 46)));
        g.fillPolygon(new int[] {gx, gx + 90, gx + 250, gx + 160}, new int[] {0, 0, H, H}, 4);
    }

    private static String description(UnitType type) {
        return switch (type) {
            case BOILER -> "Condensing gas boiler";
            case AIR_CONDITIONER -> "Split system air conditioner, indoor unit";
            case HEAT_PUMP -> "Air to water heat pump";
            case WATER_HEATER -> "Electric storage water heater";
            case SOLAR_INVERTER -> "Grid-tied photovoltaic inverter";
            case ALARM_PANEL -> "Security control panel";
            case OTHER -> "Circulator pump";
        };
    }

    private static String rating(UnitType type) {
        return switch (type) {
            case BOILER -> "Pn 80/60 °C  24 kW   PMS 3 bar";
            case AIR_CONDITIONER -> "Cool 3.5 kW  Heat 4.0 kW  R32 0.75 kg";
            case HEAT_PUMP -> "A7/W35 6.0 kW   R32 1.30 kg";
            case WATER_HEATER -> "80 L   1500 W   8 bar";
            case SOLAR_INVERTER -> "AC 5000 W   DC 600 V max";
            case ALARM_PANEL -> "Grade 2   EN 50131";
            case OTHER -> "5 - 45 W   PN 10";
        };
    }

    private static String supply(UnitType type) {
        return switch (type) {
            case ALARM_PANEL -> "12 V DC  1.5 A";
            case SOLAR_INVERTER -> "230 V ~ 50 Hz  grid";
            default -> "230 V ~ 50 Hz";
        };
    }

    private static String ipRating(UnitType type) {
        return switch (type) {
            case BOILER, OTHER -> "IPX5D";
            case SOLAR_INVERTER -> "IP65";
            case ALARM_PANEL -> "IP20";
            default -> "IPX4";
        };
    }

    private static byte[] encode(BufferedImage img) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(out)) {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.82f);
            writer.setOutput(stream);
            writer.write(null, new IIOImage(img, null, null), param);
            writer.dispose();
            stream.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
