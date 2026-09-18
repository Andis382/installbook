<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">
<head>
@include('partials.head')
<meta name="robots" content="noindex, nofollow">
<title>@yield('title', __('card.title'))</title>
</head>
<body>
<a class="skip" href="#main">{{ __('ui.a11y.skip') }}</a>
<main class="sheet" id="main" tabindex="-1">
    @include('partials.flash')
    @yield('content')
</main>
@stack('scripts')
</body>
</html>
