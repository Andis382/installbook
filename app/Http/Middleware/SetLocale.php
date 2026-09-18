<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\App;
use Symfony\Component\HttpFoundation\Response;

/**
 * The interface follows the installer's language. Customer-facing pages set their own
 * locale from the customer record instead, so an Albanian installer can hand an Italian
 * customer a card in Italian.
 */
class SetLocale
{
    public function handle(Request $request, Closure $next): Response
    {
        $available = array_keys(config('installbook.locales'));

        $locale = $request->user()?->locale
            ?? $request->session()->get('locale')
            ?? config('installbook.defaults.locale');

        if (in_array($locale, $available, true)) {
            App::setLocale($locale);
        }

        return $next($request);
    }
}
