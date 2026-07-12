<script setup lang="ts">
import { useRouter } from "vue-router";
import { message } from "@/utils/message";
import { loginRules } from "./utils/rule";
import type { FormInstance } from "element-plus";
import { useLayout } from "@/layout/hooks/useLayout";
import { useUserStoreHook } from "@/store/modules/user";
import { ref, reactive, onMounted, onBeforeUnmount } from "vue";
import { addPathMatch, getTopMenu } from "@/router/utils";
import { usePermissionStoreHook } from "@/store/modules/permission";

import Eye from "@iconify-icons/ri/eye-line";
import EyeOff from "@iconify-icons/ri/eye-off-line";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";

defineOptions({
  name: "Login"
});

const BG_IMAGE_1 =
  "https://images.higgs.ai/?default=1&output=webp&url=https%3A%2F%2Fd8j0ntlcm91z4.cloudfront.net%2Fuser_38xzZboKViGWJOttwIXH07lWA1P%2Fhf_20260609_195923_b0ba8ace-1d1d-4f2c-9a28-1ab84b330680.png&w=1280&q=85";
const BG_IMAGE_2 =
  "https://images.higgs.ai/?default=1&output=webp&url=https%3A%2F%2Fd8j0ntlcm91z4.cloudfront.net%2Fuser_38xzZboKViGWJOttwIXH07lWA1P%2Fhf_20260609_201152_bba90a12-bf12-459f-91f0-51f237dbaf3b.png&w=1280&q=85";
const SPOTLIGHT_R = 260;

const router = useRouter();
const loading = ref(false);
const ruleFormRef = ref<FormInstance>();
const spotlightCanvasRef = ref<HTMLCanvasElement | null>(null);
const maskUrl = ref("");
const mouse = ref({ x: -999, y: -999 });
const smooth = ref({ x: -999, y: -999 });
const cursorPos = ref({ x: -999, y: -999 });
let rafId: number | undefined;

const { initStorage } = useLayout();
initStorage();

const ruleForm = reactive({
  username: "",
  password: ""
});

const rememberMe = ref(false);
const showPassword = ref(false);

const toggleShowPassword = () => {
  showPassword.value = !showPassword.value;
};

const onLogin = async (formEl: FormInstance | undefined) => {
  if (!formEl) return;
  await formEl.validate((valid, fields) => {
    if (valid) {
      loading.value = true;
      useUserStoreHook().SET_ISREMEMBERED(rememberMe.value);
      useUserStoreHook()
        .loginByUsername({
          username: ruleForm.username,
          password: ruleForm.password
        })
        .then(res => {
          if (res.code === 0) {
            usePermissionStoreHook().handleWholeMenus([]);
            addPathMatch();
            const topMenu = getTopMenu(true);
            if (topMenu?.path) {
              router.push(topMenu.path).then(() => {
                message("登录成功", { type: "success" });
              });
            } else {
              message("登录成功，但未找到可跳转页面", { type: "warning" });
            }
          } else {
            message("登录失败，" + res.message, { type: "error" });
          }
        })
        .catch(error => {
          const errorMsg =
            error?.response?.data?.message ||
            error?.message ||
            "请检查网络连接或稍后重试";
          message("登录失败，" + errorMsg, { type: "error" });
          console.error("登录错误：", error);
        })
        .finally(() => (loading.value = false));
    } else {
      console.error("表单校验失败:", fields);
    }
  });
};

function onkeypress({ code }: KeyboardEvent) {
  if (["Enter", "NumpadEnter"].includes(code)) {
    onLogin(ruleFormRef.value);
  }
}

function resizeCanvas() {
  const canvas = spotlightCanvasRef.value;
  if (!canvas) return;
  canvas.width = window.innerWidth;
  canvas.height = window.innerHeight;
}

function drawMask() {
  const canvas = spotlightCanvasRef.value;
  if (!canvas) return;
  const ctx = canvas.getContext("2d");
  if (!ctx) return;

  ctx.clearRect(0, 0, canvas.width, canvas.height);
  const gradient = ctx.createRadialGradient(
    cursorPos.value.x,
    cursorPos.value.y,
    0,
    cursorPos.value.x,
    cursorPos.value.y,
    SPOTLIGHT_R
  );
  gradient.addColorStop(0, "rgba(255, 255, 255, 1)");
  gradient.addColorStop(0.4, "rgba(255, 255, 255, 1)");
  gradient.addColorStop(0.6, "rgba(255, 255, 255, 0.75)");
  gradient.addColorStop(0.75, "rgba(255, 255, 255, 0.4)");
  gradient.addColorStop(0.88, "rgba(255, 255, 255, 0.12)");
  gradient.addColorStop(1, "rgba(255, 255, 255, 0)");

  ctx.fillStyle = gradient;
  ctx.beginPath();
  ctx.arc(
    cursorPos.value.x,
    cursorPos.value.y,
    SPOTLIGHT_R,
    0,
    Math.PI * 2
  );
  ctx.fill();

  maskUrl.value = canvas.toDataURL();
}

