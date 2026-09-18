<?php

namespace App\Http\Controllers\Auth;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Support\Phone;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Validation\Rules\Password;
use Illuminate\View\View;

/**
 * Hand-rolled instead of a starter kit, so the repository installs with composer alone
 * and has no Node build step. One account is one installer; there are no seats.
 */
class AuthController extends Controller
{
    public function showRegister(): View
    {
        return view('auth.register');
    }

    public function register(Request $request): RedirectResponse
    {
        $data = $request->validate([
            'name' => ['required', 'string', 'max:120'],
            'business_name' => ['nullable', 'string', 'max:120'],
            'email' => ['required', 'email', 'max:190', 'unique:users,email'],
            'phone' => ['nullable', 'string', 'max:32'],
            'city' => ['nullable', 'string', 'max:120'],
            'locale' => ['nullable', 'string', 'in:'.implode(',', array_keys(config('installbook.locales')))],
            'password' => ['required', 'confirmed', Password::min(8)],
        ]);

        $user = User::create([
            'name' => $data['name'],
            'business_name' => $data['business_name'] ?? null,
            'email' => strtolower($data['email']),
            'phone' => Phone::normalize($data['phone'] ?? null, config('installbook.defaults.country_prefix')),
            'city' => $data['city'] ?? null,
            'locale' => $data['locale'] ?? config('installbook.defaults.locale'),
            'timezone' => config('installbook.defaults.timezone'),
            'password' => $data['password'],
        ]);

        Auth::login($user, remember: true);
        $request->session()->regenerate();

        return redirect()->route('installations.create')->with('status', __('auth.welcome'));
    }

    public function showLogin(): View
    {
        return view('auth.login');
    }

    public function login(Request $request): RedirectResponse
    {
        $credentials = $request->validate([
            'email' => ['required', 'email'],
            'password' => ['required', 'string'],
        ]);

        if (! Auth::attempt($credentials, $request->boolean('remember', true))) {
            return back()
                ->withInput($request->only('email'))
                ->withErrors(['email' => __('auth.failed')]);
        }

        $request->session()->regenerate();

        return redirect()->intended(route('dashboard'));
    }

    public function logout(Request $request): RedirectResponse
    {
        Auth::logout();
        $request->session()->invalidate();
        $request->session()->regenerateToken();

        return redirect()->route('login');
    }
}
