<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['success'])

const isRecording = ref(false)
const mediaRecorder = ref<MediaRecorder | null>(null)
const audioChunks = ref<Blob[]>([])
const recognitionState = ref<'idle' | 'recording' | 'analyzing'>('idle')
let recordTimer: ReturnType<typeof setTimeout> | null = null

const clearRecordTimer = () => {
  if (recordTimer !== null) {
    clearTimeout(recordTimer)
    recordTimer = null
  }
}

const startRecording = async () => {
  if (recognitionState.value !== 'idle') return

  // 检查浏览器兼容性及HTTPS环境
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    ElMessage.error('您的浏览器不支持录音或未在安全环境(HTTPS/localhost)下运行。')
    return
  }

  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder.value = new MediaRecorder(stream)
    audioChunks.value = []

    mediaRecorder.value.addEventListener("dataavailable", event => {
      audioChunks.value.push(event.data)
    })

    mediaRecorder.value.start()
    isRecording.value = true
    recognitionState.value = 'recording'

    // 录音5秒自动停止并识别
    recordTimer = setTimeout(() => {
      stopRecording()
    }, 5000)
  } catch (error) {
    console.error("无法获取麦克风权限:", error)
    ElMessage.error("无法获取麦克风权限，请检查浏览器设置。")
    recognitionState.value = 'idle'
  }
}

const stopRecording = () => {
  clearRecordTimer()
  if (mediaRecorder.value && mediaRecorder.value.state !== 'inactive') {
    // 使用 once 避免重复绑定
    mediaRecorder.value.addEventListener("stop", () => {
      const audioBlob = new Blob(audioChunks.value, { type: 'audio/wav' })
      // 停止所有轨道以释放麦克风
      mediaRecorder.value?.stream.getTracks().forEach(track => track.stop())
      sendAudioForRecognition(audioBlob)
    }, { once: true })
    mediaRecorder.value.stop()
    isRecording.value = false
    recognitionState.value = 'analyzing'
  }
}

const sendAudioForRecognition = async (audioBlob: Blob) => {
  const formData = new FormData()
  // 注意：字段名 'file' 必须与 Python 端 UploadFile 参数名一致
  formData.append('file', audioBlob, 'recording.wav')

  try {
    // 调用 Python 服务
    const response = await fetch('/shazam/recognize', { //在宝塔linux中改为/recognize
      method: 'POST',
      body: formData
    })

    if (!response.ok) {
      const errorData = await response.text();
      console.error('Server Error Detail:', errorData);
      throw new Error(`HTTP error! status: ${response.status} - ${errorData}`)
    }

    const result = await response.json()
    recognitionState.value = 'idle'
    if (result.track) {
      emit('success', result)
    } else {
      ElMessage.warning('未能识别歌曲')
    }

  } catch (error) {
    console.error("识别服务请求失败:", error)
    recognitionState.value = 'idle'
    ElMessage.error('识别服务请求失败')
  }
}

onUnmounted(() => {
  clearRecordTimer()
  if (mediaRecorder.value && mediaRecorder.value.state !== 'inactive') {
    mediaRecorder.value.stream.getTracks().forEach(track => track.stop())
    mediaRecorder.value.stop()
  }
})
</script>

<template>
  <button
    @click="startRecording"
    class="mr-recognizer-btn"
    :disabled="recognitionState !== 'idle'"
    :title="recognitionState === 'idle' ? '听歌识曲' : '正在识别...'"
  >
    <Icon
      v-if="recognitionState === 'analyzing'"
      icon="mdi:loading"
      class="mr-recognizer-icon mr-recognizer-loading"
    />
    <Icon
      v-else
      icon="mdi:microphone"
      class="mr-recognizer-icon"
      :class="{ 'mr-recognizer-recording': recognitionState === 'recording' }"
    />

    <span v-if="recognitionState === 'recording'" class="mr-recognizer-text">正在录音...</span>
    <span v-else-if="recognitionState === 'analyzing'" class="mr-recognizer-text">正在识别...</span>
  </button>
</template>

<style scoped>
.mr-recognizer-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: background-color 200ms ease;
}

.mr-recognizer-btn:hover:not(:disabled) {
  background-color: var(--bg-hover, rgba(255, 255, 255, 0.1));
}

.mr-recognizer-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.mr-recognizer-icon {
  font-size: 1.25rem;
  color: var(--text-subdued, #b3b3b3);
  transition: color 200ms ease;
}

.mr-recognizer-btn:hover:not(:disabled) .mr-recognizer-icon {
  color: var(--text-base, #fff);
}

.mr-recognizer-loading {
  color: #3b82f6;
  animation: spin 1s linear infinite;
}

.mr-recognizer-recording {
  color: #ef4444;
  animation: pulse 1s ease-in-out infinite;
}

.mr-recognizer-text {
  font-size: 0.875rem;
  color: var(--text-base, #fff);
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* Light Theme */
:root:not(.dark) .mr-recognizer-btn:hover:not(:disabled) {
  background-color: rgba(0, 0, 0, 0.08);
}

:root:not(.dark) .mr-recognizer-icon {
  color: #6a6a6a;
}

:root:not(.dark) .mr-recognizer-btn:hover:not(:disabled) .mr-recognizer-icon {
  color: #000000;
}

:root:not(.dark) .mr-recognizer-text {
  color: #000000;
}
</style>
