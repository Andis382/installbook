<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="theme-color" content="#0d1b2f">
<meta name="color-scheme" content="light dark">

{{--
    Plus Jakarta Sans for the interface — it has real character at heading
    weights and stays quiet at 16px, which is the whole job here. JetBrains Mono
    for serials, dates and counts, where tabular figures stop a column of dates
    from dancing. Both with display=swap behind a full system fallback: this app
    is used in basements, and text waiting for a font it will never get is worse
    than text in the wrong font.
--}}
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet"
      href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500;700&display=swap">

<link rel="stylesheet" href="{{ asset('css/app.css') }}?v={{ config('installbook.asset_version', '3') }}">
<link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 64 64'><rect width='64' height='64' rx='14' fill='%230d1b2f'/><rect x='14' y='17' width='36' height='5' rx='2.5' fill='%23ffffff'/><rect x='14' y='29' width='36' height='5' rx='2.5' fill='%23ffffff' opacity='.55'/><rect x='14' y='41' width='20' height='5' rx='2.5' fill='%233b6bf0'/></svg>">
