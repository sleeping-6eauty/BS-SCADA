<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import loginBg from '@/assets/login-bg.png'
import loginLeftLogo from '@/assets/login-left.png'
import loginRightLogo from '@/assets/login-right.png'
import hidePw from '@/assets/hide-pw.png'
import showPw from '@/assets/show-pw.png'
import { login } from '@/api/auth.js'

const router = useRouter()

// 이미지
const loginLeftLogoSrc = loginLeftLogo
const loginRightLogoSrc = loginRightLogo
const loginBgSrc = loginBg

// 폼 상태
const email = ref('')
const password = ref('')
const showPassword = ref(false)
const loading = ref(false)
const errorMsg = ref('')

const passwordIconSrc = computed(() => (showPassword.value ? hidePw : showPw))

const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

async function handleLogin() {
  if (!email.value || !password.value) {
    errorMsg.value = '이메일과 비밀번호를 입력해주세요.'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await login(email.value, password.value)
    localStorage.setItem('token', data.accessToken)
    localStorage.setItem('user', JSON.stringify({ id: data.id, username: data.username, role: data.role }))
    router.push('/dashboard')
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page login-page">
    <section class="brand-panel" aria-label="설비 알람 관리 시스템 소개">
      <div class="brand-bg" :style="{ backgroundImage: `url(${loginBgSrc})` }" aria-hidden="true"></div>
      <div class="brand-copy">
        <img class="brand-logo-left" :src="loginLeftLogoSrc" alt="로그인 로고" aria-hidden="true" />
        <h1>설비 알람 관리 시스템</h1>
        <p>실시간 모니터링으로 설비의 안정성과 효율성을 높입니다.</p>
      </div>
    </section>

    <section class="form-panel" aria-label="로그인">
      <form class="auth-card" @submit.prevent="handleLogin">
        <div class="form-heading">
          <img class="brand-logo-right" :src="loginRightLogoSrc" alt="로그인 로고" aria-hidden="true" />
          <h2>로그인</h2>
          <p>계정으로 로그인하여 시스템을 이용하세요.</p>
        </div>

        <label class="field-label" for="user-id">이메일</label>
        <div class="input-wrap">
          <span class="input-icon user" aria-hidden="true"></span>
          <input
            id="user-id"
            type="email"
            v-model="email"
            placeholder="이메일을 입력하세요"
            autocomplete="username"
          />
        </div>

        <label class="field-label" for="password">비밀번호</label>
        <div class="input-wrap password-wrap">
          <span class="input-icon lock" aria-hidden="true"></span>
          <input
            id="password"
            :type="showPassword ? 'text' : 'password'"
            v-model="password"
            placeholder="비밀번호를 입력하세요"
            autocomplete="current-password"
          />
          <button
            class="ghost-icon eye"
            type="button"
            @click="togglePasswordVisibility"
            :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 보기'"
          >
            <img class="eye-icon" :src="passwordIconSrc" alt="비밀번호 토글" />
          </button>
        </div>

        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

        <button class="primary-button" type="submit" :disabled="loading">
          {{ loading ? '로그인 중...' : '로그인' }}
        </button>

        <p class="auth-link">
          계정이 없으신가요?
          <RouterLink to="/signup">회원가입</RouterLink>
        </p>
      </form>

      <p class="copyright">Copyright © 2024 Alarm Management System. All rights reserved.</p>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  --navy: #071f49;
  --deep-navy: #031b43;
  --blue: #075ff0;
  --blue-bright: #0a6fff;
  --line: #d9e2ef;
  --muted: #7584a3;
  display: grid;
  grid-template-columns: minmax(380px, 36vw) minmax(0, 1fr);
  min-width: 0;
  min-height: 100vh;
  background: #f7f9fd;
  color: #0b1b46;
}

.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
  padding: clamp(46px, 7vh, 78px) clamp(34px, 4vw, 52px) 44px;
  color: #fff;
  background:
    radial-gradient(circle at 28% 42%, rgba(16, 104, 214, 0.42), transparent 31%),
    linear-gradient(180deg, rgba(2, 24, 60, 0.88), rgba(1, 19, 48, 0.98)),
    #031b43;
}

