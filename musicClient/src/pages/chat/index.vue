<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { sendChatMessage, type ChatMessage } from '@/api/chat'
import { ElMessage } from 'element-plus'
import { Icon } from '@iconify/vue'
import Matrix from '@/components/Matrix/index.vue'
import { imageToColorFrame, createAnimatedFrames, type ColorFrame, type AnimationType } from '@/utils/matrix'

const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const loading = ref(false)
const scrollbarRef = ref<HTMLElement | null>(null)
const matrixFrames = ref<ColorFrame[]>([])
const matrixLoading = ref(true)
const colorFrameRef = ref<ColorFrame | null>(null)

const animationModes: AnimationType[] = ['float', 'sparkle', 'wave', 'scan']
const currentModeIndex = ref(0)
const modeTimerId = ref<number | undefined>(undefined)

const scrollToBottom = async () => {
  await nextTick()
  if (scrollbarRef.value) {
    scrollbarRef.value.scrollTop = scrollbarRef.value.scrollHeight
  }
}

function updateAnimationMode() {
  if (!colorFrameRef.value) return
  currentModeIndex.value = (currentModeIndex.value + 1) % animationModes.length
  matrixFrames.value = createAnimatedFrames(colorFrameRef.value, animationModes[currentModeIndex.value], 12)
}

onMounted(async () => {
  try {
    const colorFrame = await imageToColorFrame('/thinking.png', 45, 54)
    colorFrameRef.value = colorFrame
    matrixFrames.value = createAnimatedFrames(colorFrame, animationModes[0], 12)
    
    modeTimerId.value = window.setInterval(() => {
      updateAnimationMode()
    }, 5000)
  } catch (error) {
    console.error('Failed to load matrix frame:', error)
  } finally {
    matrixLoading.value = false
  }
})

onUnmounted(() => {
  if (modeTimerId.value) {
    clearInterval(modeTimerId.value)
  }
})

const handleSend = async () => {
  if (!inputMessage.value.trim()) return

  const userMsg = inputMessage.value
  messages.value.push({ role: 'user', content: userMsg })
  inputMessage.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await sendChatMessage(messages.value)
    if (res.code === 0 && res.data) {
      const answerText = typeof res.data === 'string' ? res.data : res.data.answer
      const audioUrl = typeof res.data === 'string' ? '' : res.data.audio

      messages.value.push({ role: 'assistant', content: answerText })

      if (audioUrl) {
        const audio = new Audio(audioUrl)
        audio.play().catch(e => console.error('Audio play failed', e))
      }
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
  <div class="spotify-chat-page">
    <div class="spotify-chat-header">
      <span class="spotify-chat-subtitle">Ciallo～(∠・ω< )⌒★</span>
    </div>

    <div ref="scrollbarRef" class="spotify-chat-messages">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="spotify-message"
        :class="msg.role === 'user' ? 'spotify-message-user' : 'spotify-message-assistant'"
      >
        <div v-if="msg.role === 'assistant'" class="spotify-avatar spotify-avatar-assistant">
          <img src="/congyu.png" alt="AI" />
        </div>
        <div
          class="spotify-message-bubble"
          :class="msg.role === 'user' ? 'spotify-bubble-user' : 'spotify-bubble-assistant'"
        >
          {{ msg.content }}
        </div>
      </div>

      <div v-if="loading" class="spotify-message spotify-message-assistant">
        <div class="spotify-avatar spotify-avatar-assistant">
          <img src="/thinking.png" alt="AI" />
        </div>
        <div class="spotify-bubble-assistant spotify-message-bubble spotify-typing">
          <div class="spotify-typing-dots">
            <span></span>
            <span></span>
            <span></span>
          </div>
          <span class="spotify-typing-text">思考中...</span>
        </div>
      </div>

      <div v-if="messages.length === 0" class="spotify-empty-state">
        <div v-if="matrixLoading" class="spotify-matrix-loading">
          <img src="/congyu.png" alt="AI Assistant" class="spotify-matrix-fallback" />
        </div>
        <Matrix
          v-else-if="matrixFrames.length > 0"
          :rows="45"
          :cols="54"
          :color-frames="matrixFrames"
          :size="4"
          :gap="1"
          :brightness="1.0"
          :fps="12"
          loop
        />
      </div>
    </div>

    <div class="spotify-chat-input">
      <div class="spotify-input-wrapper">
        <Icon icon="ri:chat-1-line" class="spotify-input-icon" />
        <input
          v-model="inputMessage"
          placeholder="输入消息..."
          @keyup.enter="handleSend"
          :disabled="loading"
          class="spotify-input"
        />
      </div>
      <button
        @click="handleSend"
        :disabled="loading || !inputMessage.trim()"
        class="spotify-send-btn"
        :class="{ 'spotify-send-btn-disabled': loading || !inputMessage.trim() }"
      >
        <Icon v-if="loading" icon="eos-icons:bubble-loading" class="text-lg" />
        <Icon v-else icon="mdi:send" class="text-lg" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.spotify-chat-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--bg-surface, #121212);
  padding: 16px;
  gap: 16px;
}

.spotify-chat-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex-shrink: 0;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
}

