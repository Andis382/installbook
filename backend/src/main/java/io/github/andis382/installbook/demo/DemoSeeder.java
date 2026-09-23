package io.github.andis382.installbook.demo;

import io.github.andis382.installbook.auth.Organization;
import io.github.andis382.installbook.auth.OrganizationRepository;
import io.github.andis382.installbook.auth.Role;
import io.github.andis382.installbook.auth.User;
import io.github.andis382.installbook.auth.UserRepository;
import io.github.andis382.installbook.bookings.BookingRequest;
import io.github.andis382.installbook.bookings.BookingRequestRepository;
import io.github.andis382.installbook.common.Tokens;
import io.github.andis382.installbook.config.AppProperties;
import io.github.andis382.installbook.customers.Customer;
import io.github.andis382.installbook.customers.CustomerRepository;
import io.github.andis382.installbook.demo.DemoCatalog.Model;
import io.github.andis382.installbook.demo.DemoCatalog.Town;
import io.github.andis382.installbook.files.FileStorage;
import io.github.andis382.installbook.messaging.InboundMessage;
import io.github.andis382.installbook.messaging.InboundMessageRepository;
import io.github.andis382.installbook.messaging.OutboundMessage;
import io.github.andis382.installbook.notify.CustomerNotifier;
import io.github.andis382.installbook.reminders.Reminder;
import io.github.andis382.installbook.reminders.ReminderRepository;
import io.github.andis382.installbook.settings.InstallerSettings;
import io.github.andis382.installbook.settings.Installers;
import io.github.andis382.installbook.units.Unit;
import io.github.andis382.installbook.units.UnitRepository;
import io.github.andis382.installbook.units.UnitType;
import io.github.andis382.installbook.visits.ServiceKind;
import io.github.andis382.installbook.visits.ServiceVisit;
import io.github.andis382.installbook.visits.ServiceVisitRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills an empty database with "Termo Hoxha", a Tirana installer with 30 months of history:
 * ~120 units for ~95 customers, services, reminders that did and did not turn into bookings,
 * a few requests waiting for a call, and some units nobody has serviced yet. History is played
 * forward through the same notifier the app uses, so every message reads exactly as a real one.
 */
@Component
@Profile("!test")
public class DemoSeeder implements ApplicationRunner {

    public static final String DEMO_EMAIL = "demo@installbook.test";
    public static final String TECH_EMAIL = "tech@installbook.test";
    public static final String DEMO_PASSWORD = "demo1234";
    /** Fixed so the README and screenshots can link the showcase warranty card. */
    public static final String SHOWCASE_TOKEN = "demo-vaillant-ecotec";

