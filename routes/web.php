<?php

use App\Http\Controllers\Auth\AuthController;
use App\Http\Controllers\BookingController;
use App\Http\Controllers\CardController;
use App\Http\Controllers\CustomerController;
use App\Http\Controllers\DashboardController;
use App\Http\Controllers\InstallationController;
use App\Http\Controllers\OutboxController;
use App\Http\Controllers\PlateController;
use App\Http\Controllers\SettingsController;
use App\Http\Controllers\VisitController;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| Public
|--------------------------------------------------------------------------
| The customer never signs in. Their card lives behind an unguessable token,
| which is the link they were sent and can keep for as long as the unit lasts.
*/

Route::get('/', [CardController::class, 'landing'])->name('home');

Route::prefix('c/{token}')->group(function () {
    Route::get('/', [CardController::class, 'show'])->name('card.show');
    Route::get('/print', [CardController::class, 'print'])->name('card.print');
    Route::get('/book', [CardController::class, 'book'])->name('card.book');
    Route::get('/stop', [CardController::class, 'optOutForm'])->name('card.stop');

    // The two routes that write. Throttled because they are the only unauthenticated
    // writes in the application and the token is the only thing guarding them.
    Route::middleware('throttle:10,1')->group(function () {
        Route::post('/book', [CardController::class, 'storeBooking'])->name('card.book.store');
        Route::post('/stop', [CardController::class, 'optOut'])->name('card.stop.store');
    });
});

/*
|--------------------------------------------------------------------------
| Installer
|--------------------------------------------------------------------------
*/

Route::middleware('guest')->group(function () {
    Route::get('/register', [AuthController::class, 'showRegister'])->name('register');
    Route::post('/register', [AuthController::class, 'register']);
    Route::get('/login', [AuthController::class, 'showLogin'])->name('login');
    Route::post('/login', [AuthController::class, 'login']);
});

Route::post('/logout', [AuthController::class, 'logout'])->middleware('auth')->name('logout');

Route::middleware('auth')->group(function () {
    Route::get('/dashboard', [DashboardController::class, 'index'])->name('dashboard');
    Route::get('/due', [DashboardController::class, 'due'])->name('due');
    Route::get('/to-register', [DashboardController::class, 'toRegister'])->name('to-register');

    // The capture flow. "create" is the screen the whole product is built around.
    Route::get('/installs', [InstallationController::class, 'index'])->name('installations.index');
    Route::get('/installs/new', [InstallationController::class, 'create'])->name('installations.create');
    Route::post('/installs', [InstallationController::class, 'store'])->name('installations.store');
    Route::post('/installs/read-plate', [PlateController::class, 'read'])->name('plate.read');

    Route::get('/installs/{installation}', [InstallationController::class, 'show'])->name('installations.show');
    Route::get('/installs/{installation}/edit', [InstallationController::class, 'edit'])->name('installations.edit');
    Route::put('/installs/{installation}', [InstallationController::class, 'update'])->name('installations.update');
    Route::post('/installs/{installation}/archive', [InstallationController::class, 'archive'])->name('installations.archive');
    Route::post('/installs/{installation}/registered', [InstallationController::class, 'markRegistered'])->name('installations.registered');
    Route::post('/installs/{installation}/card', [InstallationController::class, 'resendCard'])->name('installations.card');
    Route::post('/installs/{installation}/plates', [PlateController::class, 'store'])->name('plates.store');
    Route::delete('/plates/{plate}', [PlateController::class, 'destroy'])->name('plates.destroy');
    Route::post('/installs/{installation}/visits', [VisitController::class, 'store'])->name('visits.store');

    Route::get('/customers', [CustomerController::class, 'index'])->name('customers.index');
    Route::get('/customers/{customer}', [CustomerController::class, 'show'])->name('customers.show');
    Route::get('/customers/{customer}/export', [CustomerController::class, 'export'])->name('customers.export');
    Route::post('/customers/{customer}/consent', [CustomerController::class, 'consent'])->name('customers.consent');
    Route::delete('/customers/{customer}', [CustomerController::class, 'destroy'])->name('customers.destroy');

    Route::get('/outbox', [OutboxController::class, 'index'])->name('outbox.index');
    Route::post('/outbox/{message}/sent', [OutboxController::class, 'markSent'])->name('outbox.sent');
    Route::post('/outbox/{message}/retry', [OutboxController::class, 'retry'])->name('outbox.retry');
    Route::delete('/outbox/{message}', [OutboxController::class, 'destroy'])->name('outbox.destroy');

    Route::get('/bookings', [BookingController::class, 'index'])->name('bookings.index');
    Route::post('/bookings/{booking}', [BookingController::class, 'update'])->name('bookings.update');

    Route::get('/settings', [SettingsController::class, 'edit'])->name('settings.edit');
    Route::put('/settings', [SettingsController::class, 'update'])->name('settings.update');
});
