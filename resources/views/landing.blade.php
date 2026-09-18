<!DOCTYPE html>
<html lang="{{ app()->getLocale() }}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{{ __('ui.app_name') }} — {{ __('ui.tagline') }}</title>
<meta name="description" content="A register of every unit an installer has fitted: serial, install date, warranty card for the customer, and a reminder when the service is due.">
<link rel="stylesheet" href="{{ asset('css/app.css') }}">
</head>
<body>
<header class="topbar">
    <a class="brand" href="{{ route('home') }}">install<span>book</span></a>
    <nav class="row tight">
        <a href="{{ route('login') }}">{{ __('ui.nav.login') }}</a>
        <a class="btn small" href="{{ route('register') }}">{{ __('ui.nav.register') }}</a>
    </nav>
</header>

<main class="wrap">
    <h1 style="font-size:28px">{{ __('ui.tagline') }}</h1>
    <p class="muted" style="font-size:18px">
        You fit two hundred machines a year. Each one has a serial number, a warranty, and a service
        due twelve months later. None of that is written down anywhere, so the service only happens
        if the customer thinks of it. This is the notebook that remembers instead.
    </p>

    <div class="card">
        <h3>Photograph the plate</h3>
        <p>One photo, one tap for the type, the customer's phone number. Under a minute, standing at the unit. The serial can be added later; the photo is already the record.</p>
    </div>
    <div class="card">
        <h3>The customer gets a card they cannot lose</h3>
        <p>A link with the unit, the serial, the install date and both warranty dates, in their language. It opens in your own WhatsApp, from your own number. No business account, no approvals, no per-message fee.</p>
    </div>
    <div class="card">
        <h3>You get told before the service is due</h3>
        <p>Thirty days before, and on the day. The customer taps one button to ask for a visit and it lands on your list. When you record the service, the next one is scheduled from that date.</p>
    </div>

    <h2>Why this and not a field-service app</h2>
    <p class="muted">
        Every product in this category is priced per user, assumes somebody in an office types in customers,
        and triggers reminders from a customer record rather than from a machine. An installer's unit of work
        is a machine at an address, which is why those apps sit unfilled. Here the machine is the record and
        the serial is what the reminder hangs on.
    </p>

    <div class="card">
        <h3>Honest by construction</h3>
        <p class="small">
            A customer's statutory guarantee is shown separately from the manufacturer's warranty, because only
            the second one can depend on servicing. A reminder may mention the warranty only when that unit is
            recorded as genuinely requiring an annual service. Nothing is sent to anyone who has not agreed,
            and every message says how to stop.
        </p>
    </div>

    <a class="btn big block" href="{{ route('register') }}">{{ __('ui.nav.register') }}</a>
    <p class="center small muted">Open source. Run it yourself, or read exactly what it does.</p>
</main>
</body>
</html>