function animate() {
  smooth.value.x += (mouse.value.x - smooth.value.x) * 0.1;
  smooth.value.y += (mouse.value.y - smooth.value.y) * 0.1;
  cursorPos.value = { x: smooth.value.x, y: smooth.value.y };
  drawMask();
  rafId = requestAnimationFrame(animate);
}

function handleMouseMove(e: MouseEvent) {
  mouse.value = { x: e.clientX, y: e.clientY };
}

onMounted(() => {
  window.document.addEventListener("keypress", onkeypress);
  resizeCanvas();
  window.addEventListener("resize", resizeCanvas);
  window.addEventListener("mousemove", handleMouseMove);
  rafId = requestAnimationFrame(animate);
});

onBeforeUnmount(() => {
  window.document.removeEventListener("keypress", onkeypress);
  window.removeEventListener("resize", resizeCanvas);
  window.removeEventListener("mousemove", handleMouseMove);
  if (rafId) cancelAnimationFrame(rafId);
});
</script>

<template>
  <div class="login-page">
    <div class="login-visual">
      <div
        class="geo-bg-base hero-zoom"
        :style="{ backgroundImage: `url(${BG_IMAGE_1})` }"
      />
      <canvas
        ref="spotlightCanvasRef"
        class="spotlight-canvas"
        aria-hidden="true"
      />
      <div
        class="geo-bg-reveal"
        :style="{
          backgroundImage: `url(${BG_IMAGE_2})`,
          maskImage: maskUrl ? `url(${maskUrl})` : undefined,
          webkitMaskImage: maskUrl ? `url(${maskUrl})` : undefined
        }"
      />
      <div class="visual-overlay">
        <div class="hero-title">
          <h1 class="hero-heading">
            <span
              class="hero-line hero-line-1 hero-anim hero-reveal"
              style="animation-delay: 0.25s"
            >
              parachutes
            </span>
          </h1>
        </div>

        <div
          class="login-card"
          style="width: 500px; height: 499px; padding: 50px; background: rgba(5, 5, 5, 0.5)"
        >
          <div class="login-card-body">
            <el-form
              ref="ruleFormRef"
              :model="ruleForm"
              :rules="loginRules"
              size="large"
              class="login-form"
            >
              <el-form-item prop="username">
                <el-input
                  v-model="ruleForm.username"
                  clearable
                  placeholder="用户名"
                  class="login-input"
                />
              </el-form-item>

              <el-form-item prop="password">
                <el-input
                  v-model="ruleForm.password"
                  :type="showPassword ? 'text' : 'password'"
                  placeholder="密码"
                  class="login-input"
                >
                  <template #suffix>
                    <component
                      :is="useRenderIcon(showPassword ? Eye : EyeOff)"
                      class="password-toggle"
                      @click="toggleShowPassword"
                    />
                  </template>
                </el-input>
              </el-form-item>

              <div class="login-options">
                <el-checkbox v-model="rememberMe" class="remember-checkbox">
                  7天内免登录
                </el-checkbox>
              </div>

              <button
                type="button"
                class="login-button"
                :disabled="loading"
                @click="onLogin(ruleFormRef)"
              >
                {{ loading ? "登录中..." : "登录" }}
              </button>
            </el-form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=Playfair+Display:ital,wght@1,400;1,500;1,600&display=swap');

.login-page {
  min-height: 100vh;
  min-height: 100dvh;
  background: var(--mr-bg-base);
  font-family: var(--mr-font-family, 'SF Mono', 'Consolas', monospace);
  color: var(--mr-text-base);
}

.login-visual {
  position: relative;
  width: 100%;
  min-height: 100vh;
  min-height: 100dvh;
  background: #000;
  overflow: hidden;
}

.geo-bg-base {
  position: absolute;
  inset: 0;
  z-index: 10;
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
}

.spotlight-canvas {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: none;
  pointer-events: none;
}

.geo-bg-reveal {
  position: absolute;
  inset: 0;
  z-index: 30;
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
  pointer-events: none;
  mask-size: 100vw 100vh;
  -webkit-mask-size: 100vw 100vh;
}

.visual-overlay {
  position: relative;
  z-index: 50;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 40px;
  min-height: 100vh;
  min-height: 100dvh;
  padding: 48px;
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.55) 0%,
    rgba(0, 0, 0, 0.2) 45%,
    rgba(0, 0, 0, 0.65) 100%
  );
}

.visual-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  pointer-events: none;
}

.brand-logo {
  flex-shrink: 0;
}

.brand-name {
  font-family: 'Playfair Display', serif;
  font-size: 1.75rem;
  font-style: italic;
  font-weight: 400;
  color: #fff;
  letter-spacing: -0.02em;
}

.hero-title {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 0 20px;
  pointer-events: none;
}

