<script setup lang="ts">
import BrandMark from './BrandMark.vue'
import LanguageSwitch from './LanguageSwitch.vue'
import AuthAside from '@/components/AuthAside.vue'
</script>

<template>
  <div class="auth">
    <aside class="auth__aside">
      <BrandMark :size="40" />
      <div class="auth__pitch">
        <AuthAside />
      </div>
      <p class="auth__foot">© {{ new Date().getFullYear() }} {{ $t('app.name') }}</p>
    </aside>
    <main class="auth__main">
      <header class="auth__band">
        <div class="auth__band-bar">
          <BrandMark :size="32" />
          <LanguageSwitch inverse />
        </div>
        <p class="auth__band-title">{{ $t('aside.title') }}</p>
      </header>
      <div class="auth__top">
        <LanguageSwitch />
      </div>
      <div class="auth__card">
        <slot />
      </div>
    </main>
  </div>
</template>

<style scoped>
.auth {
  display: grid;
  grid-template-columns: minmax(380px, 5fr) 7fr;
  min-height: 100dvh;
}
.auth__aside {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 40px;
  padding: 40px clamp(28px, 4vw, 56px);
  color: var(--header-text);
  background:
    radial-gradient(700px 420px at 0% 0%, var(--header-glow), transparent 60%),
    radial-gradient(600px 400px at 100% 100%, rgb(255 255 255 / 0.06), transparent 60%),
    linear-gradient(150deg, var(--header-from), var(--header-via) 55%, var(--header-to));
  overflow: hidden;
  isolation: isolate;
}
.auth__aside::before,
.auth__band::before {
  /* the same blueprint grid as the app's band */
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  background-image:
    linear-gradient(var(--header-grid) 1px, transparent 1px),
    linear-gradient(90deg, var(--header-grid) 1px, transparent 1px),
    linear-gradient(rgb(174 213 234 / 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgb(174 213 234 / 0.05) 1px, transparent 1px);
  background-size:
    96px 96px,
    96px 96px,
    24px 24px,
    24px 24px;
  mask-image: radial-gradient(circle at 30% 20%, #000, transparent 80%);
}
.auth__pitch {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.auth__foot {
  font-size: var(--text-xs);
  color: var(--text-inverse-muted);
}
.auth__main {
  display: flex;
  flex-direction: column;
  padding: 28px clamp(20px, 5vw, 64px);
  background:
    radial-gradient(800px 400px at 100% 0%, var(--primary-soft), transparent 60%),
    var(--bg);
}
.auth__top {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}
.auth__band {
  display: none;
}
.auth__card {
  width: 100%;
  max-width: 440px;
  margin: auto;
  padding: 36px 0;
}
/* Phones: the pitch shrinks to a dark band, and the form sits on a card that overlaps it */
@media (max-width: 900px) {
  .auth {
    grid-template-columns: 1fr;
  }
  .auth__aside,
  .auth__top {
    display: none;
  }
  .auth__main {
    padding: 0 0 32px;
  }
  .auth__band {
    position: relative;
    isolation: isolate;
    overflow: hidden;
    display: block;
    padding: 18px 20px 84px;
    color: var(--header-text);
    background:
      radial-gradient(500px 260px at 0% 0%, var(--header-glow), transparent 65%),
      linear-gradient(150deg, var(--header-from), var(--header-via) 55%, var(--header-to));
  }
  .auth__band-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .auth__band-title {
    margin-top: 26px;
    font-family: var(--font-display);
    font-size: 1.9rem;
    font-weight: 700;
    line-height: 1.1;
    letter-spacing: -0.03em;
    max-width: 14ch;
  }
  .auth__card {
    position: relative;
    z-index: 1;
    width: auto;
    max-width: 480px;
    margin: -56px 16px 0;
    padding: 24px 20px;
    border: 1px solid var(--border);
    border-radius: var(--radius-xl);
    background: var(--surface);
    box-shadow: var(--shadow-lg), var(--highlight);
  }
}
@media (min-width: 560px) and (max-width: 900px) {
  .auth__card {
    margin-inline: auto;
  }
}
</style>
