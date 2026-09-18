<?php

namespace App\Http\Controllers;

use App\Enums\ApplianceType;
use App\Http\Controllers\Concerns\ScopesToInstaller;
use App\Http\Requests\StoreInstallationRequest;
use App\Http\Requests\UpdateInstallationRequest;
use App\Models\Installation;
use App\Services\InstallationRegistrar;
use App\Services\Messaging\MessageDispatcher;
use App\Services\ReminderPlanner;
use App\Support\Uploads;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class InstallationController extends Controller
{
    use ScopesToInstaller;

    public function __construct(
        private readonly InstallationRegistrar $registrar,
        private readonly MessageDispatcher $dispatcher,
        private readonly ReminderPlanner $planner,
    ) {}

    public function index(Request $request): View
    {
        $q = trim((string) $request->query('q', ''));

        $installs = $request->user()->installations()
            ->with('customer')
            ->when($q !== '', function ($query) use ($q) {
                $like = '%'.str_replace('%', '\%', $q).'%';
                // Searching by serial goes through the folded key, so a query typed with the
                // characters a person confuses still lands on the unit that was recorded.
                $serialKey = \App\Support\Serial::key($q);
                $query->where(function ($sub) use ($like, $serialKey) {
                    $sub->whereRaw('UPPER(serial) LIKE UPPER(?)', [$like])
                        ->when($serialKey, fn ($s) => $s->orWhere('serial_key', 'like', '%'.$serialKey.'%'))
                        ->orWhereRaw('UPPER(brand) LIKE UPPER(?)', [$like])
                        ->orWhereRaw('UPPER(model) LIKE UPPER(?)', [$like])
                        ->orWhereRaw('UPPER(location_note) LIKE UPPER(?)', [$like])
                        ->orWhereHas('customer', function ($c) use ($like) {
                            $c->whereRaw('UPPER(name) LIKE UPPER(?)', [$like])
                                ->orWhere('phone', 'like', $like)
                                ->orWhereRaw('UPPER(address) LIKE UPPER(?)', [$like]);
                        });
                });
            })
            ->when($request->query('status') === 'archived',
                fn ($query) => $query->where('status', Installation::STATUS_ARCHIVED),
                fn ($query) => $query->active(),
            )
            ->latest('installed_on')
            ->paginate(25)
            ->withQueryString();

        return view('installations.index', [
            'installs' => $installs,
            'q' => $q,
            'archived' => $request->query('status') === 'archived',
        ]);
    }

    /** The screen the product lives or dies on. */
    public function create(Request $request): View
    {
        $user = $request->user();

        return view('installations.create', [
            'types' => ApplianceType::ordered(),
            'lastCustomer' => $user->customers()->latest()->first(),
            'ocrEnabled' => app(\App\Services\Plate\PlateReader::class)->isEnabled(),
            'consentText' => __('consent.statement', ['business' => $user->displayName()]),
        ]);
    }

    public function store(StoreInstallationRequest $request): RedirectResponse
    {
        $data = $request->validated();

        $data['plate_photo_path'] = Uploads::store($request->file('plate_photo'), 'plates');
        $data['unit_photo_path'] = Uploads::store($request->file('unit_photo'), 'units');

        if ($reading = $request->input('plate_reading')) {
            $decoded = json_decode($reading, true);
            $data['plate_reading'] = is_array($decoded) ? $decoded : null;
        }

        $installation = $this->registrar->register($request->user(), $data);

        return redirect()
            ->route('installations.show', $installation)
            ->with('status', __('flash.install_saved'));
    }

    public function show(Request $request, Installation $installation): View
    {
        $this->mine($request, $installation);

        $installation->load(['customer', 'visits', 'plates', 'bookingRequests']);

        return view('installations.show', [
            'install' => $installation,
            'reminders' => $installation->reminders()->orderBy('fire_on')->get(),
            'messages' => $installation->messages()->latest()->limit(10)->get(),
            'requiresTap' => $this->dispatcher->requiresInstallerAction(),
        ]);
    }

    public function edit(Request $request, Installation $installation): View
    {
        $this->mine($request, $installation);

        return view('installations.edit', [
            'install' => $installation->load('customer'),
            'types' => ApplianceType::ordered(),
        ]);
    }

    public function update(UpdateInstallationRequest $request, Installation $installation): RedirectResponse
    {
        $this->mine($request, $installation);

        $data = $request->validated();

        if ($path = Uploads::store($request->file('plate_photo'), 'plates')) {
            $data['plate_photo_path'] = $path;
        }

        $installation->fill($data);
        $installation->save();

        // Dates and reminders are derived, never typed, so they are recomputed here.
        $this->registrar->refreshSchedule($installation);

        return redirect()
            ->route('installations.show', $installation)
            ->with('status', __('flash.install_updated'));
    }

    public function archive(Request $request, Installation $installation): RedirectResponse
    {
        $this->mine($request, $installation);

        $installation->update(['status' => Installation::STATUS_ARCHIVED]);
        $this->planner->cancelAll($installation, 'archived');

        return redirect()
            ->route('installations.index')
            ->with('status', __('flash.install_archived'));
    }

    public function markRegistered(Request $request, Installation $installation): RedirectResponse
    {
        $this->mine($request, $installation);

        $installation->update([
            'manufacturer_registered_at' => $installation->manufacturer_registered_at ? null : now(),
        ]);

        return back()->with('status', $installation->manufacturer_registered_at
            ? __('flash.marked_registered')
            : __('flash.marked_unregistered'));
    }

    public function resendCard(Request $request, Installation $installation): RedirectResponse
    {
        $this->mine($request, $installation);

        $message = $this->dispatcher->sendCard($installation->load(['customer', 'user']));

        if ($message === null) {
            return back()->withErrors(['card' => __('flash.no_customer')]);
        }

        return back()->with('status', $message->status === \App\Models\Message::STATUS_SKIPPED
            ? __('flash.card_skipped')
            : __('flash.card_queued'));
    }
}
