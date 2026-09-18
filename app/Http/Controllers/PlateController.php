<?php

namespace App\Http\Controllers;

use App\Http\Controllers\Concerns\ScopesToInstaller;
use App\Models\Installation;
use App\Models\InstallationPlate;
use App\Services\Plate\PlateReader;
use App\Support\Uploads;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;

class PlateController extends Controller
{
    use ScopesToInstaller;

    /**
     * Read a photographed plate and hand back what could be made out, so the capture form
     * can pre-fill itself. Always returns 200 with whatever was found, including nothing:
     * a failed read is an ordinary outcome here, not an error the installer must handle.
     */
    public function read(Request $request, PlateReader $reader): JsonResponse
    {
        $request->validate([
            'plate_photo' => ['required', 'image', 'max:'.config('installbook.uploads.max_kb')],
            'appliance_type' => ['nullable', 'string', 'max:40'],
        ]);

        $path = Uploads::store($request->file('plate_photo'), 'plates');
        $absolute = Uploads::absolutePath($path);

        $reading = $absolute && $reader->isEnabled()
            ? $reader->read($absolute, $request->input('appliance_type'))
            : \App\Services\Plate\PlateReading::empty($reader->isEnabled() ? 'file_unreadable' : 'ocr_disabled');

        return response()->json([
            'stored_path' => $path,
            'photo_url' => Uploads::url($path),
            'enabled' => $reader->isEnabled(),
            'reading' => $reading->toArray(),
        ]);
    }

    /** A second or third plate on the same installation: outdoor unit, inverter, panel. */
    public function store(Request $request, Installation $installation): RedirectResponse
    {
        $this->mine($request, $installation);

        $data = $request->validate([
            'role' => ['required', Rule::in(InstallationPlate::ROLES)],
            'brand' => ['nullable', 'string', 'max:120'],
            'model' => ['nullable', 'string', 'max:120'],
            'serial' => ['nullable', 'string', 'max:120'],
            'note' => ['nullable', 'string', 'max:160'],
            'photo' => ['nullable', 'image', 'max:'.config('installbook.uploads.max_kb')],
        ]);

        $installation->plates()->create([
            'role' => $data['role'],
            'brand' => $data['brand'] ?? null,
            'model' => $data['model'] ?? null,
            'serial' => isset($data['serial']) ? trim($data['serial']) : null,
            'note' => $data['note'] ?? null,
            'photo_path' => Uploads::store($request->file('photo'), 'plates'),
        ]);

        return back()->with('status', __('flash.plate_added'));
    }

    public function destroy(Request $request, InstallationPlate $plate): RedirectResponse
    {
        $installation = $plate->installation;
        $this->mine($request, $installation);

        Uploads::delete($plate->photo_path);
        $plate->delete();

        return back()->with('status', __('flash.plate_removed'));
    }
}