.brand-bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  opacity: 0.45;
  filter: saturate(0.8) brightness(0.75);
  z-index: 0;
}

.brand-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(32, 131, 255, 0.18) 1px, transparent 1px),
    linear-gradient(90deg, rgba(32, 131, 255, 0.18) 1px, transparent 1px);
  background-size: 74px 74px;
  opacity: 0.18;
  transform: perspective(600px) rotateX(56deg) translateY(18%);
  transform-origin: bottom;
}

.brand-panel::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(0, 16, 45, 0.38), rgba(4, 42, 94, 0.14));
  pointer-events: none;
}

.brand-copy {
  position: relative;
  z-index: 1;
  margin-bottom: 28px;
  text-align: center;
}

.brand-logo-left,
.brand-logo-right {
  width: clamp(66px, 7vw, 82px);
  height: auto;
  object-fit: contain;
  display: block;
  margin: 0 auto 18px;
}

.password-wrap {
  position: relative;
}

.ghost-icon.eye {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: #4a6d9f;
  background: rgba(7, 65, 150, 0.06);
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
}

.ghost-icon.eye .eye-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.ghost-icon.eye:hover {
  background: rgba(7, 65, 150, 0.12);
}

.line-robot {
  display: inline-grid;
  place-items: center;
}

.line-robot svg {
  width: 78px;
  height: 78px;
  fill: none;
  stroke: currentColor;
  stroke-width: 4;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.mark-light {
  color: rgba(255, 255, 255, 0.88);
}

.mark-blue {
  color: var(--blue);
}

.brand-copy h1 {
  margin: 18px 0 12px;
  font-size: clamp(28px, 2.6vw, 34px);
  font-weight: 950;
  letter-spacing: 0;
}

.brand-copy p {
  margin: 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: clamp(16px, 1.45vw, 18px);
  font-weight: 650;
}


.feature-row {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  margin: auto 0 0;
  padding: 0;
  list-style: none;
}

.feature-row li {
  display: grid;
  gap: 9px;
  justify-items: center;
  min-height: 112px;
  padding: 0 20px;
  text-align: center;
  border-right: 1px solid rgba(120, 180, 255, 0.36);
}

.feature-row li:last-child {
  border-right: 0;
}

.feature-row strong {
  font-size: 17px;
  font-weight: 900;
}

.feature-row small {
  color: rgba(255, 255, 255, 0.78);
  font-size: 14px;
  font-weight: 650;
}

.feature-icon {
  position: relative;
  width: 54px;
  height: 54px;
  color: #68b8ff;
}

.feature-icon::before,
.feature-icon::after {
  content: '';
  position: absolute;
  inset: 8px;
  border: 4px solid currentColor;
}

.feature-icon.shield::before {
  border-radius: 8px 8px 18px 18px;
  transform: perspective(60px) rotateX(8deg);
}

.feature-icon.shield::after {
  inset: 21px 17px 18px 17px;
  border-width: 0 0 4px 4px;
  transform: rotate(-45deg);
}

.feature-icon.bell::before {
  top: 8px;
  height: 30px;
  border-radius: 24px 24px 12px 12px;
}

.feature-icon.bell::after {
  inset: auto 20px 6px;
  width: 14px;
  height: 8px;
  border-radius: 999px;
  background: currentColor;
  border: 0;
}

.feature-icon.chart::before {
  inset: auto 8px 8px;
  width: 38px;
  height: 34px;
  border-width: 0 0 4px 4px;
}

.feature-icon.chart::after {
  inset: 10px 11px 12px;
  border-width: 0 4px 4px 0;
  transform: skewY(-16deg);
}

.form-panel {
  position: relative;
  display: grid;
  place-items: center;
  padding: clamp(26px, 4vh, 42px) clamp(32px, 5vw, 56px) 58px;
}

.chevron,
.input-icon,
.ghost-icon {
  position: relative;
  flex: none;
  width: 24px;
  height: 24px;
  color: #273d73;
}

.chevron::before {
  content: '';
  position: absolute;
  left: 7px;
  top: 8px;
  width: 9px;
  height: 9px;
  border-right: 3px solid currentColor;
  border-bottom: 3px solid currentColor;
  transform: rotate(45deg);
}

.auth-card {
  width: min(560px, 100%);
  padding: clamp(36px, 5.8vh, 54px) clamp(42px, 5vw, 56px) clamp(34px, 5vh, 48px);
  border: 1px solid #dfe6f1;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 18px 50px rgba(20, 42, 83, 0.08);
}

.form-heading {
  margin-bottom: 34px;
  text-align: center;
}

.form-heading h2 {
  margin: 16px 0 8px;
  font-size: clamp(28px, 2.5vw, 32px);
  font-weight: 950;
  letter-spacing: 0;
  color: #071b54;
}

.form-heading p {
  margin: 0;
  color: var(--muted);
  font-size: 16px;
  font-weight: 700;
}

.field-label {
  display: block;
  margin: 0 0 10px;
  color: #0a1b4d;
  font-size: 16px;
  font-weight: 900;
}

.input-wrap {
  display: flex;
  align-items: center;
  height: 54px;
  margin-bottom: 22px;
  padding: 0 16px;
  border: 1px solid #ccd7e7;
  border-radius: 9px;
  background: #fff;
  box-shadow: inset 0 1px 0 rgba(16, 49, 98, 0.02);
  position: relative;
}

.input-wrap:focus-within {
  border-color: #1d6fff;
  box-shadow: 0 0 0 4px rgba(13, 102, 255, 0.1);
}

.input-wrap input {
  width: 100%;
  min-width: 0;
  height: 100%;
  border: 0;
  outline: 0;
  padding: 0 44px 0 14px;
  color: #102653;
  font-size: 17px;
  font-weight: 700;
  background: transparent;
}

.input-wrap input::placeholder {
  color: #8a98b2;
}

.input-icon.user::before {
  content: '';
  position: absolute;
  left: 7px;
  top: 3px;
  width: 10px;
  height: 10px;
  border: 3px solid currentColor;
  border-radius: 50%;
}

.input-icon.user::after {
  content: '';
  position: absolute;
  left: 3px;
  bottom: 2px;
  width: 18px;
  height: 11px;
  border: 3px solid currentColor;
  border-bottom: 0;
  border-radius: 14px 14px 0 0;
}

.input-icon.lock::before {
  content: '';
  position: absolute;
  left: 4px;
  bottom: 2px;
  width: 18px;
  height: 15px;
  border: 3px solid currentColor;
  border-radius: 4px;
}

.input-icon.lock::after {
  content: '';
  position: absolute;
  left: 8px;
  top: 1px;
  width: 10px;
  height: 12px;
  border: 3px solid currentColor;
  border-bottom: 0;
  border-radius: 12px 12px 0 0;
}

.ghost-icon {
  border: 0;
  background: transparent;
}


.auth-link a {
  color: var(--blue);
  font-weight: 900;
}

.error-msg {
  margin: 0 0 16px;
  padding: 11px 16px;
  border-radius: 8px;
  background: #fff0f0;
  border: 1px solid #ffcdd2;
  color: #c62828;
  font-size: 15px;
  font-weight: 700;
  text-align: center;
}

.primary-button {
  width: 100%;
  height: 56px;
  border: 0;
  border-radius: 9px;
  color: #fff;
  background: linear-gradient(180deg, #086bff, #0753dc);
  box-shadow: 0 12px 22px rgba(6, 88, 231, 0.23);
  font-size: 19px;
  font-weight: 950;
  cursor: pointer;
}

.primary-button:hover {
  background: linear-gradient(180deg, #1677ff, #075ce9);
}

.primary-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.auth-link {
  margin: 24px 0 0;
  color: #7584a3;
  text-align: center;
  font-size: 16px;
  font-weight: 800;
}

.copyright {
  position: absolute;
  bottom: 24px;
  margin: 0;
  color: #8492ad;
  font-size: 15px;
  font-weight: 650;
}

@media (max-width: 1280px) {
  .auth-page {
    grid-template-columns: 390px minmax(0, 1fr);
  }

  .brand-panel {
    padding-left: 44px;
    padding-right: 44px;
  }

  .auth-card {
    width: 520px;
    padding-left: 42px;
    padding-right: 42px;
  }
}

@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    min-height: 220px;
    padding: 34px 28px;
  }

  .form-panel {
    padding: 28px 20px 54px;
  }
}
</style>
