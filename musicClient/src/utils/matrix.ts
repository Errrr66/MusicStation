export type Frame = number[][]
export type ColorPixel = { r: number; g: number; b: number; brightness: number }
export type ColorFrame = ColorPixel[][]

export function imageToFrame(
  imageUrl: string,
  rows: number,
  cols: number
): Promise<Frame> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      const canvas = document.createElement('canvas')
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        reject(new Error('Canvas context not available'))
        return
      }

      canvas.width = cols
      canvas.height = rows

      ctx.drawImage(img, 0, 0, cols, rows)

      const imageData = ctx.getImageData(0, 0, cols, rows)
      const pixels = imageData.data

      const frame: Frame = []
      for (let y = 0; y < rows; y++) {
        const row: number[] = []
        for (let x = 0; x < cols; x++) {
          const i = (y * cols + x) * 4
          const r = pixels[i]
          const g = pixels[i + 1]
          const b = pixels[i + 2]
          const brightness = (r * 0.299 + g * 0.587 + b * 0.114) / 255
          row.push(brightness)
        }
        frame.push(row)
      }

      resolve(frame)
    }
    img.onerror = () => reject(new Error('Failed to load image'))
    img.src = imageUrl
  })
}

export function imageToColorFrame(
  imageUrl: string,
  rows: number,
  cols: number
): Promise<ColorFrame> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      const canvas = document.createElement('canvas')
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        reject(new Error('Canvas context not available'))
        return
      }

      canvas.width = cols
      canvas.height = rows

      ctx.drawImage(img, 0, 0, cols, rows)

      const imageData = ctx.getImageData(0, 0, cols, rows)
      const pixels = imageData.data

      const frame: ColorFrame = []
      for (let y = 0; y < rows; y++) {
        const row: ColorPixel[] = []
        for (let x = 0; x < cols; x++) {
          const i = (y * cols + x) * 4
          const r = pixels[i]
          const g = pixels[i + 1]
          const b = pixels[i + 2]
          const brightness = (r * 0.299 + g * 0.587 + b * 0.114) / 255
          row.push({ r, g, b, brightness })
        }
        frame.push(row)
      }

      resolve(frame)
    }
    img.onerror = () => reject(new Error('Failed to load image'))
    img.src = imageUrl
  })
}

export type AnimationType = 
  | 'wave' 
  | 'scan' 
  | 'pulse' 
  | 'rain' 
  | 'breathe'
  | 'sparkle'
  | 'glow'
  | 'float'

export function createAnimatedFrames(
  colorFrame: ColorFrame,
  animationType: AnimationType = 'wave',
  frameCount: number = 12
): ColorFrame[] {
  const rows = colorFrame.length
  const cols = colorFrame[0]?.length || 0
  const frames: ColorFrame[] = []

  const sparkleMap: Map<string, number[]> = new Map()
  if (animationType === 'sparkle') {
    for (let y = 0; y < rows; y++) {
      for (let x = 0; x < cols; x++) {
        if (colorFrame[y][x].brightness > 0.3 && Math.random() > 0.7) {
          sparkleMap.set(`${y}-${x}`, Array.from({ length: frameCount }, () => Math.random()))
        }
      }
    }
  }

  for (let f = 0; f < frameCount; f++) {
    const frame: ColorFrame = []
    const progress = f / frameCount

    for (let y = 0; y < rows; y++) {
      const row: ColorPixel[] = []
      for (let x = 0; x < cols; x++) {
        const original = colorFrame[y][x]
        let brightness = original.brightness
        let { r, g, b } = original

        switch (animationType) {
          case 'wave': {
            const wave = Math.sin(progress * Math.PI * 2 + (x + y) * 0.3) * 0.3 + 0.7
            brightness = original.brightness * wave
            break
          }
          case 'scan': {
            const scanLine = (progress * rows * 2) % (rows * 2)
            const distance = Math.abs(y - scanLine)
            const intensity = distance < 3 ? 1.5 - distance * 0.3 : 0.7
            brightness = original.brightness * intensity
            break
          }
          case 'pulse': {
            const pulse = Math.sin(progress * Math.PI * 2) * 0.3 + 0.7
            brightness = original.brightness * pulse
            break
          }
          case 'rain': {
            const dropY = (progress * rows * 3 + x * 0.5) % rows
            const distance = Math.abs(y - dropY)
            const intensity = distance < 2 ? 1.3 - distance * 0.15 : 0.6
            brightness = original.brightness * intensity
            break
          }
          case 'breathe': {
            const breathe = Math.sin(progress * Math.PI * 2) * 0.15 + 0.85
            brightness = original.brightness * breathe
            break
          }
          case 'sparkle': {
            const key = `${y}-${x}`
            if (sparkleMap.has(key)) {
              const sparkleValues = sparkleMap.get(key)!
              const sparkleIndex = Math.floor(progress * sparkleValues.length)
              const sparkle = sparkleValues[sparkleIndex]
              if (sparkle > 0.8) {
                brightness = Math.min(1, original.brightness * 1.5)
                r = Math.min(255, original.r * 1.2)
                g = Math.min(255, original.g * 1.2)
                b = Math.min(255, original.b * 1.2)
              }
            }
            break
          }
          case 'glow': {
            const glow = Math.sin(progress * Math.PI * 2) * 0.2 + 0.8
            brightness = original.brightness * glow
            if (original.brightness > 0.5) {
              const glowBoost = Math.sin(progress * Math.PI * 2) * 0.3 + 0.7
              r = Math.min(255, original.r * glowBoost * 1.1)
              g = Math.min(255, original.g * glowBoost * 1.1)
              b = Math.min(255, original.b * glowBoost * 1.1)
            }
            break
          }
          case 'float': {
            const floatY = Math.sin(progress * Math.PI * 2) * 2
            const sourceY = Math.max(0, Math.min(rows - 1, Math.floor(y - floatY)))
            const sourcePixel = colorFrame[sourceY][x]
            brightness = sourcePixel.brightness
            r = sourcePixel.r
            g = sourcePixel.g
            b = sourcePixel.b
            break
          }
        }

        row.push({
          r,
          g,
          b,
          brightness: Math.max(0, Math.min(1, brightness)),
        })
      }
      frame.push(row)
    }
    frames.push(frame)
  }

  return frames
}