.hero-heading {
  margin: 0;
  color: #fff;
  line-height: 0.95;
}

.hero-line {
  display: block;
}

.hero-line-1 {
  font-family: 'Playfair Display', serif;
  font-size: 3rem;
  font-style: italic;
  font-weight: 400;
  letter-spacing: -0.05em;
}

.hero-line-2 {
  font-family: 'Inter', sans-serif;
  font-size: 3rem;
  font-weight: 400;
  letter-spacing: -0.08em;
  margin-top: -4px;
}

/* 入场动画 */
@keyframes heroReveal {
  0% {
    opacity: 0;
    transform: translateY(28px);
    filter: blur(12px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}

@keyframes heroFadeUp {
  0% {
    opacity: 0;
    transform: translateY(20px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes heroZoom {
  0% {
    transform: scale(1.12);
  }
  100% {
    transform: scale(1);
  }
}

.hero-anim {
  opacity: 0;
  animation-fill-mode: forwards;
  animation-timing-function: cubic-bezier(0.16, 1, 0.3, 1);
}

.hero-reveal {
  animation-name: heroReveal;
  animation-duration: 1.1s;
}

.hero-fade {
  animation-name: heroFadeUp;
  animation-duration: 1s;
}

.hero-zoom {
  animation: heroZoom 1.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@media (prefers-reduced-motion: reduce) {
  .hero-anim,
  .hero-zoom {
    animation: none;
    opacity: 1;
  }
}

.login-card {
  position: relative;
  width: 100%;
  height: auto;
  min-height: auto;
  max-width: none;
  max-height: none;
  padding: 40px 48px;
  background: var(--mr-bg-base);
  border-radius: 0;
  box-shadow: none;
  overflow: hidden;
  contain: paint layout;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.login-card-body {
  width: 100%;
  max-width: 460px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.login-input :deep(.el-input__wrapper) {
  background: var(--mr-bg-elevated);
  border: 1px solid var(--mr-border-subdued);
  border-radius: var(--el-border-radius-base);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.03),
    0 1px 0 rgba(255, 255, 255, 0.02);
  padding: 0 16px;
  height: 52px;
  opacity: 0.6;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

.login-input :deep(.el-input__wrapper:hover) {
  border-color: var(--mr-essential-subdued);
  background: var(--mr-bg-tinted);
}

.login-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--mr-accent);
  background: var(--mr-bg-elevated);
  box-shadow:
    0 0 0 1px var(--mr-accent),
    0 0 16px var(--mr-accent-glow),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.login-input :deep(.el-input__inner) {
  color: var(--mr-text-base);
  font-size: 0.9375rem;
  font-family: var(--mr-font-family);
}

.login-input :deep(.el-input__inner::placeholder) {
  color: var(--mr-text-subdued);
}

.password-toggle {
  cursor: pointer;
  color: var(--mr-text-subdued);
  transition: color 0.2s ease;
}

.password-toggle:hover {
  color: var(--mr-accent);
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.remember-checkbox {
  color: var(--mr-text-subdued);
  font-size: 0.875rem;
}

.remember-checkbox :deep(.el-checkbox__inner) {
  background: var(--mr-bg-elevated);
  border-color: var(--mr-border-subdued);
}

.remember-checkbox :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: var(--mr-accent);
  border-color: var(--mr-accent);
}

.remember-checkbox :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  color: var(--mr-text-base);
}

.login-button {
  width: 100%;
  height: 52px;
  margin-top: 4px;
  background: var(--mr-accent);
  border: 1px solid var(--mr-accent);
  border-radius: var(--el-border-radius-round);
  color: var(--mr-essential-negative);
  font-size: 0.9375rem;
  font-weight: 700;
  letter-spacing: 1px;
  cursor: pointer;
  transition: transform 0.2s ease, background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.login-button:hover:not(:disabled) {
  transform: scale(1.02);
  background: var(--mr-accent-hover);
  border-color: var(--mr-accent-hover);
  box-shadow: 0 0 20px var(--mr-accent-glow);
}

.login-button:active:not(:disabled) {
  transform: scale(0.98);
}

.login-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (min-width: 640px) {
  .hero-line-1,
  .hero-line-2 {
    font-size: 4.5rem;
  }
}

@media (min-width: 768px) {
  .hero-line-1,
  .hero-line-2 {
    font-size: 6rem;
  }
}

/* 平板响应式 */
@media (max-width: 1024px) {
  .visual-overlay {
    padding: 32px;
    gap: 32px;
  }
}

/* 手机响应式 */
@media (max-width: 480px) {
  .visual-overlay {
    padding: 24px;
    gap: 24px;
  }

  .hero-title {
    padding: 0;
  }

  .hero-line-1 {
    font-size: 2.5rem;
  }

  .login-card {
    padding: 32px 20px;
  }

  .login-card-body {
    max-width: 100%;
  }
}
</style>