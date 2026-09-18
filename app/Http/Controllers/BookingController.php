<?php

namespace App\Http\Controllers;

use App\Http\Controllers\Concerns\ScopesToInstaller;
use App\Models\BookingRequest;
use App\Models\ServiceVisit;
use App\Services\InstallationRegistrar;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;
use Illuminate\View\View;

class BookingController extends Controller
{
    use ScopesToInstaller;

    public function __construct(private readonly InstallationRegistrar $registrar) {}

    public function index(Request $request): View
    {
        $bookings = $request->user()->bookingRequests()
            ->with(['installation.customer'])
            ->orderByRaw("CASE WHEN status = 'new' THEN 0 ELSE 1 END")
            ->latest()
            ->paginate(25);

        return view('bookings.index', ['bookings' => $bookings]);
    }

    public function update(Request $request, BookingRequest $booking): RedirectResponse
    {
        $this->mine($request, $booking);

        $data = $request->validate([
            'status' => ['required', Rule::in([
                BookingRequest::STATUS_NEW,
                BookingRequest::STATUS_SCHEDULED,
                BookingRequest::STATUS_DONE,
                BookingRequest::STATUS_DECLINED,
            ])],
            'scheduled_for' => ['nullable', 'date'],
        ]);

        $booking->fill([
            'status' => $data['status'],
            'scheduled_for' => $data['scheduled_for'] ?? $booking->scheduled_for,
            'resolved_at' => $data['status'] === BookingRequest::STATUS_NEW ? null : now(),
        ])->save();

        // Marking a booking done is the same event as "the service happened", so the
        // install's clock restarts here rather than making the installer log it twice.
        if ($data['status'] === BookingRequest::STATUS_DONE) {
            $this->registrar->recordVisit($booking->installation, [
                'kind' => ServiceVisit::KIND_SERVICE,
                'performed_on' => ($booking->scheduled_for ?? now())->toDateString(),
                'notes' => __('booking.recorded_from_request'),
            ]);
        }

        return back()->with('status', __('flash.booking_updated'));
    }
}