.spotify-chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-base, #fff);
}

.spotify-chat-icon {
  font-size: 1.75rem;
  color: #1db954;
}

.spotify-chat-subtitle {
  font-size: 0.875rem;
  color: var(--text-subdued, #b3b3b3);
  padding-left: 32px;
}

.spotify-chat-messages {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0;
  min-height: 0;
}

.spotify-message {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  animation: spotify-fade-in 200ms ease-out;
}

@keyframes spotify-fade-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.spotify-message-user {
  flex-direction: row-reverse;
}

.spotify-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.spotify-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.spotify-avatar-assistant {
  background: linear-gradient(135deg, #1db954 0%, #1ed760 100%);
}

.spotify-message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 0.9375rem;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.spotify-bubble-user {
  background-color: #1db954;
  color: #000;
  border-bottom-right-radius: 4px;
  font-weight: 500;
}

.spotify-bubble-assistant {
  background-color: var(--bg-elevated, #242424);
  color: var(--text-base, #fff);
  border-bottom-left-radius: 4px;
}

.spotify-typing {
  display: flex;
  align-items: center;
  gap: 8px;
}

.spotify-typing-dots {
  display: flex;
  gap: 4px;
}

.spotify-typing-dots span {
  width: 6px;
  height: 6px;
  background-color: #1db954;
  border-radius: 50%;
  animation: spotify-bounce 1.4s infinite ease-in-out both;
}

.spotify-typing-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.spotify-typing-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes spotify-bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.spotify-typing-text {
  color: var(--text-subdued, #b3b3b3);
  font-size: 0.875rem;
}

.spotify-empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.spotify-matrix-loading {
  display: flex;
  align-items: center;
  justify-content: center;
}

.spotify-matrix-fallback {
  max-width: 200px;
  border-radius: 16px;
}

.spotify-chat-input {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
  padding-top: 16px;
  border-top: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
}

.spotify-input-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}

.spotify-input-icon {
  position: absolute;
  left: 16px;
  color: var(--text-subdued, #b3b3b3);
  font-size: 1.25rem;
  pointer-events: none;
}

.spotify-input {
  width: 100%;
  padding: 14px 16px 14px 48px;
  background-color: var(--bg-elevated, #242424);
  border: none;
  border-radius: 500px;
  color: var(--text-base, #fff);
  font-size: 0.9375rem;
  transition: box-shadow 200ms ease;
}

.spotify-input::placeholder {
  color: var(--text-subdued, #b3b3b3);
}

.spotify-input:focus {
  outline: none;
  box-shadow: 0 0 0 2px #fff;
}

.spotify-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spotify-send-btn {
  width: 48px;
  height: 48px;
  background-color: #1db954;
  border: none;
  border-radius: 50%;
  color: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 200ms ease;
  flex-shrink: 0;
}

.spotify-send-btn:hover:not(.spotify-send-btn-disabled) {
  background-color: #1ed760;
  transform: scale(1.04);
}

.spotify-send-btn:active:not(.spotify-send-btn-disabled) {
  transform: scale(1);
}

.spotify-send-btn-disabled {
  background-color: #535353;
  color: #b3b3b3;
  cursor: not-allowed;
}

/* Light Theme */
:root:not(.dark) .spotify-chat-page {
  --bg-surface: #f0f0f0;
  --bg-elevated: #e8e8e8;
  --text-base: #000000;
  --text-subdued: #6a6a6a;
  --border-color: rgba(0, 0, 0, 0.1);
}

:root:not(.dark) .spotify-input:focus {
  box-shadow: 0 0 0 2px #000;
}

:root:not(.dark) .spotify-empty-image {
  background: linear-gradient(135deg, rgba(29, 185, 84, 0.1) 0%, rgba(30, 215, 96, 0.05) 100%);
}

/* Scrollbar */
.spotify-chat-messages::-webkit-scrollbar {
  width: 8px;
}

.spotify-chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.spotify-chat-messages::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 4px;
}

.spotify-chat-messages::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

@media (max-width: 640px) {
  .spotify-chat-page {
    padding-bottom: 80px;
  }
  
  .spotify-chat-header {
    padding: 12px 16px;
  }
  
  .spotify-chat-subtitle {
    font-size: 0.8125rem;
  }
  
  .spotify-message-bubble {
    max-width: 85%;
  }
  
  .spotify-chat-input {
    padding: 12px;
    gap: 8px;
  }
  
  .spotify-input-wrapper {
    padding: 10px 12px 10px 40px;
  }
  
  .spotify-input {
    font-size: 0.875rem;
  }
  
  .spotify-send-btn {
    width: 40px;
    height: 40px;
  }
}
</style>
