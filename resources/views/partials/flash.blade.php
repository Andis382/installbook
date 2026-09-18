@if (session('status'))
    <div class="flash ok">{{ session('status') }}</div>
@endif

@if ($errors->any())
    <div class="flash err">
        @if ($errors->count() === 1)
            {{ $errors->first() }}
        @else
            <ul>@foreach ($errors->all() as $error)<li>{{ $error }}</li>@endforeach</ul>
        @endif
    </div>
@endif
