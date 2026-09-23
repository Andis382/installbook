package io.github.andis382.installbook.units;

import io.github.andis382.installbook.customers.Customer;
import io.github.andis382.installbook.messaging.MessagesController.MessageView;
import io.github.andis382.installbook.units.ServiceSchedule.ServiceState;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class UnitDtos {

    private UnitDtos() {}

    /** Everything captured at the door, in one request. */
    public record RecordInstallRequest(
        @NotNull UnitType type,
        @NotBlank @Size(max = 80) String brand,
        @Size(max = 120) String model,
        @Size(max = 80) String serialNumber,
        @Size(max = 36) String platePhotoId,
        @NotBlank @Size(max = 40) String customerPhone,
        @NotBlank @Size(max = 160) String customerName,
        @Pattern(regexp = "en|sq") String customerLocale,
        boolean whatsappConsent,
        @NotBlank @Size(max = 255) String address,
        @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @NotNull LocalDate installedOn,
        @NotNull @Min(1) @Max(120) Integer warrantyMonths,
        @NotNull @Min(1) @Max(60) Integer serviceIntervalMonths,
        @Size(max = 2000) String notes) {}

    public record UpdateUnitRequest(
        @NotNull UnitType type,
        @NotBlank @Size(max = 80) String brand,
        @Size(max = 120) String model,
        @Size(max = 80) String serialNumber,
        @Size(max = 36) String platePhotoId,
        @NotBlank @Size(max = 255) String address,
        @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @NotNull LocalDate installedOn,
        @NotNull @Min(1) @Max(120) Integer warrantyMonths,
        @NotNull @Min(1) @Max(60) Integer serviceIntervalMonths,
        @Size(max = 2000) String notes) {}

    public record RemoveRequest(LocalDate removedOn) {}

    public record CustomerSummary(Long id, String name, String phone, String locale, boolean whatsappOptIn,
                                  Instant whatsappOptInAt) {
        public static CustomerSummary of(Customer c) {
            return new CustomerSummary(c.getId(), c.getName(), c.getPhone(), c.getLocale(), c.isWhatsappOptIn(),
                c.getWhatsappOptInAt());
        }
    }

    /** One line of the register. {@code serviceState} is null for removed units. */
    public record UnitRow(Long id, UnitType type, String brand, String model, String serialNumber, UnitStatus status,
                          LocalDate installedOn, LocalDate warrantyUntil, boolean warrantyActive, LocalDate nextServiceDue,
                          ServiceState serviceState, long daysUntilDue, String address, Long customerId,
                          String customerName, String customerPhone) {

        public static UnitRow of(Unit u, LocalDate today, int leadDays) {
            return new UnitRow(u.getId(), u.getType(), u.getBrand(), u.getModel(), u.getSerialNumber(), u.getStatus(),
                u.getInstalledOn(), u.getWarrantyUntil(), ServiceSchedule.warrantyActive(u.getWarrantyUntil(), today),
                u.getNextServiceDue(),
                u.isActive() ? ServiceSchedule.serviceState(u.getNextServiceDue(), today, leadDays) : null,
                ServiceSchedule.daysUntil(u.getNextServiceDue(), today), u.getAddress(), u.getCustomer().getId(),
                u.getCustomer().getName(), u.getCustomer().getPhone());
        }
    }

    public record PageView<T>(List<T> items, long total, int page, int size) {}

    /** The current cycle's reminder, as the unit page shows it. */
    public record CycleReminder(LocalDate dueOn, LocalDate goesOutOn, String outcome, Instant sentAt) {}

    public record OpenBooking(Long id, String status, LocalDate preferredDate, String preferredPeriod, Instant scheduledAt,
                              Instant createdAt) {}

    /**
     * One line of a unit's history. Flat on purpose: the client picks the fields each
     * kind needs (INSTALLED, VISIT, REMINDER, BOOKING, MESSAGE, REPLY, REMOVED).
     */
    public record TimelineEntry(String kind, Instant at, LocalDate date, String by, String visitKind, Integer priceCents,
                                String parts, String notes, String outcome, String templateKey, String body,
                                String messageStatus, String source, LocalDate preferredDate, String preferredPeriod,
                                String bookingStatus, Instant scheduledAt, Long refId) {}

    public record UnitDetail(Long id, UnitType type, String brand, String model, String serialNumber, UnitStatus status,
                             LocalDate installedOn, int warrantyMonths, LocalDate warrantyUntil, boolean warrantyActive,
                             int serviceIntervalMonths, LocalDate lastServiceOn, LocalDate nextServiceDue,
                             ServiceState serviceState, long daysUntilDue, LocalDate removedOn, String address,
                             Double latitude, Double longitude, String notes, String platePhotoId, String platePhotoUrl,
                             String installedByName, Instant createdAt, String cardNumber, String cardUrl, String cardShareUrl,
                             String reminderShareUrl, CustomerSummary customer, CycleReminder cycleReminder,
                             OpenBooking openBooking, MessageView lastCardMessage, List<UnitRow> otherUnits,
                             List<TimelineEntry> timeline) {}

    /** What the install screen shows after saving: the card and whether it reached the customer. */
    public record InstallResult(UnitDetail unit, MessageView cardMessage) {}

    public record SendResult(MessageView message, String shareUrl) {}
}
