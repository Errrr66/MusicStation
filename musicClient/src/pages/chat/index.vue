<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { sendChatMessage, type ChatMessage } from '@/api/chat'
import { ElMessage } from 'element-plus'
import { Icon } from '@iconify/vue'

const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const loading = ref(false)
const scrollbarRef = ref<HTMLElement | null>(null)

const scrollToBottom = async () => {
  await nextTick()
  if (scrollbarRef.value) {
    scrollbarRef.value.scrollTop = scrollbarRef.value.scrollHeight
  }
}

const handleSend = async () => {
  if (!inputMessage.value.trim()) return

  const userMsg = inputMessage.value
  messages.value.push({ role: 'user', content: userMsg })
  inputMessage.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await sendChatMessage(messages.value)
    if (res.code === 0) {
      messages.value.push({ role: 'assistant', content: res.data })
    } else {
      ElMessage.error(res.message || '发送失败')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('网络错误')
  } finally {
    loading.value = false
    scrollToBottom()
  }
}
</script>

<template>
  <div class="flex flex-col h-full w-full bg-themeBgColor p-4 gap-4 box-border">
    <div
      class="text-2xl font-bold text-primary-foreground flex items-center gap-2"
    >
      <Icon icon="ri:robot-line" />
      <span>Ciallo～(∠・ω< )⌒★</span>
    </div>

    <div
      ref="scrollbarRef"
      class="flex-1 overflow-y-auto rounded-lg bg-black/20 p-4 space-y-4"
    >
      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="flex w-full gap-2 items-start"
        :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
      >
        <img
          v-if="msg.role === 'assistant'"
          src="/congyu.png"
          class="w-10 h-10 rounded-full object-cover border border-white/10 shadow-sm"
        />
        <div
          class="max-w-[80%] rounded-lg p-3 text-sm leading-relaxed whitespace-pre-wrap shadow-md"
          :class="[
            msg.role === 'user'
              ? 'bg-blue-600 text-white rounded-br-none'
              : 'bg-gray-700 text-gray-100 rounded-bl-none',
          ]"
        >
          {{ msg.content }}
        </div>
      </div>

      <div v-if="loading" class="flex justify-start gap-2 items-start">
        <img
          src="/thinking.png"
          class="w-10 h-10 rounded-full object-cover border border-white/10 shadow-sm"
        />
        <div
          class="bg-gray-700 text-gray-100 rounded-lg p-3 rounded-bl-none text-sm flex items-center gap-2 shadow-md"
        >
          <Icon icon="eos-icons:bubble-loading" />
          <span>思考中...</span>
        </div>
      </div>

      <div
        v-if="messages.length === 0"
        class="flex flex-col items-center justify-center h-full text-inactive select-none"
      >
        <img
          src="/congyu.png"
          class="w-[280px] h-auto object-cover mb-6 opacity-90 hover:scale-105 transition-all duration-500 rounded-xl"
        />
        <p class="text-lg font-medium tracking-wide text-black dark:text-white/60">
          ご主人様、何かご命令はございますか？
        </p>
      </div>
    </div>

    <div class="flex gap-2">
      <el-input
        v-model="inputMessage"
        placeholder="enter..."
        @keyup.enter="handleSend"
        :disabled="loading"
        class="flex-1"
      >
        <template #prefix>
          <Icon icon="ri:chat-1-line" class="text-gray-400" />
        </template>
      </el-input>
      <el-button
        type="primary"
        :loading="loading"
        @click="handleSend"
        class="!px-6"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<style scoped>
:deep(.el-input__wrapper) {
  background-color: rgba(0, 0, 0, 0.2);
  box-shadow: none;
  border: 1px solid rgba(255, 255, 255, 0.1);
}
:deep(.el-input__inner) {
  color: white;
}
</style>
