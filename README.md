# InstallBook

**A register of every machine an installer has fitted: the serial, the install date, a warranty card the customer cannot lose, and a reminder when the service is due.**

An HVAC or plumbing installer fits one to three hundred units a year. Each one has a serial number on a plate behind it, a manufacturer warranty that usually only starts if the unit is registered within a few weeks, and a service due twelve months later that is worth more per hour than the installation was. None of that is written down anywhere, so the service only happens if the customer happens to think of it.

This is the notebook that remembers instead.

```
photograph the plate  →  one tap for the type  →  the customer's phone number
                      →  they get a card they can keep
                      →  you get told before the service is due
```

---

## Why it is shaped like this

Every product in this category prices per user, assumes somebody in an office types customers in, and fires reminders from a customer record or a certificate date. An installer's unit of work is **a machine at an address**, not a person, which is why those apps sit unfilled. Here the machine is the record and the serial is what the reminder hangs on.

Three decisions follow from that, and they are the whole design:

**The form never refuses.** You can save an installation with nothing but its type. No serial, no customer, no address, no photo. A record with a photo and no name is worth more than the note that never got written because the app said no. Everything can be completed later, from the van.

**Nothing needs a Meta account.** The default messaging driver writes the message and hands it to you: you tap it and it opens in *your own* WhatsApp, to your own customer, from your own number, with the text already typed. No business verification, no template approval, no per-message fee, and the customer gets a message from a number they recognise. The WhatsApp Cloud API is supported and switched off.

**The two guarantees are never merged.** A buyer's statutory guarantee is owed by the seller by law and cannot be lost by missing a service. The manufacturer's warranty is a separate promise that sometimes does depend on annual servicing. Showing them as one number is the small lie that lets a reminder pretend a missed service is a catastrophe, so they are kept apart on the card and in every message.

Honest wording is also the cheap wording: a notice about one specific machine is a "utility" message in WhatsApp's own categories, several times cheaper than anything that reads like an advert. `tests/Unit/MessageToneTest.php` enforces it against the shipped text in every language, so a translation that drifts into sales language fails the build.

---

## What it does

| | |
|---|---|
| **Capture** | Photograph the data plate, pick the type, type a phone number. Warranty length and service interval are filled in from the appliance type. Optional GPS. Under a minute, one-handed. |
| **Warranty card** | A public page on an unguessable link: unit, serial, install date, both guarantee dates, service history, your contact details, in the customer's language. It prints cleanly, and it stays live as the dates change. |
| **Manufacturer registration** | Most manufacturer warranties only start if the unit is registered soon after it is fitted. Every install gets a deadline and a list, so that value arrives in week three rather than month eleven. |
| **Service reminders** | Thirty days before the service is due, and on the day. Configurable. A unit that is already overdue when you record it still gets exactly one catch-up message. |
| **Seasonal sense** | A boiler due in December is nudged to October, an air conditioner due in March to May, but never by more than three months, so a manufacturer's interval is not quietly broken. |
| **Booking** | The customer taps one button on the card. It lands on your list, and it stops the reminders for that cycle: someone who has asked should not then be reminded to ask. |
| **Service history** | Every visit on every machine. Recording a service restarts the clock from that date and replans the reminders. |
| **Consent and erasure** | Nothing is sent to anyone who has not agreed, the exact wording they agreed to is stored, every card has a one-tap opt-out, and a customer's whole file can be exported or erased. |
| **Languages** | Albanian and English, for the interface and separately for each customer, so an Albanian installer can hand an Italian customer a card in English. |
| **Plate reading** | Optional. A vision model reads the photographed plate and pre-fills brand, model and serial for you to confirm. Off by default; typing always works. |

---

## Running it

Needs PHP 8.2+ and Composer. No Node, no build step, no npm install: the CSS is hand-written.

```bash
git clone https://github.com/Andis382/installbook.git
cd installbook
composer install
cp .env.example .env
php artisan key:generate
php artisan migrate --seed
php artisan storage:link
php artisan serve
```

Open http://localhost:8000 and log in as `demo@installbook.test` / `password`. The seed gives you an installer with a year of work behind him, so the dashboard has things overdue, things due soon, and units waiting to be registered.

