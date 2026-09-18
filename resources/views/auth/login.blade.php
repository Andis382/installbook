<!DOCTYPE html>
<html lang="{{ app()->getLocale() }}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{{ __('auth.login_title') }} · {{ __('ui.app_name') }}</title>
<link rel="stylesheet" href="{{ asset('css/app.css') }}">
</head>
<body>
<header class="topbar"><a class="brand" href="{{ route('home') }}">install<span>book</span></a></header>
<main class="wrap narrow">
    @include('partials.flash')
    <h1>{{ __('auth.login_title') }}</h1>
    <form method="post" action="{{ route('login') }}" class="card">
        @csrf
        <label>{{ __('auth.email') }}<input type="email" name="email" value="{{ old('email', $email ?? '') }}" required autofocus autocomplete="email"></label>
        <label>{{ __('auth.password') }}<input type="password" name="password" required autocomplete="current-password"></label>
        <button class="btn block">{{ __('auth.login_title') }}</button>
    </form>
    <p class="center">{{ __('auth.no_account') }} <a href="{{ route('register') }}">{{ __('ui.nav.register') }}</a></p>
</main>
</body>
</html>
