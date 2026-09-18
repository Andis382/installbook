<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="theme-color" content="#1e3a5f" media="(prefers-color-scheme: light)">
<meta name="theme-color" content="#0e1116" media="(prefers-color-scheme: dark)">
<meta name="color-scheme" content="light dark">

{{--
    Inter for everything, JetBrains Mono for serials, dates and counts, where
    tabular figures stop a column of dates from dancing. Both are loaded with
    display=swap and a full system fallback behind them: this app is used in
    basements, and text that waits for a font it will never get is worse than
    text in the wrong font.
--}}
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet"
      href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500;700&display=swap">

<link rel="stylesheet" href="{{ asset('css/app.css') }}?v={{ config('installbook.asset_version', '2') }}">
<link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 64 64'><rect width='64' height='64' fill='%231e3a5f'/><rect x='12' y='18' width='40' height='5' fill='white'/><rect x='12' y='30' width='40' height='5' fill='white'/><rect x='12' y='42' width='22' height='5' fill='%2359d9a5'/></svg>">
