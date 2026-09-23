package io.github.andis382.installbook.units;

import io.github.andis382.installbook.ai.PlateReader;
import io.github.andis382.installbook.ai.PlateReading;
import io.github.andis382.installbook.auth.CurrentUser;
import io.github.andis382.installbook.auth.Organization;
import io.github.andis382.installbook.common.ApiException;
import io.github.andis382.installbook.messaging.MessagesController.MessageView;
import io.github.andis382.installbook.notify.CustomerNotifier;
import io.github.andis382.installbook.reminders.Reminder;
import io.github.andis382.installbook.reminders.ReminderService;
import io.github.andis382.installbook.settings.Installers;
import io.github.andis382.installbook.units.UnitDtos.InstallResult;
import io.github.andis382.installbook.units.UnitDtos.PageView;
import io.github.andis382.installbook.units.UnitDtos.RecordInstallRequest;
import io.github.andis382.installbook.units.UnitDtos.RemoveRequest;
import io.github.andis382.installbook.units.UnitDtos.SendResult;
import io.github.andis382.installbook.units.UnitDtos.UnitDetail;
import io.github.andis382.installbook.units.UnitDtos.UnitRow;
import io.github.andis382.installbook.units.UnitDtos.UpdateUnitRequest;
import io.github.andis382.installbook.visits.VisitService;
import io.github.andis382.installbook.visits.VisitService.VisitRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/units")
public class UnitsController {

    /** Brands every installer in the region fits; the register's own brands come first. */
    private static final List<String> COMMON_BRANDS = List.of("Vaillant", "Ariston", "Baxi", "Bosch", "Immergas", "Ferroli",
        "Daikin", "Gree", "Mitsubishi Electric", "LG", "Samsung", "Toshiba", "Viessmann", "Beretta", "Midea", "Hisense",
        "Huawei", "Fronius", "Growatt", "Ajax", "Paradox", "Hikvision");

    private final UnitRepository units;
    private final UnitService service;
    private final UnitViews views;
    private final VisitService visits;
    private final ReminderService reminders;
    private final PlateReader plateReader;
    private final CustomerNotifier notifier;
    private final Installers installers;
    private final CurrentUser currentUser;

    public UnitsController(UnitRepository units, UnitService service, UnitViews views, VisitService visits,
                           ReminderService reminders, PlateReader plateReader, CustomerNotifier notifier,
                           Installers installers, CurrentUser currentUser) {
        this.units = units;
        this.service = service;
        this.views = views;
        this.visits = visits;
        this.reminders = reminders;
        this.plateReader = plateReader;
        this.notifier = notifier;
        this.installers = installers;
        this.currentUser = currentUser;
    }

    public record PlateResult(String photoId, String photoUrl, boolean aiEnabled, PlateReading reading) {}

    @GetMapping
    @Transactional(readOnly = true)
    public PageView<UnitRow> list(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) UnitType type,
                                  @RequestParam(required = false) UnitSearch.Warranty warranty,
                                  @RequestParam(required = false) UnitSearch.Service service,
                                  @RequestParam(required = false) UnitSearch.Status status,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "40") int size) {
        Organization org = currentUser.organization();
        LocalDate today = installers.today(org);
        int lead = installers.settings(org.getId()).getReminderLeadDays();
        int pageSize = Math.min(Math.max(size, 1), 200);
        Page<Unit> found = units.findAll(new UnitSearch(q, type, warranty, service, status).toSpecification(org.getId(), today, lead),
            PageRequest.of(Math.max(page, 0), pageSize, Sort.by("nextServiceDue").ascending().and(Sort.by("id"))));
        return new PageView<>(found.map(u -> UnitRow.of(u, today, lead)).getContent(), found.getTotalElements(),
            found.getNumber(), pageSize);
    }

    @GetMapping("/brands")
    public List<String> brands() {
        Set<String> all = new LinkedHashSet<>(units.brandsByUse(currentUser.organizationId()));
        all.addAll(COMMON_BRANDS);
        return List.copyOf(all);
    }

    /** Stores the plate photo and, when AI is on and asked for, reads it. Manual entry works either way. */
    @PostMapping("/plate")
    public PlateResult readPlate(@RequestPart("file") MultipartFile file, @RequestParam(defaultValue = "true") boolean read) {
        PlateReader.Result result = plateReader.read(currentUser.organizationId(), file, read);
        String id = result.photo().getId();
        return new PlateResult(id, "/api/files/" + id, result.aiEnabled(), result.reading());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public InstallResult record(@Valid @RequestBody RecordInstallRequest req) {
        Organization org = currentUser.organization();
        Unit unit = service.recordInstall(org.getId(), currentUser.id(), req, installers.today(org));
        MessageView card = notifier.warrantyCard(unit).map(MessageView::of).orElse(null);
        return new InstallResult(views.detail(unit, org), card);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public UnitDetail get(@PathVariable Long id) {
        return views.detail(find(id), currentUser.organization());
    }

    @PutMapping("/{id}")
    @Transactional
    public UnitDetail update(@PathVariable Long id, @Valid @RequestBody UpdateUnitRequest req) {
        Organization org = currentUser.organization();
        Unit unit = service.update(find(id), req, installers.today(org));
        return views.detail(unit, org);
    }

    @PostMapping("/{id}/remove")
    @Transactional
    public UnitDetail remove(@PathVariable Long id, @RequestBody(required = false) RemoveRequest req) {
        Organization org = currentUser.organization();
        Unit unit = service.remove(find(id), req == null ? null : req.removedOn(), installers.today(org));
        return views.detail(unit, org);
    }

    @PostMapping("/{id}/restore")
    @Transactional
    public UnitDetail restore(@PathVariable Long id) {
        return views.detail(service.restore(find(id)), currentUser.organization());
    }

    @PostMapping("/{id}/visits")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public UnitDetail recordVisit(@PathVariable Long id, @Valid @RequestBody VisitRequest req) {
        Organization org = currentUser.organization();
        Unit unit = find(id);
        visits.record(unit, req, currentUser.id(), installers.today(org));
        return views.detail(unit, org);
    }

    @PostMapping("/{id}/remind")
    @Transactional
    public SendResult remindNow(@PathVariable Long id) {
        Organization org = currentUser.organization();
        Unit unit = find(id);
        Reminder reminder = reminders.sendNow(unit, installers.today(org));
        return new SendResult(messageOf(reminder), null);
    }

    /** Sends the warranty card again (for a customer who agreed to messages). */
    @PostMapping("/{id}/card")
    @Transactional
    public SendResult resendCard(@PathVariable Long id) {
        Unit unit = find(id);
        return notifier.warrantyCard(unit)
            .map(m -> new SendResult(MessageView.of(m), null))
            .orElseThrow(() -> ApiException.conflict("customer.no_consent"));
    }

    private MessageView messageOf(Reminder reminder) {
        return views.message(reminder.getMessageId());
    }

    private Unit find(Long id) {
        return units.findByIdAndOrganizationId(id, currentUser.organizationId()).orElseThrow(ApiException::notFound);
    }
}
