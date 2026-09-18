@extends('layouts.app')
@section('title', __('ui.due.title'))

@section('content')
<h1>{{ __('ui.due.title') }}</h1>

<div class="chips">
    @foreach ([30, 60, 90, 365] as $days)
        <a class="chip {{ $horizon === $days ? 'on' : '' }}" href="{{ route('due', ['days' => $days]) }}">{{ __('ui.due.horizon', ['days' => $days]) }}</a>
    @endforeach
</div>

@if ($overdue->isNotEmpty())
    <h2>{{ __('ui.due.overdue') }} ({{ $overdue->count() }})</h2>
    <div class="card tight list">
        @each('partials.due-item', $overdue, 'install')
    </div>
@endif

<h2>{{ __('ui.due.upcoming') }} ({{ $upcoming->count() }})</h2>
<div class="card tight list">
    @forelse ($upcoming as $install)
        @include('partials.due-item', ['install' => $install])
    @empty
        <div class="empty"><span class="big">✅</span>{{ __('ui.due.none') }}</div>
    @endforelse
</div>
@endsection