    private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);
    private static final int UNITS = 118;
    private static final int HISTORY_DAYS = 912;
    /** A reminder this recent can still get its answer (a late "yes" is common). */
    private static final int RECENT_DAYS = 45;
    /** What the unanswered recent reminders turn into, in order: calls to make first. */
    private enum Answer { ASKED, SCHEDULED, STOPPED }
    private static final List<Answer> ANSWERS = List.of(Answer.ASKED, Answer.ASKED, Answer.SCHEDULED, Answer.ASKED,
        Answer.STOPPED, Answer.SCHEDULED);

    private final AppProperties props;
    private final OrganizationRepository organizations;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final Installers installers;
    private final CustomerRepository customers;
    private final UnitRepository units;
    private final ServiceVisitRepository visits;
    private final ReminderRepository reminders;
    private final BookingRequestRepository bookings;
    private final InboundMessageRepository inbox;
    private final CustomerNotifier notifier;
    private final FileStorage files;
    private final Clock clock;

    public DemoSeeder(AppProperties props, OrganizationRepository organizations, UserRepository users, PasswordEncoder encoder,
                      Installers installers, CustomerRepository customers, UnitRepository units, ServiceVisitRepository visits,
                      ReminderRepository reminders, BookingRequestRepository bookings, InboundMessageRepository inbox,
                      CustomerNotifier notifier, FileStorage files, Clock clock) {
        this.props = props;
        this.organizations = organizations;
        this.users = users;
        this.encoder = encoder;
        this.installers = installers;
        this.customers = customers;
        this.units = units;
        this.visits = visits;
        this.reminders = reminders;
        this.bookings = bookings;
        this.inbox = inbox;
        this.notifier = notifier;
        this.files = files;
        this.clock = clock;
    }

    /** Everything the simulation needs to know about the business and "now". */
    private record Ctx(Organization org, User owner, User tech, InstallerSettings settings, LocalDate today, ZoneId zone,
                       Random random) {
        Instant at(LocalDate day, int hour, int minute) {
            return day.atTime(hour, minute).atZone(zone).toInstant();
        }

        LocalDate yesterday() {
            return today.minusDays(1);
        }

        Long someone() {
            return random.nextInt(10) < 7 ? owner.getId() : tech.getId();
        }
    }

    /** A unit whose current cycle's reminder went out and got no answer yet: material for open bookings. */
    private record Waiting(Unit unit, Reminder reminder) {}

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.isDemo() || users.count() > 0) {
            return;
        }
        Organization org = new Organization("Termo Hoxha");
        org.setPhone("355694201177");
        org.setLocale("sq");
        org.setTimezone("Europe/Tirane");
        organizations.save(org);
        User owner = users.save(new User(org.getId(), "Arben Hoxha", DEMO_EMAIL, encoder.encode(DEMO_PASSWORD), Role.OWNER));
        User tech = users.save(new User(org.getId(), "Ervis Dema", TECH_EMAIL, encoder.encode(DEMO_PASSWORD), Role.TECHNICIAN));
        InstallerSettings settings = installers.settings(org.getId());
        settings.setTypicalServicePriceCents(5500);
        settings.setPublicPhone("355684201177");

        LocalDate today = installers.today(org);
        Ctx ctx = new Ctx(org, owner, tech, settings, today, Installers.zone(org), new Random(20260923L));
        List<Waiting> waiting = new ArrayList<>();

        showcase(ctx);
        List<Customer> people = new ArrayList<>();
        Set<String> usedPhones = new HashSet<>(Set.of("355694201177", "355684201177", "355692281044"));
        for (LocalDate installedOn : installDates(ctx)) {
            Customer customer = people.size() > 12 && ctx.random().nextInt(100) < 21
                ? DemoCatalog.any(ctx.random(), people)
                : newCustomer(ctx, installedOn, usedPhones, people);
            Unit unit = install(ctx, customer, installedOn, typeFor(ctx.random(), installedOn), null);
            history(ctx, unit).ifPresent(r -> waiting.add(new Waiting(unit, r)));
        }
        openRequests(ctx, waiting);
        repairs(ctx);
        strayReplies(ctx, people);

        settings.setLastReminderRunOn(today.minusDays(1));
        settings.setLastDigestOn(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        log.info("Demo data ready: {} units. Sign in with {} / {} (technician: {}). Showcase card: {}",
            UNITS + 1, DEMO_EMAIL, DEMO_PASSWORD, TECH_EMAIL, props.link("/c/" + SHOWCASE_TOKEN));
    }

    /** Mira's Vaillant: installed 23 months ago, serviced through a card booking, reminded again this week. */
    private void showcase(Ctx ctx) {
        LocalDate installedOn = ctx.today().minusMonths(23).withDayOfMonth(12);
        Customer mira = new Customer(ctx.org().getId(), "Mira Kola", "355692281044", "sq");
        mira.optIn(ctx.at(installedOn, 11, 5));
        mira.setCreatedAt(ctx.at(installedOn, 11, 0));
        customers.save(mira);
        Model model = DemoCatalog.BOILERS.get(0);
        Unit unit = install(ctx, mira, installedOn, UnitType.BOILER, model);
        unit.setAddress("Rruga Myslym Shyri 42, Ap. 9, Tiranë");
        unit.setLocation(41.3239, 19.8108);
        unit.setNotes("Kaldaja në dollapin e ballkonit të kuzhinës. Oxhaku del nga muri i jashtëm.");

        LocalDate due = unit.getNextServiceDue();
        LocalDate remDay = due.minusDays(ctx.settings().getReminderLeadDays());
        Reminder first = remind(ctx, unit, remDay);
        LocalDate asked = remDay.plusDays(2);
        BookingRequest booking = new BookingRequest(ctx.org().getId(), unit, BookingRequest.Source.CARD, due.plusDays(6),
            BookingRequest.Period.MORNING, "Pasditeve nuk jam në shtëpi, paraditeve po.", ctx.at(asked, 19, 42));
        booking.setReminderId(first.getId());
        first.booked();
        bookings.save(booking);
        notifier.bookingReceived(booking).ifPresent(m -> m.backdate(ctx.at(asked, 19, 42)));
        LocalDate visitDay = due.plusDays(8);
        booking.schedule(ctx.at(visitDay, 9, 30), ctx.at(asked.plusDays(1), 9, 12));
        notifier.bookingScheduled(booking, ctx.org()).ifPresent(m -> m.backdate(ctx.at(asked.plusDays(1), 9, 12)));
        ServiceVisit visit = visit(ctx, unit, visitDay, ServiceKind.ANNUAL_SERVICE, 5500,
            "Kontroll i djegies, u pastrua shkëmbyesi", ctx.owner().getId());
        booking.done(visit.getId(), ctx.at(visitDay, 11, 20));
        unit.serviced(visitDay);
        LocalDate thisCycle = unit.getNextServiceDue().minusDays(ctx.settings().getReminderLeadDays());
        if (!thisCycle.isAfter(ctx.yesterday())) {
            remind(ctx, unit, thisCycle);
        }
    }

    /**
     * Install days over the last 30 months: more of them recent (the business is growing) and
     * more in the seasons people buy, early summer for air conditioning and autumn for heating.
     */
    private List<LocalDate> installDates(Ctx ctx) {
        double[] season = {0.8, 0.6, 0.6, 0.8, 1.2, 1.4, 1.3, 0.9, 1.4, 1.7, 1.5, 1.1};
        List<LocalDate> dates = new ArrayList<>();
        while (dates.size() < UNITS) {
            long daysAgo = Math.round(HISTORY_DAYS * Math.pow(ctx.random().nextDouble(), 1.35));
            LocalDate day = ctx.today().minusDays(daysAgo);
            if (ctx.random().nextDouble() * 1.7 > season[day.getMonthValue() - 1]) {
                continue;
            }
            dates.add(day.getDayOfWeek() == DayOfWeek.SUNDAY ? day.minusDays(1) : day);
        }
        dates.sort(Comparator.naturalOrder());
        return dates;
    }

    /** Air conditioners in early summer, boilers before and during winter. */
    private static UnitType typeFor(Random r, LocalDate day) {
        int month = day.getMonthValue();
        int[] weights;
        if (month >= 4 && month <= 8) {
            weights = new int[] {14, 58, 7, 9, 6, 4, 2};
        } else if (month >= 9 || month <= 2) {
            weights = new int[] {62, 6, 9, 13, 3, 5, 2};
        } else {
            weights = new int[] {38, 26, 8, 14, 7, 5, 2};
        }
        return UnitType.values()[DemoCatalog.pick(r, weights)];
    }

    private Customer newCustomer(Ctx ctx, LocalDate firstInstall, Set<String> usedPhones, List<Customer> people) {
        Random r = ctx.random();
        boolean woman = r.nextBoolean();
        String name = DemoCatalog.any(r, woman ? DemoCatalog.WOMEN : DemoCatalog.MEN) + " " + DemoCatalog.any(r, DemoCatalog.SURNAMES);
        String phone;
        do {
            phone = "3556" + (7 + r.nextInt(3)) + (1 + r.nextInt(9)) + String.format("%06d", r.nextInt(1_000_000));
        } while (!usedPhones.add(phone));
        Customer c = new Customer(ctx.org().getId(), name, phone, r.nextInt(100) < 14 ? "en" : "sq");
        Instant at = ctx.at(firstInstall, 9 + r.nextInt(8), r.nextInt(60));
        c.setCreatedAt(at);
        if (r.nextInt(100) < 82) {
            c.optIn(at);
        }
        customers.save(c);
        people.add(c);
        return c;
    }

    private Unit install(Ctx ctx, Customer customer, LocalDate installedOn, UnitType type, Model fixed) {
        Random r = ctx.random();
        Model model = fixed != null ? fixed : DemoCatalog.any(r, DemoCatalog.models(type));
        int week = Math.max(1, installedOn.minusMonths(1 + r.nextInt(5)).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        String serial = model.serial().make(r, installedOn.minusMonths(2).getYear(), week);
        String token = fixed != null ? SHOWCASE_TOKEN : Tokens.urlToken();
        Unit unit = new Unit(ctx.org().getId(), customer, type, model.brand(), token);
        unit.setModel(model.model());
        unit.setSerialNumber(serial);
        unit.setAddress(address(ctx, unit));
        unit.setInstalledBy(r.nextInt(10) < 7 ? ctx.owner().getId() : ctx.tech().getId());
        boolean longWarranty = fixed == null && model.brand().equals("Vaillant") && type == UnitType.BOILER && r.nextInt(3) == 0;
        int warranty = longWarranty ? 60 : model.warrantyMonths();
        unit.schedule(installedOn, warranty, 12);
        Instant at = ctx.at(installedOn, 10 + r.nextInt(7), r.nextInt(60));
        unit.setCreatedAt(at);
        units.save(unit);
        if (fixed != null || r.nextInt(100) < 88) {
            byte[] photo = PlateImages.jpeg(type, model.brand(), model.model(), serial, installedOn.minusMonths(2), r.nextLong());
            unit.setPlatePhotoId(files.store(ctx.org().getId(), photo, "image/jpeg", "plate.jpg").getId());
        }
        notifier.warrantyCard(unit).ifPresent(m -> m.backdate(at.plus(4, ChronoUnit.MINUTES)));
        return unit;
    }

    private String address(Ctx ctx, Unit unit) {
        Random r = ctx.random();
        Town town = DemoCatalog.TOWNS.get(DemoCatalog.pick(r, DemoCatalog.TOWN_WEIGHTS));
        unit.setLocation(town.lat() + (r.nextDouble() - 0.5) * town.spread(), town.lng() + (r.nextDouble() - 0.5) * town.spread());
        String street = DemoCatalog.any(r, town.streets());
        return switch (r.nextInt(3)) {
            case 0 -> street + " " + (1 + r.nextInt(140)) + ", " + town.name();
            case 1 -> "Pallati " + (1 + r.nextInt(40)) + ", Ap. " + (1 + r.nextInt(30)) + ", " + street + ", " + town.name();
            default -> street + ", Shkalla " + (1 + r.nextInt(4)) + ", Ap. " + (1 + r.nextInt(24)) + ", " + town.name();
        };
    }

    /**
     * Plays a unit's service cycles forward to yesterday. Returns the current cycle's reminder
     * when it went out and is still unanswered.
     */
    private Optional<Reminder> history(Ctx ctx, Unit unit) {
        Random r = ctx.random();
        int lead = ctx.settings().getReminderLeadDays();
        while (true) {
            LocalDate due = unit.getNextServiceDue();
            LocalDate remDay = due.minusDays(lead);
            if (remDay.isAfter(ctx.yesterday())) {
                return Optional.empty();
            }
            Reminder reminder = remind(ctx, unit, remDay);
            boolean reached = reminder.wentOut();
            int roll = r.nextInt(100);
            if (reached && roll < 32) {
                LocalDate asked = remDay.plusDays(1 + r.nextInt(8));
                LocalDate visitDay = due.plusDays(r.nextInt(18) - 6);
                if (visitDay.isAfter(ctx.yesterday()) || !asked.isBefore(visitDay.minusDays(1))) {
                    return Optional.of(reminder).filter(x -> !due.isBefore(ctx.today().minusDays(RECENT_DAYS)));
                }
                bookAndServe(ctx, unit, reminder, asked, visitDay);
            } else if (roll < (reached ? 78 : 68)) {
                LocalDate visitDay = due.plusDays(r.nextInt(38) - 12);
                if (visitDay.isAfter(ctx.yesterday())) {
                    return reached ? Optional.of(reminder) : Optional.empty();
                }
                visit(ctx, unit, visitDay, ServiceKind.ANNUAL_SERVICE, price(r), part(r), ctx.someone());
                reminder.serviced();
                unit.serviced(visitDay);
            } else {
                return reached && !due.isBefore(ctx.today().minusDays(RECENT_DAYS)) ? Optional.of(reminder) : Optional.empty();
            }
        }
    }

    private void bookAndServe(Ctx ctx, Unit unit, Reminder reminder, LocalDate asked, LocalDate visitDay) {
        Random r = ctx.random();
        boolean viaWhatsApp = r.nextInt(100) < 55;
        Instant askedAt = ctx.at(asked, 8 + r.nextInt(12), r.nextInt(60));
        BookingRequest booking = viaWhatsApp
            ? new BookingRequest(ctx.org().getId(), unit, BookingRequest.Source.WHATSAPP, null, BookingRequest.Period.ANY, null, askedAt)
            : new BookingRequest(ctx.org().getId(), unit, BookingRequest.Source.CARD, visitDay.plusDays(r.nextInt(3)),
                r.nextBoolean() ? BookingRequest.Period.MORNING : BookingRequest.Period.AFTERNOON, null, askedAt);
        booking.setReminderId(reminder.getId());
        reminder.booked();
        bookings.save(booking);
        if (viaWhatsApp) {
            reply(ctx, unit, r.nextBoolean() ? "Po" : "1", askedAt);
        }
        notifier.bookingReceived(booking).ifPresent(m -> m.backdate(askedAt.plus(1, ChronoUnit.MINUTES)));
        Instant decided = ctx.at(asked.plusDays(1), 9, 10 + r.nextInt(40));
        int[][] slots = {{9, 0}, {10, 30}, {14, 0}, {16, 30}};
        int[] slot = slots[r.nextInt(slots.length)];
        booking.schedule(ctx.at(visitDay, slot[0], slot[1]), decided);
        notifier.bookingScheduled(booking, ctx.org()).ifPresent(m -> m.backdate(decided));
        ServiceVisit visit = visit(ctx, unit, visitDay, ServiceKind.ANNUAL_SERVICE, price(r), part(r), ctx.someone());
        booking.done(visit.getId(), ctx.at(visitDay, slot[0] + 2, 5));
        unit.serviced(visitDay);
    }

    /** The reminder the daily run would have sent (or recorded as a call) on that morning. */
    private Reminder remind(Ctx ctx, Unit unit, LocalDate day) {
        Instant at = ctx.at(day, 8, 2 + ctx.random().nextInt(20));
        Reminder reminder = new Reminder(ctx.org().getId(), unit.getId(), unit.getNextServiceDue(), Reminder.Trigger.AUTO, at);
        if (unit.getCustomer().isWhatsappOptIn()) {
            OutboundMessage m = notifier.serviceDue(unit, day);
            m.backdate(at);
            reminder.delivered(m.getId(), at);
        }
        return reminders.save(reminder);
    }

    private ServiceVisit visit(Ctx ctx, Unit unit, LocalDate day, ServiceKind kind, Integer price, String parts, Long by) {
        ServiceVisit v = new ServiceVisit(ctx.org().getId(), unit.getId(), day, kind, price, parts, null, by);
        v.setCreatedAt(ctx.at(day, 17, 10 + ctx.random().nextInt(40)));
        return visits.save(v);
    }

    /** A few customers asked for a visit in the last days (NEW); a few already have a time (SCHEDULED). */
    private void openRequests(Ctx ctx, List<Waiting> waiting) {
        Random r = ctx.random();
        List<Waiting> pool = new ArrayList<>(waiting.stream().filter(w -> !w.unit().getCardToken().equals(SHOWCASE_TOKEN)).toList());
        Collections.shuffle(pool, r);
        for (int i = 0; i < Math.min(pool.size(), ANSWERS.size()); i++) {
            Waiting w = pool.get(i);
            Answer answer = ANSWERS.get(i);
            Unit unit = w.unit();
            LocalDate sent = w.reminder().getSentAt().atZone(ctx.zone()).toLocalDate();
            LocalDate asked = ctx.today().minusDays(r.nextInt(4));
            if (!asked.isAfter(sent)) {
                asked = sent.plusDays(1);
            }
            Instant askedAt = ctx.at(asked, 8 + r.nextInt(10), r.nextInt(60));
            if (askedAt.isAfter(clock.instant())) {
                askedAt = clock.instant().minus(3, ChronoUnit.HOURS);
            }
            if (answer == Answer.STOPPED) {
                reply(ctx, unit, "Ndalo", askedAt);
                unit.getCustomer().optOut(askedAt);
                notifier.optOutConfirmed(unit.getCustomer()).backdate(askedAt.plus(1, ChronoUnit.MINUTES));
                continue;
            }
            // alternate the two ways customers ask; the first one wrote a note on the card
            boolean viaWhatsApp = i % 2 == 1;
            String note = i != 0 ? null : "en".equals(unit.getCustomer().getLocale())
                ? "Please call before coming, the entrance code is 1405."
                : "Ju lutem më telefononi para se të vini, kodi i hyrjes është 1405.";
            BookingRequest booking = viaWhatsApp
                ? new BookingRequest(ctx.org().getId(), unit, BookingRequest.Source.WHATSAPP, null, BookingRequest.Period.ANY, null, askedAt)
                : new BookingRequest(ctx.org().getId(), unit, BookingRequest.Source.CARD, ctx.today().plusDays(2 + r.nextInt(9)),
                    r.nextBoolean() ? BookingRequest.Period.MORNING : BookingRequest.Period.AFTERNOON, note, askedAt);
            booking.setReminderId(w.reminder().getId());
            w.reminder().booked();
            bookings.save(booking);
            if (viaWhatsApp) {
                reply(ctx, unit, r.nextBoolean() ? "Po" : "1", askedAt);
            }
            Instant receivedAt = askedAt.plus(1, ChronoUnit.MINUTES);
            notifier.bookingReceived(booking).ifPresent(m -> m.backdate(receivedAt));
            if (answer == Answer.SCHEDULED) {
                LocalDate day = ctx.today().plusDays(1 + r.nextInt(8));
                if (day.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    day = day.plusDays(1);
                }
                Instant decided = askedAt.plus(2, ChronoUnit.HOURS).isAfter(clock.instant()) ? clock.instant() : askedAt.plus(2, ChronoUnit.HOURS);
                booking.schedule(ctx.at(day, r.nextBoolean() ? 9 : 15, r.nextBoolean() ? 0 : 30), decided);
                notifier.bookingScheduled(booking, ctx.org()).ifPresent(m -> m.backdate(decided));
            }
        }
    }

    /** Repairs and the odd warranty claim, which do not move the service cycle. */
    private void repairs(Ctx ctx) {
        Random r = ctx.random();
        for (Unit unit : units.findAll()) {
            long age = ChronoUnit.DAYS.between(unit.getInstalledOn(), ctx.today());
            if (age < 40 || r.nextInt(100) >= 9) {
                continue;
            }
            LocalDate day = unit.getInstalledOn().plusDays(20 + r.nextInt((int) age - 20));
            boolean underWarranty = !day.isAfter(unit.getWarrantyUntil());
            ServiceKind kind = underWarranty && r.nextInt(3) == 0 ? ServiceKind.WARRANTY_CLAIM : ServiceKind.REPAIR;
            visit(ctx, unit, day, kind, kind == ServiceKind.WARRANTY_CLAIM ? 0 : 3000 + r.nextInt(9) * 1000,
                DemoCatalog.any(r, DemoCatalog.REPAIR_PARTS), ctx.someone());
        }
    }

    /** Replies a person has to read: questions the app does not try to answer. */
    private void strayReplies(Ctx ctx, List<Customer> people) {
        String[] texts = {"A mund të vini të shtunën në mëngjes?", "Kaldaja bën zhurmë kur ndizet, a mund ta shikoni?"};
        for (int i = 0; i < texts.length; i++) {
            Customer c = people.get((i + 1) * 7 % people.size());
            InboundMessage in = new InboundMessage(c.getPhone(), texts[i], "text", null, null, "demo-" + UUID.randomUUID());
            in.setOrganizationId(ctx.org().getId());
            in.setReceivedAt(ctx.at(ctx.today().minusDays(1 + i * 2L), 18, 25));
            inbox.save(in);
        }
    }

    private void reply(Ctx ctx, Unit unit, String text, Instant at) {
        InboundMessage in = new InboundMessage(unit.getCustomer().getPhone(), text, "text", null, null, "demo-" + UUID.randomUUID());
        in.markHandled("Ndalo".equals(text) ? "opt-out" : "booking", ctx.org().getId(),
            "Ndalo".equals(text) ? CustomerNotifier.RELATED_CUSTOMER : CustomerNotifier.RELATED_UNIT,
            "Ndalo".equals(text) ? unit.getCustomer().getId() : unit.getId());
        in.setReceivedAt(at);
        inbox.save(in);
    }

    private static int price(Random r) {
        return 4500 + r.nextInt(6) * 500;
    }

    private static String part(Random r) {
        return r.nextInt(100) < 65 ? DemoCatalog.any(r, DemoCatalog.SERVICE_PARTS) : null;
    }
}
