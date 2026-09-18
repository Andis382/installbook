<!DOCTYPE html>
<html lang="{{ app()->getLocale() }}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="robots" content="noindex, nofollow">
<title>@yield('title', __('card.title'))</title>
<link rel="stylesheet" href="{{ asset('css/app.css') }}">
<link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect width='100' height='100' rx='20' fill='%230d6efd'/><text x='50' y='70' font-size='58' text-anchor='middle' fill='white' font-family='sans-serif' font-weight='bold'>iB</text></svg>">
</head>
<body>
<main class="public">
    @include('partials.flash')
    @yield('content')
</main>
@stack('scripts')
</body>
</html>
