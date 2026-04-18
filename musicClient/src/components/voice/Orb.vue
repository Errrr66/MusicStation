<script setup lang="ts">
import * as THREE from 'three'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

type AgentState = null | 'thinking' | 'listening' | 'talking'

type VolumeMode = 'auto' | 'manual'

interface OrbProps {
  colors?: [string, string]
  colorsRef?: { value: [string, string] } | [string, string]
  seed?: number
  agentState?: AgentState
  volumeMode?: VolumeMode
  manualInput?: number
  manualOutput?: number
  getInputVolume?: () => number
  getOutputVolume?: () => number
}

const props = withDefaults(defineProps<OrbProps>(), {
  colors: () => ['#CADCFC', '#A0B9D1'],
  colorsRef: undefined,
  seed: undefined,
  agentState: null,
  volumeMode: 'auto',
  manualInput: 0,
  manualOutput: 0,
  getInputVolume: undefined,
  getOutputVolume: undefined,
})

const rootRef = ref<HTMLElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)

let renderer: THREE.WebGLRenderer | null = null
let scene: THREE.Scene | null = null
let camera: THREE.OrthographicCamera | null = null
let mesh: THREE.Mesh<THREE.PlaneGeometry, THREE.ShaderMaterial> | null = null
let rafId = 0
let resizeObserver: ResizeObserver | null = null
let startTs = 0
let smoothInput = 0
let smoothOutput = 0
let smoothState = 0
let currentColorA = new THREE.Color('#CADCFC')
let currentColorB = new THREE.Color('#A0B9D1')
let targetColorA = new THREE.Color('#CADCFC')
let targetColorB = new THREE.Color('#A0B9D1')

const stateToNumber = computed(() => {
  if (props.agentState === 'thinking') return 1
  if (props.agentState === 'listening') return 2
  if (props.agentState === 'talking') return 3
  return 0
})

const clamp01 = (value: number) => Math.max(0, Math.min(1, value))

const createSeed = () => {
  if (typeof props.seed === 'number') return props.seed
  return Math.floor(Math.random() * 100000)
}

const getVolumes = () => {
  if (props.volumeMode === 'manual') {
    return {
      input: clamp01(props.manualInput),
      output: clamp01(props.manualOutput),
    }
  }
  return {
    input: clamp01(props.getInputVolume?.() ?? 0),
    output: clamp01(props.getOutputVolume?.() ?? 0),
  }
}

const isColorTuple = (value: unknown): value is [string, string] =>
  Array.isArray(value) && value.length === 2

const isColorRefObject = (value: unknown): value is { value: [string, string] } =>
  typeof value === 'object' &&
  value !== null &&
  'value' in value &&
  isColorTuple((value as { value: unknown }).value)

const resolveColors = (): [string, string] => {
  const fromRefOrValue = props.colorsRef
  if (isColorTuple(fromRefOrValue)) {
    return fromRefOrValue
  }
  if (isColorRefObject(fromRefOrValue)) {
    return fromRefOrValue.value
  }
  return props.colors
}

const updateTargetColors = (next: [string, string]) => {
  targetColorA.set(next[0])
  targetColorB.set(next[1])
}

const vertexShader = `
  varying vec2 vUv;
  void main() {
    vUv = uv;
    gl_Position = vec4(position, 1.0);
  }
`

const fragmentShader = `
  precision highp float;

  uniform vec2 u_resolution;
  uniform float u_time;
  uniform float u_input;
  uniform float u_output;
  uniform float u_state;
  uniform float u_seed;
  uniform vec3 u_colorA;
  uniform vec3 u_colorB;

  varying vec2 vUv;

  float hash(float n) {
    return fract(sin(n) * 43758.5453123);
  }

  float noise(vec2 x) {
    vec2 i = floor(x);
    vec2 f = fract(x);

    float a = hash(i.x + i.y * 57.0 + u_seed);
    float b = hash(i.x + 1.0 + i.y * 57.0 + u_seed);
    float c = hash(i.x + (i.y + 1.0) * 57.0 + u_seed);
    float d = hash(i.x + 1.0 + (i.y + 1.0) * 57.0 + u_seed);

    vec2 u = f * f * (3.0 - 2.0 * f);

    return mix(a, b, u.x) +
      (c - a) * u.y * (1.0 - u.x) +
      (d - b) * u.x * u.y;
  }

  mat2 rotate2d(float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return mat2(c, -s, s, c);
  }

  void main() {
    vec2 uv = vUv * 2.0 - 1.0;
    uv.x *= u_resolution.x / max(u_resolution.y, 1.0);

    float stateBoost = 0.0;
    if (u_state > 0.5 && u_state < 1.5) {
      stateBoost = 0.05;
    } else if (u_state > 1.5 && u_state < 2.5) {
      stateBoost = 0.09;
    } else if (u_state > 2.5) {
      stateBoost = 0.14;
    }

    float audioBoost = 0.18 * u_output + 0.1 * u_input;
    float pulse = sin(u_time * (1.5 + u_output * 2.0)) * 0.02;
    float radius = 0.56 + stateBoost + audioBoost + pulse;

    vec2 nUv = rotate2d(u_time * 0.1) * uv;
    float n = noise(nUv * (3.2 + u_input * 2.0) + vec2(u_time * 0.4, -u_time * 0.3));
    n += 0.5 * noise(nUv * 6.7 - vec2(u_time * 0.6, u_time * 0.45));
    n = n / 1.5;

    float edge = radius + (n - 0.5) * 0.18;
    float dist = length(uv);
    float orbMask = smoothstep(edge, edge - 0.12, dist);

    vec3 grad = mix(u_colorA, u_colorB, clamp(0.5 + uv.y * 0.45 + (n - 0.5) * 0.25, 0.0, 1.0));

    float glow = smoothstep(edge + 0.22, edge - 0.06, dist);
    vec3 glowColor = mix(u_colorB, u_colorA, 0.6) * glow * 0.35;

    float core = smoothstep(0.0, edge * 0.9, edge - dist);
    vec3 coreLight = vec3(0.12, 0.14, 0.2) * core;

    vec3 color = grad * orbMask + glowColor + coreLight;
    float alpha = clamp(max(orbMask, glow * 0.65), 0.0, 1.0);

    gl_FragColor = vec4(color, alpha);
  }
`

