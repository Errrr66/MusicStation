<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['success'])

const isRecording = ref(false)
const mediaRecorder = ref<MediaRecorder | null>(null)
const audioChunks = ref<Blob[]>([])
const recognitionState = ref<'idle' | 'recording' | 'analyzing'>('idle')

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
    console.log("识别中...")

    // 录音5秒自动停止并识别
    setTimeout(() => {
      stopRecording()
    }, 5000)
  } catch (error) {
    console.error("无法获取麦克风权限:", error)
    ElMessage.error("无法获取麦克风权限，请检查浏览器设置。")
    recognitionState.value = 'idle'
  }
}

const stopRecording = () => {
  if (mediaRecorder.value && mediaRecorder.value.state !== 'inactive') {
    mediaRecorder.value.stop()
    mediaRecorder.value.addEventListener("stop", () => {
      const audioBlob = new Blob(audioChunks.value, { type: 'audio/wav' })
      // 停止所有轨道以释放麦克风
      mediaRecorder.value?.stream.getTracks().forEach(track => track.stop())
      sendAudioForRecognition(audioBlob)
    })
    console.log("录音结束")
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
    const response = await fetch('/recognize', {
      method: 'POST',
      body: formData
    })

    if (!response.ok) {
      const errorData = await response.text();
      console.error('Server Error Detail:', errorData);
      throw new Error(`HTTP error! status: ${response.status} - ${errorData}`)
    }

    const result = await response.json()
    console.log("识别结果:", result)
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
</script>

<template>
  <button
    @click="startRecording"
    class="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
    :disabled="recognitionState !== 'idle'"
    :title="recognitionState === 'idle' ? '听歌识曲' : '正在识别...'"
  >
    <Icon
      v-if="recognitionState === 'analyzing'"
      icon="mdi:loading"
      class="text-xl text-blue-500 animate-spin"
    />
    <Icon
      v-else
      icon="mdi:microphone"
      class="text-xl"
      :class="{ 'text-red-500 animate-pulse': recognitionState === 'recording' }"
    />

    <span v-if="recognitionState === 'recording'" class="text-sm">正在录音...</span>
    <span v-else-if="recognitionState === 'analyzing'" class="text-sm">正在识别...</span>
    <!-- Only show text on larger screens or tooltip -->
  </button>
</template>

<style scoped>
</style>
