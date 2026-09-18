<?php

namespace App\Http\Controllers;

use App\Support\Phone;
use App\Support\Uploads;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;
use Illuminate\View\View;

class SettingsController extends Controller
{
    public function edit(Request $request): View
    {
        return view('settings', [
            'user' => $request->user(),
            'driver' => config('installbook.messaging.driver'),
            'ocr' => config('installbook.ocr.driver'),
        ]);
    }

    public function update(Request $request): RedirectResponse
    {
        $user = $request->user();

        $data = $request->validate([
            'name' => ['required', 'string', 'max:120'],
            'business_name' => ['nullable', 'string', 'max:120'],
            'phone' => ['nullable', 'string', 'max:32'],
            'city' => ['nullable', 'string', 'max:120'],
            'registration_number' => ['nullable', 'string', 'max:60'],
            'locale' => ['required', Rule::in(array_keys(config('installbook.locales')))],
            'card_footer' => ['nullable', 'string', 'max:500'],
            'reminder_lead_days' => ['required', 'integer', 'between:7,84'],
            'registration_window_days' => ['required', 'integer', 'between:0,180'],
            'logo' => ['nullable', 'image', 'max:2048'],
        ]);

        $data['phone'] = Phone::normalize($data['phone'] ?? null, config('installbook.defaults.country_prefix'));

        if ($path = Uploads::store($request->file('logo'), 'logos')) {
            Uploads::delete($user->logo_path);
            $data['logo_path'] = $path;
        }
        unset($data['logo']);

        $user->update($data);

        return back()->with('status', __('flash.settings_saved'));
    }
}
