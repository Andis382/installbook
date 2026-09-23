package io.github.andis382.installbook.cards;

import io.github.andis382.installbook.auth.Organization;
import io.github.andis382.installbook.auth.OrganizationRepository;
import io.github.andis382.installbook.auth.User;
import io.github.andis382.installbook.auth.UserRepository;
import io.github.andis382.installbook.bookings.BookingRequest;
import io.github.andis382.installbook.bookings.BookingService;
import io.github.andis382.installbook.common.ApiException;
import io.github.andis382.installbook.common.SignedUrls;
import io.github.andis382.installbook.notify.CustomerNotifier;
import io.github.andis382.installbook.settings.InstallerSettings;
import io.github.andis382.installbook.settings.Installers;
import io.github.andis382.installbook.units.ServiceSchedule;
import io.github.andis382.installbook.units.Unit;
import io.github.andis382.installbook.units.UnitRepository;
import io.github.andis382.installbook.units.UnitType;
import io.github.andis382.installbook.visits.ServiceVisit;
import io.github.andis382.installbook.visits.ServiceVisitRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The customer's warranty card: no login, only the unguessable token from the link. It shows
 * what the customer needs about their own unit and nothing about the installer's register
 * (no internal ids, no other customers, not even their own phone number).
 */
@RestController
@RequestMapping("/api/public/cards/{token}")
public class PublicCardController {

    private final UnitRepository units;
    private final ServiceVisitRepository visits;
    private final OrganizationRepository organizations;
    private final BookingService bookings;
    private final CertificatePdf pdf;
    private final CustomerNotifier notifier;
    private final Installers installers;
    private final SignedUrls signedUrls;
    private final UserRepository users;

    public PublicCardController(UnitRepository units, ServiceVisitRepository visits, OrganizationRepository organizations,
                                BookingService bookings, CertificatePdf pdf, CustomerNotifier notifier, Installers installers,
                                SignedUrls signedUrls, UserRepository users) {
        this.units = units;
        this.visits = visits;
        this.organizations = organizations;
        this.bookings = bookings;
        this.pdf = pdf;
        this.notifier = notifier;
        this.installers = installers;
        this.signedUrls = signedUrls;
        this.users = users;
    }

    public record Business(String name, String phone) {}

    public record CardUnit(UnitType type, String brand, String model, String serialNumber, String address,
                           LocalDate installedOn, int warrantyMonths, LocalDate warrantyUntil, boolean warrantyActive,
                           int serviceIntervalMonths, LocalDate lastServiceOn, LocalDate nextServiceDue, long daysUntilDue,
                           boolean active, LocalDate removedOn, String platePhotoUrl) {}

    public record CardVisit(LocalDate date, String kind, String parts) {}

    public record CardBooking(String status, LocalDate preferredDate, String preferredPeriod, Instant scheduledAt,
                              Instant createdAt) {
        static CardBooking of(BookingRequest b) {
            return new CardBooking(b.getStatus().name(), b.getPreferredDate(),
                b.getPreferredPeriod() == null ? null : b.getPreferredPeriod().name(), b.getScheduledAt(), b.getCreatedAt());
        }
    }

    public record Card(String cardNumber, String locale, String customerName, LocalDate today, Business business,
                       CardUnit unit, List<CardVisit> history, CardBooking openBooking) {}

    public record BookingRequestBody(
        @NotNull LocalDate preferredDate,
        @NotNull BookingRequest.Period preferredPeriod,
        @Size(max = 500) String note) {}

    public record BookingResult(boolean created, CardBooking booking) {}

    @GetMapping
    @Transactional(readOnly = true)
    public Card card(@PathVariable String token) {
        Unit unit = find(token);
        Organization org = organizations.findById(unit.getOrganizationId()).orElseThrow();
        InstallerSettings settings = installers.settings(org.getId());
        LocalDate today = installers.today(org);
        List<CardVisit> history = visits.findByUnitIdOrderByVisitedOnDescIdDesc(unit.getId()).stream()
            .map(v -> new CardVisit(v.getVisitedOn(), v.getKind().name(), v.getParts()))
            .toList();
        CardUnit cardUnit = new CardUnit(unit.getType(), unit.getBrand(), unit.getModel(), unit.getSerialNumber(),
            unit.getAddress(), unit.getInstalledOn(), unit.getWarrantyMonths(), unit.getWarrantyUntil(),
            ServiceSchedule.warrantyActive(unit.getWarrantyUntil(), today), unit.getServiceIntervalMonths(),
            unit.getLastServiceOn(), unit.getNextServiceDue(), ServiceSchedule.daysUntil(unit.getNextServiceDue(), today),
            unit.isActive(), unit.getRemovedOn(),
            unit.getPlatePhotoId() == null ? null : signedUrls.fileUrl(unit.getPlatePhotoId(), Duration.ofHours(2)));
        return new Card(CertificatePdf.cardNumber(unit), unit.getCustomer().getLocale(), unit.getCustomer().getName(), today,
            new Business(org.getName(), installers.publicPhone(org, settings)), cardUnit, history,
            bookings.openFor(unit).map(CardBooking::of).orElse(null));
    }

    /** "Book my service". Asking again while a request is open returns that request. */
    @PostMapping("/bookings")
    @Transactional
    public BookingResult book(@PathVariable String token, @Valid @RequestBody BookingRequestBody req) {
        Unit unit = find(token);
        Organization org = organizations.findById(unit.getOrganizationId()).orElseThrow();
        BookingService.Created result = bookings.request(unit, BookingRequest.Source.CARD, req.preferredDate(),
            req.preferredPeriod(), req.note(), installers.today(org));
        return new BookingResult(result.created(), CardBooking.of(result.booking()));
    }

    @GetMapping("/certificate.pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> certificate(@PathVariable String token, @RequestParam(required = false) String lang) {
        Unit unit = find(token);
        Organization org = organizations.findById(unit.getOrganizationId()).orElseThrow();
        String locale = "en".equals(lang) || "sq".equals(lang) ? lang : unit.getCustomer().getLocale();
        List<ServiceVisit> history = visits.findByUnitIdOrderByVisitedOnDescIdDesc(unit.getId());
        String installer = unit.getInstalledBy() == null ? null
            : users.findById(unit.getInstalledBy()).map(User::getName).orElse(null);
        CertificatePdf.Issuer issuer = new CertificatePdf.Issuer(org.getName(),
            installers.publicPhoneDisplay(org, installers.settings(org.getId())), installer, notifier.cardLink(unit));
        byte[] bytes = pdf.render(unit, history, issuer, locale, installers.today(org));
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename("warranty-" + CertificatePdf.cardNumber(unit) + ".pdf").build().toString())
            .body(bytes);
    }

    private Unit find(String token) {
        if (token == null || token.length() > 64) {
            throw ApiException.notFound();
        }
        return units.findByCardToken(token).orElseThrow(ApiException::notFound);
    }
}
