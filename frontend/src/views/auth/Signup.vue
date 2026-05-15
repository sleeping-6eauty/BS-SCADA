<script setup>
const features = [
  {
    title: '실시간 모니터링',
    description: '설비 상태 실시간 확인',
    icon: 'shield',
  },
  {
    title: '알람 관리',
    description: '신속한 알람 감지 및 처리',
    icon: 'bell',
  },
  {
    title: '데이터 분석',
    description: '정확한 데이터로 효율적 운영',
    icon: 'chart',
  },
]
</script>

<template>
  <main class="auth-page signup-page">
    <section class="brand-panel" aria-label="설비 알람 관리 시스템 소개">
      <div class="brand-copy">
        <div class="line-robot mark-light" aria-hidden="true">
          <svg viewBox="0 0 80 80" role="img">
            <path d="M18 64h38M24 56h26M36 52V39M29 39l-9 9M39 35l16-10M54 24l8 9" />
            <circle cx="35" cy="31" r="8" />
            <circle cx="60" cy="22" r="7" />
            <circle cx="66" cy="37" r="4" />
            <path d="M24 56h26v8H24zM18 48l6 8M20 48h10M43 31l10-6M48 16l7 6" />
          </svg>
        </div>
        <h1>설비 알람 관리 시스템</h1>
        <p>실시간 모니터링으로 설비의 안정성과 효율성을 높입니다.</p>
      </div>

      <div class="factory-scene" aria-hidden="true">
        <div class="grid-floor"></div>
        <div class="robot-arm">
          <span class="joint joint-a"></span>
          <span class="joint joint-b"></span>
          <span class="joint joint-c"></span>
          <span class="arm arm-a"></span>
          <span class="arm arm-b"></span>
          <span class="arm arm-c"></span>
          <span class="base"></span>
        </div>
        <div class="conveyor">
          <span v-for="index in 9" :key="index"></span>
        </div>
        <div class="box box-a"></div>
        <div class="box box-b"></div>
        <div class="tower tower-a"></div>
        <div class="tower tower-b"></div>
        <div class="console"></div>
      </div>

      <ul class="feature-row">
        <li v-for="feature in features" :key="feature.title">
          <span class="feature-icon" :class="feature.icon" aria-hidden="true"></span>
          <strong>{{ feature.title }}</strong>
          <small>{{ feature.description }}</small>
        </li>
      </ul>
    </section>

    <section class="form-panel" aria-label="회원가입">
      <form class="auth-card">
        <div class="form-heading">
          <div class="line-robot mark-blue" aria-hidden="true">
            <svg viewBox="0 0 80 80">
              <path d="M18 64h38M24 56h26M36 52V39M29 39l-9 9M39 35l16-10M54 24l8 9" />
              <circle cx="35" cy="31" r="8" />
              <circle cx="60" cy="22" r="7" />
              <circle cx="66" cy="37" r="4" />
              <path d="M24 56h26v8H24zM18 48l6 8M20 48h10M43 31l10-6M48 16l7 6" />
            </svg>
          </div>
          <h2>회원가입</h2>
          <p>계정을 생성하여 시스템을 이용하세요.</p>
        </div>

        <label class="field-label full" for="name">이름</label>
        <div class="input-wrap full">
          <span class="input-icon user" aria-hidden="true"></span>
          <input id="name" type="text" placeholder="이름을 입력하세요" autocomplete="name" />
        </div>

        <div class="field-grid">
          <label class="field-label" for="user-id">아이디</label>
          <label class="field-label" for="email">이메일</label>

          <div class="input-wrap">
            <span class="input-icon user" aria-hidden="true"></span>
            <input id="user-id" type="text" placeholder="아이디를 입력하세요" autocomplete="username" />
          </div>
          <div class="input-wrap">
            <span class="input-icon mail" aria-hidden="true"></span>
            <input id="email" type="email" placeholder="이메일을 입력하세요" autocomplete="email" />
          </div>
        </div>

        <label class="field-label full" for="password">비밀번호</label>
        <div class="input-wrap full">
          <span class="input-icon lock" aria-hidden="true"></span>
          <input id="password" type="password" placeholder="비밀번호를 입력하세요" autocomplete="new-password" />
          <button class="ghost-icon eye" type="button" aria-label="비밀번호 보기"></button>
        </div>

        <label class="field-label full" for="password-confirm">비밀번호 확인</label>
        <div class="input-wrap full">
          <span class="input-icon lock" aria-hidden="true"></span>
          <input
            id="password-confirm"
            type="password"
            placeholder="비밀번호를 다시 입력하세요"
            autocomplete="new-password"
          />
          <button class="ghost-icon eye" type="button" aria-label="비밀번호 확인 보기"></button>
        </div>

        <div class="field-grid">
          <label class="field-label" for="equipment">담당 설비</label>
          <label class="field-label" for="role">직책</label>

          <div class="input-wrap select-wrap">
            <span class="input-icon equipment" aria-hidden="true"></span>
            <select id="equipment">
              <option>담당 설비를 선택하세요</option>
              <option>Robot A1</option>
              <option>Nutrunner B2</option>
              <option>Conveyor D1</option>
            </select>
            <span class="chevron" aria-hidden="true"></span>
          </div>
          <div class="input-wrap select-wrap">
            <span class="input-icon briefcase" aria-hidden="true"></span>
            <select id="role">
              <option>직책을 선택하세요</option>
              <option>관리자</option>
              <option>운영자</option>
              <option>일반 사용자</option>
            </select>
            <span class="chevron" aria-hidden="true"></span>
          </div>
        </div>

        <button class="primary-button" type="submit">회원가입</button>

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

.brand-copy {
  position: relative;
  z-index: 1;
  margin-bottom: 42px;
  text-align: center;
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
  padding: 44px 54px 42px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 50px rgba(20, 42, 83, 0.13);
}

.form-heading {
  margin-bottom: 26px;
  text-align: center;
}

.form-heading h2 {
  margin: 18px 0 8px;
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
  margin: 0 0 10px;
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
  height: 58px;
  margin-bottom: 24px;
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

.ghost-icon.eye::before {
  content: '';
  position: absolute;
  left: 2px;
  top: 7px;
  width: 20px;
  height: 12px;
  border: 3px solid currentColor;
  border-radius: 50%;
}

.ghost-icon.eye::after {
  content: '';
  position: absolute;
  left: 1px;
  top: 11px;
  width: 23px;
  height: 3px;
  border-radius: 4px;
  background: currentColor;
  transform: rotate(-34deg);
}

.primary-button {
  width: 100%;
  height: 62px;
  margin-top: 8px;
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

.auth-link {
  margin: 22px 0 0;
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
