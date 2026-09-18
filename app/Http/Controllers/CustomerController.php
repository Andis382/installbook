<?php

namespace App\Http\Controllers;

use App\Http\Controllers\Concerns\ScopesToInstaller;
use App\Models\Customer;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class CustomerController extends Controller
{
    use ScopesToInstaller;

    public function index(Request $request): View
    {
        $q = trim((string) $request->query('q', ''));

        $customers = $request->user()->customers()
            ->withCount('installations')
            ->when($q !== '', function ($query) use ($q) {
                $like = '%'.str_replace('%', '\%', $q).'%';
                $query->whereRaw('UPPER(name) LIKE UPPER(?)', [$like])
                    ->orWhere('phone', 'like', $like)
                    ->orWhereRaw('UPPER(address) LIKE UPPER(?)', [$like]);
            })
            ->orderBy('name')
            ->paginate(30)
            ->withQueryString();

        return view('customers.index', ['customers' => $customers, 'q' => $q]);
    }

    public function show(Request $request, Customer $customer): View
    {
        $this->mine($request, $customer);

        return view('customers.show', [
            'customer' => $customer,
            'installs' => $customer->installations()->latest('installed_on')->get(),
        ]);
    }

    /**
     * Everything held about one person, in one file. A person has the right to ask for
     * this, and an installer should be able to answer in one tap rather than by promising
     * to look through his phone.
     */
    public function export(Request $request, Customer $customer): JsonResponse
    {
        $this->mine($request, $customer);

        $customer->load(['installations.visits', 'installations.plates']);

        $payload = [
            'exported_at' => now()->toIso8601String(),
            'installer' => $request->user()->displayName(),
            'customer' => [
                'name' => $customer->name,
                'phone' => $customer->phone,
                'address' => $customer->address,
                'city' => $customer->city,
                'location' => $customer->lat ? ['lat' => $customer->lat, 'lng' => $customer->lng] : null,
                'language' => $customer->locale,
                'messaging_consent_at' => $customer->messaging_consent_at?->toIso8601String(),
                'consent_text' => $customer->consent_text,
                'consent_method' => $customer->consent_method,
                'opted_out_at' => $customer->opted_out_at?->toIso8601String(),
                'created_at' => $customer->created_at->toIso8601String(),
            ],
            'installations' => $customer->installations->map(fn ($i) => [
                'type' => $i->appliance_type,
                'brand' => $i->brand,
                'model' => $i->model,
                'serial' => $i->serial,
                'installed_on' => $i->installed_on?->toDateString(),
                'warranty_expires_on' => $i->warranty_expires_on?->toDateString(),
                'statutory_guarantee_until' => $i->statutoryGuaranteeUntil()?->toDateString(),
                'next_service_due_on' => $i->next_service_due_on?->toDateString(),
                'card_url' => route('card.show', $i->public_token),
                'plates' => $i->plates->map(fn ($p) => [
                    'role' => $p->role, 'brand' => $p->brand, 'model' => $p->model, 'serial' => $p->serial,
                ]),
                'visits' => $i->visits->map(fn ($v) => [
                    'kind' => $v->kind,
                    'performed_on' => $v->performed_on->toDateString(),
                    'notes' => $v->notes,
                ]),
            ]),
            'messages' => $request->user()->messages()
                ->where('customer_id', $customer->id)
                ->get(['template_key', 'locale', 'body', 'status', 'sent_at', 'created_at']),
        ];

        return response()->json($payload, 200, [
            'Content-Disposition' => 'attachment; filename="customer-'.$customer->id.'.json"',
        ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    }

    /** Record or withdraw permission to message this person. */
    public function consent(Request $request, Customer $customer): RedirectResponse
    {
        $this->mine($request, $customer);

        if ($request->boolean('granted')) {
            $customer->update([
                'messaging_consent_at' => $customer->messaging_consent_at ?? now(),
                'consent_text' => $customer->consent_text ?? __('consent.statement', ['business' => $request->user()->displayName()], $customer->locale),
                'consent_method' => $customer->consent_method ?? 'installer_recorded',
                'opted_out_at' => null,
            ]);

            return back()->with('status', __('flash.consent_recorded'));
        }

        $customer->update(['opted_out_at' => now()]);

        return back()->with('status', __('flash.consent_withdrawn'));
    }

    /**
     * Erasing a person removes their installations, visits and messages with them. The
     * installer is warned that this also destroys the proof of what he fitted and when.
     */
    public function destroy(Request $request, Customer $customer): RedirectResponse
    {
        $this->mine($request, $customer);

        $request->validate(['confirm' => ['required', 'accepted']]);

        $request->user()->messages()->where('customer_id', $customer->id)->delete();
        $customer->delete();

        return redirect()->route('customers.index')->with('status', __('flash.customer_deleted'));
    }
}
