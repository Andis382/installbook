package io.github.andis382.installbook.ai;

import io.github.andis382.installbook.files.FileStorage;
import io.github.andis382.installbook.files.StoredFile;
import io.github.andis382.installbook.units.UnitType;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores a photo of a unit's data plate and, when AI is configured, reads type, brand, model
 * and serial off it. The photo is kept either way: it is the proof of which unit went where.
 */
@Service
public class PlateReader {

    static final String INSTRUCTION = """
        This is a photo of the data plate (rating label) on a heating, cooling, hot-water, solar or alarm unit,
        taken by the installer who just fitted it.
        Read only what is printed on the plate:
        - type: one of BOILER, AIR_CONDITIONER, HEAT_PUMP, WATER_HEATER, SOLAR_INVERTER, ALARM_PANEL, OTHER,
          chosen from the printed product description; null if the plate does not say.
        - brand: the manufacturer name as printed (e.g. "Vaillant", "Daikin").
        - model: the model name or model code as printed.
        - serialNumber: the serial number exactly as printed, character by character, without spaces you do not see.
        - notes: anything the installer should double-check, in one short sentence (e.g. a character you are unsure of).
        Return null for any field that is missing, cut off, blurred or not readable.
        Never guess or complete serial number characters: a wrong serial is worse than none.
        """;

    private final FileStorage storage;
    private final AiExtractor ai;

    public PlateReader(FileStorage storage, AiExtractor ai) {
        this.storage = storage;
        this.ai = ai;
    }

    public record Result(StoredFile photo, boolean aiEnabled, PlateReading reading) {}

    /** Stores the photo; reads it only when asked and when AI is configured. */
    public Result read(Long organizationId, MultipartFile photo, boolean readIt) {
        StoredFile stored = storage.store(organizationId, photo, FileStorage.IMAGES);
        if (!readIt || !ai.enabled()) {
            return new Result(stored, ai.enabled(), null);
        }
        Optional<PlateReading> reading = ai.fromImage(storage.bytes(stored), stored.getContentType(), INSTRUCTION, PlateReading.class)
            .map(PlateReader::clean)
            .filter(PlateReader::hasAnything);
        return new Result(stored, true, reading.orElse(null));
    }

    /** Models answer "unknown", "N/A" or "" where the instruction asked for null; treat all of them as null. */
    static PlateReading clean(PlateReading raw) {
        UnitType type = UnitType.parse(value(raw.type()));
        String serial = value(raw.serialNumber());
        return new PlateReading(type == null ? null : type.name(), value(raw.brand()), value(raw.model()),
            serial == null ? null : serial.toUpperCase(), value(raw.notes()));
    }

    private static boolean hasAnything(PlateReading r) {
        return r.type() != null || r.brand() != null || r.model() != null || r.serialNumber() != null;
    }

    private static String value(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() || t.equalsIgnoreCase("null") || t.equalsIgnoreCase("unknown") || t.equalsIgnoreCase("n/a")
            ? null : t;
    }
}
