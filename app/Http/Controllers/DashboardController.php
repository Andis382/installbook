<?php

namespace App\Http\Controllers;

use App\Models\BookingRequest;
use App\Models\Installation;
use App\Models\Message;
use Illuminate\Http\Request;
use Illuminate\Support\Carbon;
use Illuminate\View\View;

class DashboardController extends Controller
{
    public function index(Request $request): View
    {
        $user = $request->user();
        $today = Carbon::today();

        $installs = $user->installations();

        $dueSoon = (clone $installs)->active()
            ->with('customer')
            ->dueBy($today->copy()->addDays(30))
            ->orderBy('next_service_due_on')
            ->limit(8)
            ->get();

        $toRegister = (clone $installs)->awaitingRegistration()
            ->with('customer')
            ->orderBy('registration_deadline_on')
            ->limit(8)
            ->get();

        return view('dashboard', [
            'counts' => [
                'installs' => (clone $installs)->active()->count(),
                'due_30' => (clone $installs)->active()->dueBy($today->copy()->addDays(30))->count(),
                'overdue' => (clone $installs)->active()->dueBy($today->copy()->subDay())->count(),
                'to_register' => (clone $installs)->awaitingRegistration()->count(),
                'outbox' => $user->messages()->where('status', Message::STATUS_QUEUED)->count(),
                'bookings' => $user->bookingRequests()->where('status', BookingRequest::STATUS_NEW)->count(),
            ],
            'dueSoon' => $dueSoon,
            'toRegister' => $toRegister,
            'recent' => (clone $installs)->with('customer')->latest()->limit(6)->get(),
            'today' => $today,
        ]);
    }

    /** The work calendar: everything that needs a visit, oldest first. */
    public function due(Request $request): View
    {
        $today = Carbon::today();
        $horizon = (int) $request->integer('days', 60);
        $horizon = max(7, min(365, $horizon));

        $installs = $request->user()->installations()
            ->active()
            ->with(['customer', 'bookingRequests' => fn ($q) => $q->where('status', BookingRequest::STATUS_NEW)])
            ->dueBy($today->copy()->addDays($horizon))
            ->orderBy('next_service_due_on')
            ->get();

        return view('due', [
            'installs' => $installs,
            'horizon' => $horizon,
            'today' => $today,
            'overdue' => $installs->filter(fn (Installation $i) => $i->isOverdue($today)),
            'upcoming' => $installs->reject(fn (Installation $i) => $i->isOverdue($today)),
        ]);
    }

    /** Units whose manufacturer registration window is still open. */
    public function toRegister(Request $request): View
    {
        $installs = $request->user()->installations()
            ->awaitingRegistration()
            ->with('customer')
            ->orderBy('registration_deadline_on')
            ->get();

        return view('to-register', [
            'installs' => $installs,
            'today' => Carbon::today(),
        ]);
    }
}
