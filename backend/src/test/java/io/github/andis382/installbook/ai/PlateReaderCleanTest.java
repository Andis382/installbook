package io.github.andis382.installbook.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlateReaderCleanTest {

    @Test
    void turnsTheModelsWaysOfSayingNothingIntoNull() {
        PlateReading cleaned = PlateReader.clean(new PlateReading("air conditioner", " Daikin ", "unknown", "", "N/A"));
        assertThat(cleaned.type()).isEqualTo("AIR_CONDITIONER");
        assertThat(cleaned.brand()).isEqualTo("Daikin");
        assertThat(cleaned.model()).isNull();
        assertThat(cleaned.serialNumber()).isNull();
        assertThat(cleaned.notes()).isNull();
    }

    @Test
    void keepsTheSerialAsPrintedButInCapitals() {
        PlateReading cleaned = PlateReader.clean(new PlateReading("BOILER", "Vaillant", "ecoTEC plus", "21223300100n7", null));
        assertThat(cleaned.serialNumber()).isEqualTo("21223300100N7");
        assertThat(cleaned.type()).isEqualTo("BOILER");
    }

    @Test
    void anUnknownTypeIsDroppedRatherThanGuessed() {
        assertThat(PlateReader.clean(new PlateReading("fridge", "LG", null, null, null)).type()).isNull();
    }
}
