<script setup lang="ts">
import Motion from "./utils/motion";
import { useRouter } from "vue-router";
import { message } from "@/utils/message";
import { loginRules } from "./utils/rule";
import { useNav } from "@/layout/hooks/useNav";
import type { FormInstance } from "element-plus";
import { useLayout } from "@/layout/hooks/useLayout";
import { useUserStoreHook } from "@/store/modules/user";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { ref, reactive, onMounted, onBeforeUnmount, computed } from "vue";
import { useDataThemeChange } from "@/layout/hooks/useDataThemeChange";
import { addPathMatch, getTopMenu } from "@/router/utils";
import { usePermissionStoreHook } from "@/store/modules/permission";

import dayIcon from "@/assets/svg/day.svg?component";
import darkIcon from "@/assets/svg/dark.svg?component";
import Eye from "@iconify-icons/ri/eye-line";
import EyeOff from "@iconify-icons/ri/eye-off-line";

import AnimatedCharacters from "./components/AnimatedCharacters/index.vue";
import InteractiveHoverButton from "./components/InteractiveHoverButton.vue";

defineOptions({
  name: "Login"
});
const router = useRouter();
const loading = ref(false);
const ruleFormRef = ref<FormInstance>();

const { initStorage } = useLayout();
initStorage();

const { dataTheme, overallStyle, dataThemeChange } = useDataThemeChange();
dataThemeChange(overallStyle.value);
const { title } = useNav();

const ruleForm = reactive({
  username: "",
  password: ""
});

// 仅开发环境注入测试账号，方便调试
if (import.meta.env.DEV) {
  ruleForm.username = "admin_1";
  ruleForm.password = "123456abc";
}

const rememberMe = ref(false);

// Animation refs
const isTyping = ref(false);
const showPassword = ref(false);
const passwordLength = computed(() => ruleForm.password.length);

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
            // 获取后端路由
            router.push(getTopMenu(true).path).then(() => {
              message("登录成功", { type: "success" });
            });
          } else {
            message("登录失败，" + res.message, { type: "error" });
          }
        })
        .catch(error => {
          // 处理网络错误或其他异常
          message("登录失败，请检查网络连接或稍后重试。", { type: "error" });
          console.error("登录错误：", error);
        })
        .finally(() => (loading.value = false));
    }
  });
};

/** 使用公共函数，避免`removeEventListener`失效 */
function onkeypress({ code }: KeyboardEvent) {
  if (["Enter", "NumpadEnter"].includes(code)) {
    onLogin(ruleFormRef.value);
  }
}

onMounted(() => {
  window.document.addEventListener("keypress", onkeypress);
});

onBeforeUnmount(() => {
  window.document.removeEventListener("keypress", onkeypress);
});
</script>

