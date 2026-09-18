<?php

namespace App\Http\Controllers;

use App\Http\Controllers\Concerns\ScopesToInstaller;
use App\Models\Installation;
use App\Models\ServiceVisit;
use App\Services\InstallationRegistrar;
use App\Support\Uploads;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;

class VisitController extends Controller
{
    use ScopesToInstaller;

    public function __construct(private readonly InstallationRegistrar $registrar) {}

    public function store(Request $request, Installation $installation): RedirectResponse
    {
        $this->mine($request, $installation);

        $data = $request->validate([
            'kind' => ['required', Rule::in(ServiceVisit::KINDS)],
            'performed_on' => ['required', 'date', 'before_or_equal:today'],
            'notes' => ['nullable', 'string', 'max:2000'],
            'price' => ['nullable', 'numeric', 'min:0', 'max:100000'],
            'photo' => ['nullable', 'image', 'max:'.config('installbook.uploads.max_kb')],
        ]);

        $data['photo_path'] = Uploads::store($request->file('photo'), 'visits');

        $visit = $this->registrar->recordVisit($installation, $data);

        return back()->with('status', $visit->kind === ServiceVisit::KIND_SERVICE
            ? __('flash.service_recorded', ['date' => $installation->fresh()->next_service_due_on?->format('d/m/Y')])
            : __('flash.visit_recorded'));
    }
}