**Database.** SQLite by default, which needs no setup but does need the `pdo_sqlite` extension. For Postgres, set `DB_CONNECTION=pgsql` and the usual credentials; the schema and queries are portable, and the suite is run against both.

**Reminders.** One cron line drives everything:

```
* * * * * cd /path/to/installbook && php artisan schedule:run >> /dev/null 2>&1
```

Or run the engine directly:

```bash
php artisan installbook:send-reminders --dry-run     # show what would go out
php artisan installbook:send-reminders --date=2027-03-01
```

**Tests.**

```bash
php artisan test
```

59 tests. If your PHP has no `pdo_sqlite`, point the suite at a real database instead: `DB_CONNECTION=pgsql DB_DATABASE=installbook_test php artisan test`.

---

## How the reminder engine works

```
record an install ──▶ plan reminders
                       ├─ 30 days before the service is due   (configurable)
                       ├─ on the day it is due
                       └─ 30 days before the manufacturer warranty ends

record a service  ──▶ clock restarts from that date, reminders replanned
customer books    ──▶ service reminders cancelled ("booked")
archive the unit  ──▶ all reminders cancelled, record and card kept
customer opts out ──▶ all reminders cancelled, nothing can be queued again
```

A reminder that has been sitting unfired for more than a fortnight is skipped rather than sent, so a week of downtime does not become a week of late messages arriving at once. Running the command twice on the same day does not send twice.

Everything is one table of dated rows you can look at, not a scheduler you have to trust.

---

## Layout

```
app/
  Enums/ApplianceType.php        the types, and their default warranty and service intervals
  Services/
    ScheduleCalculator.php       install date + intervals → warranty, registration and service dates
    ReminderPlanner.php          what gets said about a unit, and when
    InstallationRegistrar.php    recording an install or a visit, in one transaction
    Messaging/                   composer, dispatcher, and the manual / log / cloud_api drivers
    Plate/                       the optional plate reader and its null implementation
  Support/Phone.php              one phone number, however it was typed
  Support/Serial.php             folds O/0, I/1, S/5, B/8 so a mistyped serial still finds its unit
  Http/Controllers/              installer pages, the public card, a small admin surface
resources/views/                 Blade, mobile first
public/css/app.css               hand written, no build step
lang/{sq,en}/                    the interface, and every word a customer receives
tests/                           59 tests: dates, tenancy, the engine, the card, message tone
```

---

## Honest notes

- **This category exists.** GasPro and Checker sell service reminders to UK gas engineers at £8–11 a month; Gas Engineer Software publishes the only hard number anyone has, that about a third of service reminders turn into booked jobs. ServiceM8 has genuine asset management, gated at roughly four times this price and built around QR labels you have to buy. Daikin's Stand By Me is this exact loop for one manufacturer's units. The claim here is narrow and, I think, true: nothing is built around the machine, in Albanian, on WhatsApp, flat-priced for a one-person business.
- **Viber is unresolved.** It may be the more-used app in Kosovo and parts of the region. The channel is a driver interface and every message screen offers WhatsApp, SMS and Viber, but this needs ten phone calls to real installers before anyone writes more channel code.
- **The warranty and service defaults are defaults.** They vary by manufacturer, model and market. They are editable per unit and the card says to check your own paperwork. The figures for water heaters and alarm panels are the least well sourced.
- **Offline capture is not implemented.** Boiler rooms have no signal, and a hand-rolled photo queue is the one part of this that could silently lose a record. The app installs to a phone home screen and is usable one-handed; capturing with no signal is honest future work, not a shipped feature.
- **No payments, invoicing or certificates.** Deliberately. Those are what make the competition cost £20–44 per user per month.

## Roadmap

- QR self-claim: turn the phone round, the customer scans, picks their language and types their own number and consent
- Offline capture with a real outbox
- A per-country certificate generator (Italian libretto d'impianto, Greek maintenance sheet) on top of the same unit schema
- Viber, once someone has actually asked for it
- Photo compression before upload

## Licence

MIT. See [LICENSE](LICENSE).
