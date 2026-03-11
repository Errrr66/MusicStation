<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from "vue";
import { useMouse } from "@vueuse/core";
import EyeBall from "./EyeBall.vue";
import Pupil from "./Pupil.vue";

const props = withDefaults(
  defineProps<{
    isTyping?: boolean;
    showPassword?: boolean;
    passwordLength?: number;
  }>(),
  {
    isTyping: false,
    showPassword: false,
    passwordLength: 0
  }
);

const { x: mouseX, y: mouseY } = useMouse();

const isPurpleBlinking = ref(false);
const isBlackBlinking = ref(false);
const isLookingAtEachOther = ref(false);
const isPurplePeeking = ref(false);

const purpleRef = ref<HTMLDivElement | null>(null);
const blackRef = ref<HTMLDivElement | null>(null);
const yellowRef = ref<HTMLDivElement | null>(null);
const orangeRef = ref<HTMLDivElement | null>(null);

// Blinking logic
const setupBlink = (blinkingRef: typeof isPurpleBlinking) => {
  const getRandomBlinkInterval = () => Math.random() * 4000 + 3000;
  let timeout: ReturnType<typeof setTimeout>;

  const scheduleBlink = () => {
    timeout = setTimeout(() => {
      blinkingRef.value = true;
      setTimeout(() => {
        blinkingRef.value = false;
        scheduleBlink();
      }, 150);
    }, getRandomBlinkInterval());
  };

  scheduleBlink();
  return () => clearTimeout(timeout);
};

onMounted(() => {
  const clearPurple = setupBlink(isPurpleBlinking);
  const clearBlack = setupBlink(isBlackBlinking);

  onUnmounted(() => {
    clearPurple();
    clearBlack();
  });
});

// Looking at each other
watch(
  () => props.isTyping,
  val => {
    if (val) {
      isLookingAtEachOther.value = true;
      setTimeout(() => {
        isLookingAtEachOther.value = false;
      }, 800);
    } else {
      isLookingAtEachOther.value = false;
    }
  }
);

// Purple peeking
watch(
  [() => props.passwordLength, () => props.showPassword, isPurplePeeking],
  ([len, show, peeking], _, onCleanup) => {
    if (len > 0 && show) {
      const schedulePeek = () => {
        const timeout = setTimeout(
          () => {
            isPurplePeeking.value = true;
            setTimeout(() => {
              isPurplePeeking.value = false;
            }, 800);
          },
          Math.random() * 3000 + 2000
        );
        return timeout;
      };
      const t = schedulePeek();
      onCleanup(() => clearTimeout(t));
    } else {
      isPurplePeeking.value = false;
    }
  }
);

// Helper for position calculation
const calculatePosition = (elementRef: HTMLDivElement | null) => {
  if (!elementRef) return { faceX: 0, faceY: 0, bodySkew: 0 };

  const rect = elementRef.getBoundingClientRect();
  const centerX = rect.left + rect.width / 2;
  const centerY = rect.top + rect.height / 3;

  const deltaX = mouseX.value - centerX;
  const deltaY = mouseY.value - centerY;

  const faceX = Math.max(-15, Math.min(15, deltaX / 20));
  const faceY = Math.max(-10, Math.min(10, deltaY / 30));
  const bodySkew = Math.max(-6, Math.min(6, -deltaX / 120));

  return { faceX, faceY, bodySkew };
};

const purplePos = computed(() => calculatePosition(purpleRef.value));
const blackPos = computed(() => calculatePosition(blackRef.value));
const yellowPos = computed(() => calculatePosition(yellowRef.value));
const orangePos = computed(() => calculatePosition(orangeRef.value));

const isHidingPassword = computed(
  () => props.passwordLength > 0 && !props.showPassword
);
</script>

