@extends('layouts.plain')
@section('title', __('ui.tagline'))
@section('description', 'A register of every unit an installer has fitted: serial, install date, a warranty card the customer cannot lose, and a reminder when the service is due.')

@section('topnav')
    <a class="btn ghost small" href="{{ route('login') }}">{{ __('ui.nav.login') }}</a>
    <a class="btn small" href="{{ route('register') }}">{{ __('ui.nav.register') }}</a>
@endsection

@section('content')
<div class="hero">
    <span class="micro">{{ __('ui.app_name') }}</span>
    <h1>{{ __('ui.tagline') }}</h1>
    <p class="lead">
        You fit two hundred machines a year. Each one has a serial number, a warranty, and a service
        due twelve months later. None of that is written down anywhere, so the service only happens
        if the customer thinks of it. This is the notebook that remembers instead.
    </p>
</div>

<div class="flow">
    <div>
        <span class="n">01</span>
        <h3>Photograph the plate</h3>
        <p>One photo, one tap for the type, the customer's number. Under a minute, standing at the unit. The serial can wait; the photo is already the record.</p>
    </div>
    <div>
        <span class="n">02</span>
        <h3>They get a card they cannot lose</h3>
        <p>A link with the unit, the serial, the install date and both guarantee dates, in their language. It opens in your own WhatsApp, from your own number. No business account, no approvals, no per-message fee.</p>
    </div>
    <div>
        <span class="n">03</span>
        <h3>You get told before it is due</h3>
        <p>Thirty days before, and on the day. The customer taps one button to ask for a visit and it lands on your list. Record the service and the next one is scheduled from that date.</p>
    </div>
</div>

<h2>Why this and not a field-service app</h2>
<p class="lead">
    Every product in this category is priced per user, assumes somebody in an office types in customers,
    and fires reminders from a customer record rather than from a machine. An installer's unit of work
    is a machine at an address, which is why those apps sit unfilled. Here the machine is the record and
    the serial is what the reminder hangs on.
</p>

<div class="panel lead-primary">
    <span class="micro">Honest by construction</span>
    <p class="small">
        A customer's statutory guarantee is shown separately from the manufacturer's warranty, because only
        the second one can depend on servicing. A reminder may mention the warranty only when that unit is
        recorded as genuinely requiring an annual service. Nothing is sent to anyone who has not agreed,
        and every message says how to stop.
    </p>
</div>

<a class="btn big block" href="{{ route('register') }}">
    {{ __('ui.nav.register') }}
    <x-icon name="chevron" size="18" />
</a>
<p class="center small muted">Open source. Run it yourself, or read exactly what it does.</p>
@endsection