const updateResolution = () => {
  if (!renderer || !mesh || !rootRef.value) return
  const width = Math.max(1, rootRef.value.clientWidth)
  const height = Math.max(1, rootRef.value.clientHeight)
  renderer.setSize(width, height, false)
  mesh.material.uniforms.u_resolution.value.set(width, height)
}

const init = () => {
  if (!canvasRef.value || !rootRef.value) return

  renderer = new THREE.WebGLRenderer({
    canvas: canvasRef.value,
    alpha: true,
    antialias: true,
    powerPreference: 'high-performance',
  })

  scene = new THREE.Scene()
  camera = new THREE.OrthographicCamera(-1, 1, 1, -1, 0, 1)

  const uniforms = {
    u_resolution: { value: new THREE.Vector2(1, 1) },
    u_time: { value: 0 },
    u_input: { value: 0 },
    u_output: { value: 0 },
    u_state: { value: 0 },
    u_seed: { value: createSeed() },
    u_colorA: { value: new THREE.Color(resolveColors()[0]) },
    u_colorB: { value: new THREE.Color(resolveColors()[1]) },
  }

  currentColorA = uniforms.u_colorA.value.clone()
  currentColorB = uniforms.u_colorB.value.clone()
  targetColorA = currentColorA.clone()
  targetColorB = currentColorB.clone()

  const material = new THREE.ShaderMaterial({
    uniforms,
    vertexShader,
    fragmentShader,
    transparent: true,
  })

  mesh = new THREE.Mesh(new THREE.PlaneGeometry(2, 2), material)
  scene.add(mesh)

  updateResolution()

  resizeObserver = new ResizeObserver(updateResolution)
  resizeObserver.observe(rootRef.value)

  startTs = performance.now()

  const loop = (ts: number) => {
    if (!renderer || !scene || !camera || !mesh) return

    const elapsed = (ts - startTs) / 1000
    const { input, output } = getVolumes()

    // Smooth transitions for audio-reactivity and state switches.
    smoothInput += (input - smoothInput) * 0.12
    smoothOutput += (output - smoothOutput) * 0.12
    smoothState += (stateToNumber.value - smoothState) * 0.1
    currentColorA.lerp(targetColorA, 0.08)
    currentColorB.lerp(targetColorB, 0.08)

    mesh.material.uniforms.u_time.value = elapsed
    mesh.material.uniforms.u_input.value = smoothInput
    mesh.material.uniforms.u_output.value = smoothOutput
    mesh.material.uniforms.u_state.value = smoothState
    mesh.material.uniforms.u_colorA.value.copy(currentColorA)
    mesh.material.uniforms.u_colorB.value.copy(currentColorB)

    renderer.render(scene, camera)
    rafId = requestAnimationFrame(loop)
  }

  rafId = requestAnimationFrame(loop)
}

const cleanup = () => {
  if (rafId) cancelAnimationFrame(rafId)
  rafId = 0

  resizeObserver?.disconnect()
  resizeObserver = null

  if (mesh) {
    mesh.geometry.dispose()
    mesh.material.dispose()
    mesh = null
  }

  if (renderer) {
    renderer.dispose()
    renderer = null
  }

  scene = null
  camera = null
}

watch(
  () => props.colors,
  (next) => {
    updateTargetColors(next)
  },
  { deep: true }
)

watch(
  () => props.colorsRef,
  (next) => {
    if (!next) return
    if (isColorTuple(next)) {
      updateTargetColors(next)
      return
    }
    if (isColorRefObject(next)) {
      updateTargetColors(next.value)
    }
  },
  { deep: true }
)

onMounted(init)
onBeforeUnmount(cleanup)
</script>

<template>
  <div ref="rootRef" class="orb-root">
    <canvas ref="canvasRef" class="orb-canvas" />
  </div>
</template>

<style scoped>
.orb-root {
  position: relative;
  width: 100%;
  height: 100%;
}

.orb-canvas {
  width: 100%;
  height: 100%;
  display: block;
}
</style>