<template>
  <div
    class="relative origin-top-left xl:scale-95 2xl:scale-100"
    style="width: 550px; height: 400px"
  >
    <!-- Purple Character -->
    <div
      ref="purpleRef"
      class="absolute bottom-0 transition-all duration-700 ease-in-out"
      :style="{
        left: '70px',
        width: '180px',
        height: isTyping || isHidingPassword ? '440px' : '400px',
        backgroundColor: '#6C3FF5',
        borderRadius: '10px 10px 0 0',
        zIndex: 1,
        transform:
          passwordLength > 0 && showPassword
            ? `skewX(0deg)`
            : isTyping || isHidingPassword
              ? `skewX(${(purplePos.bodySkew || 0) - 12}deg) translateX(40px)`
              : `skewX(${purplePos.bodySkew || 0}deg)`,
        transformOrigin: 'bottom center'
      }"
    >
      <div
        class="absolute flex gap-8 transition-all duration-700 ease-in-out"
        :style="{
          left:
            passwordLength > 0 && showPassword
              ? `${20}px`
              : isLookingAtEachOther
                ? `${55}px`
                : `${45 + purplePos.faceX}px`,
          top:
            passwordLength > 0 && showPassword
              ? `${35}px`
              : isLookingAtEachOther
                ? `${65}px`
                : `${40 + purplePos.faceY}px`
        }"
      >
        <EyeBall
          :size="18"
          :pupilSize="7"
          :maxDistance="5"
          eyeColor="white"
          pupilColor="#2D2D2D"
          :isBlinking="isPurpleBlinking"
          :forceLookX="
            passwordLength > 0 && showPassword
              ? isPurplePeeking
                ? 4
                : -4
              : isLookingAtEachOther
                ? 3
                : undefined
          "
          :forceLookY="
            passwordLength > 0 && showPassword
              ? isPurplePeeking
                ? 5
                : -4
              : isLookingAtEachOther
                ? 4
                : undefined
          "
        />
        <EyeBall
          :size="18"
          :pupilSize="7"
          :maxDistance="5"
          eyeColor="white"
          pupilColor="#2D2D2D"
          :isBlinking="isPurpleBlinking"
          :forceLookX="
            passwordLength > 0 && showPassword
              ? isPurplePeeking
                ? 4
                : -4
              : isLookingAtEachOther
                ? 3
                : undefined
          "
          :forceLookY="
            passwordLength > 0 && showPassword
              ? isPurplePeeking
                ? 5
                : -4
              : isLookingAtEachOther
                ? 4
                : undefined
          "
        />
      </div>
    </div>

    <!-- Black Character -->
    <div
      ref="blackRef"
      class="absolute bottom-0 transition-all duration-700 ease-in-out"
      :style="{
        left: '240px',
        width: '120px',
        height: '310px',
        backgroundColor: '#2D2D2D',
        borderRadius: '8px 8px 0 0',
        zIndex: 2,
        transform:
          passwordLength > 0 && showPassword
            ? `skewX(0deg)`
            : isLookingAtEachOther
              ? `skewX(${(blackPos.bodySkew || 0) * 1.5 + 10}deg) translateX(20px)`
              : isTyping || isHidingPassword
                ? `skewX(${(blackPos.bodySkew || 0) * 1.5}deg)`
                : `skewX(${blackPos.bodySkew || 0}deg)`,
        transformOrigin: 'bottom center'
      }"
    >
      <div
        class="absolute flex gap-6 transition-all duration-700 ease-in-out"
        :style="{
          left:
            passwordLength > 0 && showPassword
              ? `${10}px`
              : isLookingAtEachOther
                ? `${32}px`
                : `${26 + blackPos.faceX}px`,
          top:
            passwordLength > 0 && showPassword
              ? `${28}px`
              : isLookingAtEachOther
                ? `${12}px`
                : `${32 + blackPos.faceY}px`
        }"
      >
        <EyeBall
          :size="16"
          :pupilSize="6"
          :maxDistance="4"
          eyeColor="white"
          pupilColor="#2D2D2D"
          :isBlinking="isBlackBlinking"
          :forceLookX="
            passwordLength > 0 && showPassword
              ? -4
              : isLookingAtEachOther
                ? 0
                : undefined
          "
          :forceLookY="
            passwordLength > 0 && showPassword
              ? -4
              : isLookingAtEachOther
                ? -4
                : undefined
          "
        />
        <EyeBall
          :size="16"
          :pupilSize="6"
          :maxDistance="4"
          eyeColor="white"
          pupilColor="#2D2D2D"
          :isBlinking="isBlackBlinking"
          :forceLookX="
            passwordLength > 0 && showPassword
              ? -4
              : isLookingAtEachOther
                ? 0
                : undefined
          "
          :forceLookY="
            passwordLength > 0 && showPassword
              ? -4
              : isLookingAtEachOther
                ? -4
                : undefined
          "
        />
      </div>
    </div>

    <!-- Orange Character -->
    <div
      ref="orangeRef"
      class="absolute bottom-0 transition-all duration-700 ease-in-out"
      :style="{
        left: '0px',
        width: '240px',
        height: '200px',
        zIndex: 3,
        backgroundColor: '#FF9B6B',
        borderRadius: '120px 120px 0 0',
        transform:
          passwordLength > 0 && showPassword
            ? `skewX(0deg)`
            : `skewX(${orangePos.bodySkew || 0}deg)`,
        transformOrigin: 'bottom center'
      }"
    >
      <div
        class="absolute flex gap-8 transition-all duration-200 ease-out"
        :style="{
          left:
            passwordLength > 0 && showPassword
              ? `${50}px`
              : `${82 + (orangePos.faceX || 0)}px`,
          top:
            passwordLength > 0 && showPassword
              ? `${85}px`
              : `${90 + (orangePos.faceY || 0)}px`
        }"
      >
        <Pupil
          :size="12"
          :maxDistance="5"
          pupilColor="#2D2D2D"
          :forceLookX="passwordLength > 0 && showPassword ? -5 : undefined"
          :forceLookY="passwordLength > 0 && showPassword ? -4 : undefined"
        />
        <Pupil
          :size="12"
          :maxDistance="5"
          pupilColor="#2D2D2D"
          :forceLookX="passwordLength > 0 && showPassword ? -5 : undefined"
          :forceLookY="passwordLength > 0 && showPassword ? -4 : undefined"
        />
      </div>
    </div>

    <!-- Yellow Character -->
    <div
      ref="yellowRef"
      class="absolute bottom-0 transition-all duration-700 ease-in-out"
      :style="{
        left: '310px',
        width: '140px',
        height: '230px',
        backgroundColor: '#E8D754',
        borderRadius: '70px 70px 0 0',
        zIndex: 4,
        transform:
          passwordLength > 0 && showPassword
            ? `skewX(0deg)`
            : `skewX(${yellowPos.bodySkew || 0}deg)`,
        transformOrigin: 'bottom center'
      }"
    >
      <div
        class="absolute flex gap-6 transition-all duration-200 ease-out"
        :style="{
          left:
            passwordLength > 0 && showPassword
              ? `${20}px`
              : `${52 + (yellowPos.faceX || 0)}px`,
          top:
            passwordLength > 0 && showPassword
              ? `${35}px`
              : `${40 + (yellowPos.faceY || 0)}px`
        }"
      >
        <Pupil
          :size="12"
          :maxDistance="5"
          pupilColor="#2D2D2D"
          :forceLookX="passwordLength > 0 && showPassword ? -5 : undefined"
          :forceLookY="passwordLength > 0 && showPassword ? -4 : undefined"
        />
        <Pupil
          :size="12"
          :maxDistance="5"
          pupilColor="#2D2D2D"
          :forceLookX="passwordLength > 0 && showPassword ? -5 : undefined"
          :forceLookY="passwordLength > 0 && showPassword ? -4 : undefined"
        />
      </div>
      <!-- Mouth -->
      <div
        class="absolute w-20 h-[4px] bg-[#2D2D2D] rounded-full transition-all duration-200 ease-out"
        :style="{
          left:
            passwordLength > 0 && showPassword
              ? `${10}px`
              : `${40 + (yellowPos.faceX || 0)}px`,
          top:
            passwordLength > 0 && showPassword
              ? `${88}px`
              : `${88 + (yellowPos.faceY || 0)}px`
        }"
      />
    </div>
  </div>
</template>
