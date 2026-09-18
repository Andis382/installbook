@extends('layouts.app')
@section('title', __('ui.due.title'))

@section('content')
<div class="page-head">
    <span class="micro">{{ __('ui.app_name') }}</span>
    <h1>{{ __('ui.due.title') }}</h1>
</div>

<div class="filters">
    @foreach ([30, 60, 90, 365] as $days)
        <a class="filter" href="{{ route('due', ['days' => $days]) }}"
           @if ($horizon === $days) aria-current="true" @endif>{{ __('ui.due.horizon', ['days' => $days]) }}</a>
    @endforeach
</div>

@if ($overdue->isNotEmpty())
    <h2>{{ __('ui.due.overdue') }} <span class="num muted">{{ $overdue->count() }}</span></h2>
    <div class="panel flush lead-danger">
        <div class="rows">
            @each('partials.due-item', $overdue, 'install')
        </div>
    </div>
@endif

<h2>{{ __('ui.due.upcoming') }} <span class="num muted">{{ $upcoming->count() }}</span></h2>
<div class="panel flush">
    <div class="rows">
        @forelse ($upcoming as $install)
            @include('partials.due-item', ['install' => $install])
        @empty
            <div class="empty">
                <x-icon name="check-circle" size="40" />
                <p>{{ __('ui.due.none') }}</p>
            </div>
        @endforelse
    </div>
</div>
@endsection
