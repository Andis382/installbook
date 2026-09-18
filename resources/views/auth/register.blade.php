<!DOCTYPE html>
<html lang="{{ app()->getLocale() }}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{{ __('auth.register_title') }} · {{ __('ui.app_name') }}</title>
<link rel="stylesheet" href="{{ asset('css/app.css') }}">
</head>
<body>
<header class="topbar"><a class="brand" href="{{ route('home') }}">install<span>book</span></a></header>
<main class="wrap narrow">
    @include('partials.flash')
    <h1>{{ __('auth.register_title') }}</h1>
    <p class="muted">{{ __('auth.register_intro') }}</p>
    <form method="post" action="{{ route('register') }}" class="card">
        @csrf
        <label>{{ __('ui.settings.name') }}<input type="text" name="name" value="{{ old('name', $name ?? '') }}" required autofocus autocomplete="name"></label>
        <label>{{ __('ui.settings.business_name') }} <span class="muted">({{ __('ui.common.optional') }})</span>
            <input type="text" name="business_name" value="{{ old('business_name') }}">
        </label>
        <div class="grid2">
            <label>{{ __('ui.settings.phone') }}<input type="tel" name="phone" value="{{ old('phone') }}"></label>
            <label>{{ __('ui.settings.city') }}<input type="text" name="city" value="{{ old('city') }}"></label>
        </div>
        <label>{{ __('ui.settings.language') }}
            <select name="locale">
                @foreach (config('installbook.locales') as $code => $label)
                    <option value="{{ $code }}" @selected(old('locale', config('installbook.defaults.locale')) === $code)>{{ $label }}</option>
                @endforeach
            </select>
        </label>
        <label>{{ __('auth.email') }}<input type="email" name="email" value="{{ old('email', $email ?? '') }}" required autocomplete="email"></label>
        <label>{{ __('auth.password') }}<input type="password" name="password" required autocomplete="new-password"></label>
        <label>{{ __('auth.confirm_password') }}<input type="password" name="password_confirmation" required autocomplete="new-password"></label>
        <button class="btn block">{{ __('ui.nav.register') }}</button>
    </form>
    <p class="center">{{ __('auth.have_account') }} <a href="{{ route('login') }}">{{ __('ui.nav.login') }}</a></p>
</main>
</body>
</html>
