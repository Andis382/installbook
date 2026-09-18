{{--
    Two different things, kept apart.

    A flash is the result of something that worked, announced politely so a
    screen reader hears it without losing its place.

    The summary is what happens when a form failed. It takes focus, so the very
    next thing anyone hears or sees is what went wrong, and every line links to
    the field it belongs to. The inline error stays on the field as well: a
    summary that replaces the field-level message just moves the problem.
--}}

@if (session('status'))
    <div class="notice ok" role="status">
        <x-icon name="check-circle" size="20" />
        <div>{{ session('status') }}</div>
    </div>
@endif

@if ($errors->any())
    <div class="summary" id="error-summary" role="alert" tabindex="-1">
        <h2>{{ trans_choice('ui.a11y.errors', $errors->count(), ['count' => $errors->count()]) }}</h2>
        <ul>
            @foreach ($errors->keys() as $key)
                <li>
                    <a href="#f-{{ str_replace(['[', ']', '.', '_'], ['-', '', '-', '-'], $key) }}">{{ $errors->first($key) }}</a>
                </li>
            @endforeach
        </ul>
    </div>
    @once
        @push('scripts')
        <script>
        (function () {
          var summary = document.getElementById('error-summary');
          if (summary) summary.focus();
          // Clicking a line in the summary should land on the field itself, not
          // three lines above it behind the sticky bar.
          summary.addEventListener('click', function (event) {
            var link = event.target.closest('a[href^="#"]');
            if (!link) return;
            var field = document.getElementById(link.getAttribute('href').slice(1));
            if (field) { event.preventDefault(); field.focus({ preventScroll: false }); }
          });
        })();
        </script>
        @endpush
    @endonce
@endif