<template>
  <div
    class="min-h-screen max-h-screen overflow-hidden grid lg:grid-cols-2 select-none bg-bg_color"
  >
    <!-- Left Content Section -->
    <div
      class="relative hidden lg:flex flex-col justify-between bg-gradient-to-br from-gray-400 via-gray-500 to-gray-600 dark:from-white/90 dark:via-white/80 dark:to-white/70 p-12 text-white dark:text-gray-900 overflow-hidden"
    >
      <!-- Header/Logo Area -->
      <div class="relative z-20 flex items-center justify-between">
        <div class="flex items-center gap-2 text-lg font-semibold">
          <!-- Logo -->
          <img
            src="/logo.svg?v=1"
            alt="logo"
            class="w-8 h-8 rounded bg-white/10 p-1 backdrop-blur-sm"
          />
          <span>{{ title }}</span>
        </div>

        <!-- Theme Switcher -->
        <el-switch
          v-model="dataTheme"
          inline-prompt
          :active-icon="dayIcon"
          :inactive-icon="darkIcon"
          class="ml-auto"
          style="--el-switch-on-color: #666; --el-switch-off-color: #333"
          @change="dataThemeChange"
        />
      </div>

      <!-- Animation Area -->
      <div class="relative z-20 flex items-end justify-center h-[500px]">
        <AnimatedCharacters
          :isTyping="isTyping"
          :showPassword="showPassword"
          :passwordLength="passwordLength"
        />
      </div>

      <!-- Footer/Links -->
      <div class="relative z-20 flex items-center gap-8 text-sm opacity-80">
      </div>

      <!-- Decorative elements -->
      <div
        class="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAiIGhlaWdodD0iMjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iMSIgY3k9IjEiIHI9IjEiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4wNSkiLz48L3N2Zz4=')] opacity-20 pointer-events-none"
      />
      <div
        class="absolute top-1/4 right-1/4 w-64 h-64 bg-gray-400/20 dark:bg-gray-300/30 rounded-full blur-3xl pointer-events-none"
      />
      <div
        class="absolute bottom-1/4 left-1/4 w-96 h-96 bg-gray-300/20 dark:bg-gray-200/20 rounded-full blur-3xl pointer-events-none"
      />
    </div>

    <!-- Right Login Section -->
    <div class="flex items-center justify-center p-8 bg-bg_color">
      <div class="w-full max-w-[420px]">
        <!-- Mobile Header/Logo (visible on small screens only) -->
        <div class="lg:hidden flex items-center justify-between mb-8">
          <div class="flex items-center gap-2">
            <img src="/logo.svg" alt="logo" class="w-8 h-8" />
            <span class="text-lg font-semibold text-text_color_primary">{{
              title
            }}</span>
          </div>
          <el-switch
            v-model="dataTheme"
            inline-prompt
            :active-icon="dayIcon"
            :inactive-icon="darkIcon"
            @change="dataThemeChange"
          />
        </div>

        <!-- Header -->
        <div class="text-center mb-10">
          <h1
            class="text-3xl font-bold tracking-tight mb-2 text-text_color_primary"
          >
            欢迎回来!
          </h1>
          <p class="text-text_color_regular text-sm">
            请输入您的账户信息以登录
          </p>
        </div>

        <!-- Form -->
        <el-form
          ref="ruleFormRef"
          :model="ruleForm"
          :rules="loginRules"
          size="large"
          class="space-y-5"
        >
          <el-form-item prop="username">
            <!-- Label manually added -->
            <div
              class="block mb-2 text-sm font-medium text-text_color_primary w-full text-left"
            >
              用户名
            </div>
            <el-input
              v-model="ruleForm.username"
              clearable
              placeholder="Enter username"
              class="!h-12 w-full"
              @focus="isTyping = true"
              @blur="isTyping = false"
            />
          </el-form-item>

          <el-form-item prop="password">
            <div
              class="block mb-2 text-sm font-medium text-text_color_primary w-full text-left"
            >
              密码
            </div>
            <el-input
              v-model="ruleForm.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="Enter password"
              class="!h-12 w-full"
              @focus="isTyping = true"
              @blur="isTyping = false"
            >
              <template #suffix>
                <component
                  :is="useRenderIcon(showPassword ? Eye : EyeOff)"
                  class="cursor-pointer text-gray-500 hover:text-gray-700"
                  @click="toggleShowPassword"
                />
              </template>
            </el-input>
          </el-form-item>

          <div class="flex items-center justify-between">
            <el-checkbox v-model="rememberMe">7天内免登录</el-checkbox>
          </div>

          <InteractiveHoverButton
            type="button"
            :text="loading ? 'Signing in...' : '登录'"
            className="!w-full h-12 text-base font-medium"
            :loading="loading"
            @click="onLogin(ruleFormRef)"
          />
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.el-input__wrapper) {
  background-color: var(--el-bg-color);
  box-shadow: 0 0 0 1px var(--el-border-color);
  padding-top: 2px;
  padding-bottom: 2px;
}
:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--el-color-primary);
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--el-color-primary) !important;
}
:deep(.el-form-item__content) {
  line-height: normal;
  flex-direction: column;
  align-items: flex-start;
}
:deep(.el-form-item) {
  margin-bottom: 1.25rem;
}
</style>
