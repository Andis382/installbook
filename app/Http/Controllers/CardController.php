<?php

namespace App\Http\Controllers;

use App\Models\BookingRequest;
use App\Models\Installation;
use App\Services\Messaging\MessageDispatcher;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\App;
use Illuminate\Validation\Rule;
use Illuminate\View\View;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

/**
 * The customer's side. No account, no password, no app: one link, held for as long as
 * the machine lasts, that proves what was installed and when, and has a button that puts
 * a service request in front of the installer.
 */
class CardController extends Controller
{
    public function landing(Request $request): View|RedirectResponse
    {
        if ($request->user()) {
            return redirect()->route('dashboard');
        }

        return view('landing');
    }

    public function show(string $token): View
    {
        $install = $this->resolve($token);

        // Knowing the customer opened it is worth recording: it is what tells the installer
        // the card arrived, and the first thing that convinces him any of this works.
        $install->forceFill([
            'card_view_count' => $install->card_view_count + 1,
            'card_first_viewed_at' => $install->card_first_viewed_at ?? now(),
        ])->saveQuietly();

        return view('card.show', $this->cardData($install));
    }

    /** Same card, laid out for paper. A customer who prints it has their proof offline. */
    public function print(string $token): View
    {
        $install = $this->resolve($token);

        return view('card.print', $this->cardData($install));
    }

    public function book(string $token): View
    {
        $install = $this->resolve($token);

        return view('card.book', $this->cardData($install) + [
            'windows' => BookingRequest::WINDOWS,
        ]);
    }

    public function storeBooking(Request $request, string $token): RedirectResponse
    {
        $install = $this->resolve($token);

        $data = $request->validate([
            'preferred_window' => ['nullable', Rule::in(BookingRequest::WINDOWS)],
            'note' => ['nullable', 'string', 'max:500'],
            'contact_phone' => ['nullable', 'string', 'max:32'],
        ]);

        // One open request per unit is enough; asking twice should not create a queue.
        $existing = $install->bookingRequests()->where('status', BookingRequest::STATUS_NEW)->first();

        if ($existing) {
            $existing->update([
                'preferred_window' => $data['preferred_window'] ?? $existing->preferred_window,
                'note' => $data['note'] ?? $existing->note,
            ]);
            $booking = $existing;
        } else {
            $booking = $install->bookingRequests()->create([
                'user_id' => $install->user_id,
                'preferred_window' => $data['preferred_window'] ?? null,
                'note' => $data['note'] ?? null,
                'contact_phone' => $data['contact_phone'] ?? $install->customer?->phone,
                'status' => BookingRequest::STATUS_NEW,
            ]);
        }

        // The customer has asked; there is nothing left to remind them about this cycle.
        app(\App\Services\ReminderPlanner::class)->cancelServiceRemindersFor($install, 'customer_booked');

        app(MessageDispatcher::class)->sendBookingAck($booking);

        return redirect()
            ->route('card.show', $token)
            ->with('status', __('card.booking_received'));
    }

    public function optOutForm(string $token): View
    {
        $install = $this->resolve($token);

        return view('card.stop', $this->cardData($install));
    }

    /**
     * One tap, no login, no confirmation loop. After this the customer keeps the card and
     * stops hearing from the app; nothing more can be queued for them.
     */
    public function optOut(string $token): RedirectResponse
    {
        $install = $this->resolve($token);

        $install->customer?->update(['opted_out_at' => now()]);

        app(\App\Services\ReminderPlanner::class)->cancelAll($install, 'customer_opted_out');

        return redirect()->route('card.show', $token)->with('status', __('card.opted_out_done'));
    }

    private function resolve(string $token): Installation
    {
        $install = Installation::with(['customer', 'user', 'plates', 'visits'])
            ->where('public_token', $token)
            ->first();

        if (! $install) {
            throw new NotFoundHttpException;
        }

        // The card speaks the customer's language, not the installer's.
        $locale = $install->customer?->locale ?? $install->user->locale;
        if (array_key_exists($locale, config('installbook.locales'))) {
            App::setLocale($locale);
        }

        return $install;
    }

    private function cardData(Installation $install): array
    {
        return [
            'install' => $install,
            'installer' => $install->user,
            'customer' => $install->customer,
            'statutoryUntil' => $install->statutoryGuaranteeUntil(),
            'services' => $install->visits->where('kind', '!=', 'install'),
            'openBooking' => $install->bookingRequests()->where('status', BookingRequest::STATUS_NEW)->first(),
        ];
    }
}
