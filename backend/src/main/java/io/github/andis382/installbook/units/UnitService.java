package io.github.andis382.installbook.units;

import io.github.andis382.installbook.common.ApiException;
import io.github.andis382.installbook.common.Tokens;
import io.github.andis382.installbook.customers.Customer;
import io.github.andis382.installbook.customers.CustomerService;
import io.github.andis382.installbook.files.StoredFileRepository;
import io.github.andis382.installbook.units.UnitDtos.RecordInstallRequest;
import io.github.andis382.installbook.units.UnitDtos.UpdateUnitRequest;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Recording, correcting and retiring units. Messages are the caller's business. */
@Service
public class UnitService {

    private final UnitRepository units;
    private final CustomerService customers;
    private final StoredFileRepository files;

    public UnitService(UnitRepository units, CustomerService customers, StoredFileRepository files) {
        this.units = units;
        this.customers = customers;
        this.files = files;
    }

    @Transactional
    public Unit recordInstall(Long organizationId, Long userId, RecordInstallRequest req, LocalDate today) {
        requireNotFuture(req.installedOn(), today, "installedOn");
        String serial = normalizeSerial(req.serialNumber());
        requireUniqueSerial(organizationId, serial, null);
        Customer customer = customers.upsert(organizationId, req.customerPhone(), req.customerName(), req.customerLocale(),
            req.whatsappConsent());

        Unit unit = new Unit(organizationId, customer, req.type(), req.brand().trim(), Tokens.urlToken());
        unit.setModel(blankToNull(req.model()));
        unit.setSerialNumber(serial);
        unit.setPlatePhotoId(ownedPhoto(organizationId, req.platePhotoId()));
        unit.setAddress(req.address().trim());
        unit.setLocation(req.latitude(), req.longitude());
        unit.setNotes(blankToNull(req.notes()));
        unit.setInstalledBy(userId);
        unit.schedule(req.installedOn(), req.warrantyMonths(), req.serviceIntervalMonths());
        return units.save(unit);
    }

    @Transactional
    public Unit update(Unit unit, UpdateUnitRequest req, LocalDate today) {
        requireNotFuture(req.installedOn(), today, "installedOn");
        String serial = normalizeSerial(req.serialNumber());
        requireUniqueSerial(unit.getOrganizationId(), serial, unit.getId());
        unit.setType(req.type());
        unit.setBrand(req.brand().trim());
        unit.setModel(blankToNull(req.model()));
        unit.setSerialNumber(serial);
        if (!Objects.equals(req.platePhotoId(), unit.getPlatePhotoId())) {
            unit.setPlatePhotoId(ownedPhoto(unit.getOrganizationId(), req.platePhotoId()));
        }
        unit.setAddress(req.address().trim());
        unit.setLocation(req.latitude(), req.longitude());
        unit.setNotes(blankToNull(req.notes()));
        unit.schedule(req.installedOn(), req.warrantyMonths(), req.serviceIntervalMonths());
        return unit;
    }

    @Transactional
    public Unit remove(Unit unit, LocalDate on, LocalDate today) {
        LocalDate day = on == null ? today : on;
        requireNotFuture(day, today, "removedOn");
        unit.remove(day.isBefore(unit.getInstalledOn()) ? unit.getInstalledOn() : day);
        return unit;
    }

    @Transactional
    public Unit restore(Unit unit) {
        requireUniqueSerial(unit.getOrganizationId(), unit.getSerialNumber(), unit.getId());
        unit.restore();
        return unit;
    }

    /** Plates are stamped in capitals; people type them however. Inner spacing is kept, runs collapsed. */
    static String normalizeSerial(String raw) {
        String s = blankToNull(raw);
        return s == null ? null : s.replaceAll("\\s+", " ").toUpperCase();
    }

    private void requireUniqueSerial(Long organizationId, String serial, Long exceptUnitId) {
        if (serial == null) {
            return;
        }
        boolean taken = units.findIdsBySerial(organizationId, UnitStatus.ACTIVE, serial).stream()
            .anyMatch(id -> !id.equals(exceptUnitId));
        if (taken) {
            throw ApiException.field("serialNumber", "unit.serial_taken");
        }
    }

    private String ownedPhoto(Long organizationId, String photoId) {
        if (photoId == null || photoId.isBlank()) {
            return null;
        }
        return files.findByIdAndOrganizationId(photoId, organizationId).map(f -> f.getId()).orElse(null);
    }

    private static void requireNotFuture(LocalDate date, LocalDate today, String field) {
        if (date.isAfter(today)) {
            throw ApiException.field(field, "unit.date_future");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
