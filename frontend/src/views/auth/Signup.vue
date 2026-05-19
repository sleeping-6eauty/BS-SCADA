<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import loginBg from '@/assets/login-bg.png'
import loginLeftLogo from '@/assets/login-left.png'
import loginRightLogo from '@/assets/login-right.png'
import hidePw from '@/assets/hide-pw.png'
import showPw from '@/assets/show-pw.png'
import emailIcon from '@/assets/email.png'
import { signup } from '@/api/auth.js'

const router = useRouter()

// 이미지
const loginBgSrc = loginBg
const loginLeftLogoSrc = loginLeftLogo
const loginRightLogoSrc = loginRightLogo
const emailIconSrc = emailIcon

// 폼 상태
const name = ref('')
const email = ref('')
const password = ref('')
const passwordConfirm = ref('')
const showPassword = ref(false)
const showPasswordConfirm = ref(false)
const loading = ref(false)
const errorMsg = ref('')

const passwordIconSrc = computed(() => (showPassword.value ? hidePw : showPw))
const passwordConfirmIconSrc = computed(() => (showPasswordConfirm.value ? hidePw : showPw))

async function handleSignup() {
  if (!name.value || !email.value || !password.value || !passwordConfirm.value) {
    errorMsg.value = '모든 항목을 입력해주세요.'
    return
  }
  if (password.value !== passwordConfirm.value) {
    errorMsg.value = '비밀번호가 일치하지 않습니다.'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    await signup({ username: name.value, email: email.value, password: password.value })
    router.push('/login')
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page signup-page">
    <section class="brand-panel" aria-label="설비 알람 관리 시스템 소개">
      <div class="brand-bg" :style="{ backgroundImage: `url(${loginBgSrc})` }" aria-hidden="true"></div>
      <div class="brand-copy">
        <img class="brand-logo-left" :src="loginLeftLogoSrc" alt="로그인 로고" aria-hidden="true" />
        <h1>설비 알람 관리 시스템</h1>
        <p>실시간 모니터링으로 설비의 안정성과 효율성을 높입니다.</p>
      </div>
    </section>

    <section class="form-panel" aria-label="회원가입">
      <form class="auth-card" @submit.prevent="handleSignup">
        <div class="form-heading">
          <img class="brand-logo-right" :src="loginRightLogoSrc" alt="회원가입 로고" aria-hidden="true" />
          <h2>회원가입</h2>
          <p>계정을 생성하여 시스템을 이용하세요.</p>
        </div>

        <label class="field-label" for="name">이름</label>
        <div class="input-wrap">
          <span class="input-icon user" aria-hidden="true"></span>
          <input id="name" type="text" v-model="name" placeholder="이름을 입력하세요" autocomplete="name" />
        </div>

        <label class="field-label" for="email">이메일</label>
        <div class="input-wrap">
          <img class="input-icon email-img" :src="emailIconSrc" alt="" aria-hidden="true" />
          <input id="email" type="email" v-model="email" placeholder="이메일을 입력하세요" autocomplete="email" />
        </div>

        <label class="field-label" for="password">비밀번호</label>
        <div class="input-wrap password-wrap">
          <span class="input-icon lock" aria-hidden="true"></span>
          <input id="password" :type="showPassword ? 'text' : 'password'" v-model="password" placeholder="비밀번호를 입력하세요" autocomplete="new-password" />
          <button class="ghost-icon eye" type="button" @click="showPassword = !showPassword" :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 보기'">
            <img class="eye-icon" :src="passwordIconSrc" alt="비밀번호 토글" />
          </button>
        </div>

        <label class="field-label" for="password-confirm">비밀번호 확인</label>
        <div class="input-wrap password-wrap">
          <span class="input-icon lock" aria-hidden="true"></span>
          <input
            id="password-confirm"
            :type="showPasswordConfirm ? 'text' : 'password'"
            v-model="passwordConfirm"
            placeholder="비밀번호를 다시 입력하세요"
            autocomplete="new-password"
          />
          <button class="ghost-icon eye" type="button" @click="showPasswordConfirm = !showPasswordConfirm" :aria-label="showPasswordConfirm ? '비밀번호 숨기기' : '비밀번호 보기'">
            <img class="eye-icon" :src="passwordConfirmIconSrc" alt="비밀번호 확인 토글" />
          </button>
        </div>

        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

        <button class="primary-button" type="submit" :disabled="loading">
          {{ loading ? '가입 중...' : '회원가입' }}
        </button>

        <p class="auth-link">
          이미 계정이 있으신가요?
          <RouterLink to="/login">로그인</RouterLink>
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
  grid-template-columns: minmax(500px, 39.5vw) 1fr;
  min-width: 1180px;
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
  padding: 114px 58px 54px;
  color: #fff;
  background:
    radial-gradient(circle at 28% 42%, rgba(16, 104, 214, 0.42), transparent 31%),
    linear-gradient(180deg, rgba(2, 24, 60, 0.88), rgba(1, 19, 48, 0.98)),
    #031b43;
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

.brand-bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  opacity: 0.45;
  filter: saturate(0.8) brightness(0.75);
  z-index: 0;
}

.brand-copy {
  position: relative;
  z-index: 1;
  margin-bottom: 42px;
  text-align: center;
}

.brand-logo-left,
.brand-logo-right {
  width: 92px;
  height: auto;
  object-fit: contain;
  display: block;
  margin: 0 auto 24px;
}

.brand-copy h1 {
  margin: 26px 0 18px;
  font-size: 38px;
  font-weight: 950;
  letter-spacing: 0;
}

.brand-copy p {
  margin: 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 20px;
  font-weight: 650;
}

.factory-scene {
  position: relative;
  z-index: 1;
  height: 386px;
  opacity: 0.86;
}

.grid-floor {
  position: absolute;
  inset: 38px -70px 0;
  background:
    linear-gradient(90deg, transparent 49%, rgba(25, 140, 255, 0.42) 50%, transparent 51%) 0 0 / 86px 86px,
    linear-gradient(transparent 49%, rgba(25, 140, 255, 0.42) 50%, transparent 51%) 0 0 / 86px 86px;
  transform: perspective(520px) rotateX(58deg);
  transform-origin: bottom;
}

.robot-arm,
.conveyor,
.box,
.tower,
.console {
  position: absolute;
  filter: drop-shadow(0 12px 18px rgba(0, 80, 180, 0.28));
}

.robot-arm {
  left: 42px;
  bottom: 92px;
  width: 260px;
  height: 260px;
}

.robot-arm .base {
  position: absolute;
  left: 18px;
  bottom: 0;
  width: 118px;
  height: 72px;
  border: 2px solid #1a8cff;
  background: linear-gradient(145deg, rgba(23, 118, 235, 0.82), rgba(8, 48, 105, 0.92));
}

.joint {
  position: absolute;
  z-index: 2;
  width: 54px;
  height: 54px;
  border: 9px solid #1387ff;
  border-radius: 50%;
  background: #06316c;
}

.joint-a {
  left: 44px;
  bottom: 58px;
}

.joint-b {
  left: 122px;
  bottom: 174px;
}

.joint-c {
  right: 34px;
  bottom: 114px;
}

.arm {
  position: absolute;
  height: 48px;
  border: 2px solid #36a0ff;
  background: linear-gradient(90deg, #0b5ab5, #1689f7);
  transform-origin: left center;
}

.arm-a {
  left: 72px;
  bottom: 104px;
  width: 128px;
  transform: rotate(-58deg);
}

.arm-b {
  left: 146px;
  bottom: 172px;
  width: 120px;
  transform: rotate(36deg);
}

.arm-c {
  right: 12px;
  bottom: 96px;
  width: 76px;
  transform: rotate(78deg);
}

.conveyor {
  left: 0;
  right: 120px;
  bottom: 56px;
  height: 48px;
  border: 2px solid #147ade;
  background: rgba(4, 45, 96, 0.92);
}

.conveyor span {
  display: inline-block;
  width: 28px;
  height: 28px;
  margin: 9px 10px;
  border: 3px solid #176fca;
  border-radius: 50%;
}

.box {
  width: 62px;
  height: 48px;
  border: 2px solid #177ee7;
  background: rgba(18, 83, 168, 0.8);
}

.box-a {
  left: 95px;
  bottom: 116px;
}

.box-b {
  left: 210px;
  bottom: 124px;
}

.tower {
  bottom: 126px;
  width: 54px;
  border: 2px solid rgba(42, 132, 229, 0.72);
  background: rgba(8, 47, 102, 0.78);
}

.tower-a {
  right: 190px;
  height: 136px;
}

.tower-b {
  right: 112px;
  height: 104px;
}

.console {
  right: 0;
  bottom: 48px;
  width: 138px;
  height: 120px;
  border: 2px solid #176fca;
  background: linear-gradient(145deg, rgba(14, 67, 138, 0.86), rgba(2, 28, 66, 0.95));
  transform: skewY(-12deg);
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
  padding: 46px 64px 72px;
}

.auth-card {
  width: min(780px, 100%);
  padding: 32px 54px 28px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 50px rgba(20, 42, 83, 0.13);
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

.ghost-icon.eye:hover {
  background: rgba(7, 65, 150, 0.12);
}

.form-heading {
  margin-bottom: 20px;
  text-align: center;
}

.form-heading h2 {
  margin: 14px 0 6px;
  font-size: 38px;
  font-weight: 950;
  letter-spacing: 0;
  color: #071b54;
}

.form-heading p {
  margin: 0;
  color: var(--muted);
  font-size: 19px;
  font-weight: 700;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 32px;
}

.field-label {
  display: block;
  margin: 0 0 8px;
  color: #0a1b4d;
  font-size: 17px;
  font-weight: 900;
}

.field-label.full,
.input-wrap.full {
  grid-column: 1 / -1;
}

.input-wrap {
  display: flex;
  align-items: center;
  height: 54px;
  margin-bottom: 16px;
  padding: 0 16px;
  border: 1px solid #ccd7e7;
  border-radius: 8px;
  background: #fff;
  box-shadow: inset 0 1px 0 rgba(16, 49, 98, 0.02);
}

.input-wrap:focus-within {
  border-color: #1d6fff;
  box-shadow: 0 0 0 4px rgba(13, 102, 255, 0.1);
}

.input-wrap input,
.input-wrap select {
  width: 100%;
  min-width: 0;
  height: 100%;
  border: 0;
  outline: 0;
  padding: 0 16px;
  color: #102653;
  font-size: 18px;
  font-weight: 700;
  background: transparent;
}

.input-wrap select {
  appearance: none;
  color: #8a98b2;
}

.input-wrap input::placeholder {
  color: #8a98b2;
}

.globe,
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

.input-icon.mail::before {
  content: '';
  position: absolute;
  inset: 4px 2px;
  border: 3px solid currentColor;
  border-radius: 3px;
}

.input-icon.mail::after {
  content: '';
  position: absolute;
  left: 5px;
  top: 8px;
  width: 14px;
  height: 10px;
  border-left: 3px solid currentColor;
  border-bottom: 3px solid currentColor;
  transform: rotate(-45deg);
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

.input-icon.equipment::before {
  content: '';
  position: absolute;
  left: 4px;
  top: 12px;
  width: 16px;
  height: 9px;
  border: 3px solid currentColor;
  border-radius: 8px;
}

.input-icon.equipment::after {
  content: '';
  position: absolute;
  left: 11px;
  top: 3px;
  width: 10px;
  height: 18px;
  border-left: 3px solid currentColor;
  border-top: 3px solid currentColor;
  transform: rotate(32deg);
}

.input-icon.briefcase::before {
  content: '';
  position: absolute;
  inset: 8px 2px 3px;
  border: 3px solid currentColor;
  border-radius: 4px;
}

.input-icon.briefcase::after {
  content: '';
  position: absolute;
  left: 8px;
  top: 3px;
  width: 8px;
  height: 7px;
  border: 3px solid currentColor;
  border-bottom: 0;
  border-radius: 4px 4px 0 0;
}

.ghost-icon {
  border: 0;
  background: transparent;
}

.ghost-icon.eye .eye-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.error-msg {
  margin: 0 0 12px;
  padding: 10px 14px;
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
  height: 60px;
  margin-top: 4px;
  border: 0;
  border-radius: 8px;
  color: #fff;
  background: linear-gradient(180deg, #086bff, #0753dc);
  box-shadow: 0 12px 22px rgba(6, 88, 231, 0.23);
  font-size: 21px;
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
  margin: 14px 0 0;
  color: #7584a3;
  text-align: center;
  font-size: 17px;
  font-weight: 800;
}

.auth-link a {
  color: var(--blue);
  font-weight: 900;
}

.copyright {
  position: absolute;
  bottom: 28px;
  margin: 0;
  color: #8492ad;
  font-size: 15px;
  font-weight: 650;
}

@media (max-width: 1280px) {
  .auth-page {
    min-width: 1100px;
    grid-template-columns: 450px 1fr;
  }

  .brand-panel {
    padding-left: 44px;
    padding-right: 44px;
  }

  .auth-card {
    width: 710px;
    padding-left: 46px;
    padding-right: 46px;
  }
}
</style>
