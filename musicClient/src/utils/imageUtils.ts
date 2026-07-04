/**
 * 从图片 URL 提取主色调
 * 以 artist/[id].vue 版本为基准，合并 like/playlist 中的重复实现
 * @param imageUrl 图片地址
 * @param defaultColor 提取失败时的回退颜色
 * @returns CSS 颜色字符串（rgb 格式）
 */
export function extractDominantColor(
  imageUrl: string,
  defaultColor = '#1e3a5f'
): Promise<string> {
  return new Promise((resolve) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      const canvas = document.createElement('canvas')
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        resolve(defaultColor)
        return
      }

      const sampleWidth = 50
      const sampleHeight = 50
      canvas.width = sampleWidth
      canvas.height = sampleHeight
      ctx.drawImage(img, 0, 0, sampleWidth, sampleHeight)
      const pixels = ctx.getImageData(0, 0, sampleWidth, sampleHeight).data

      let r = 0
      let g = 0
      let b = 0
      let count = 0
      for (let i = 0; i < pixels.length; i += 4) {
        const alpha = pixels[i + 3]
        if (alpha > 16) {
          r += pixels[i]
          g += pixels[i + 1]
          b += pixels[i + 2]
          count++
        }
      }

      if (!count) {
        resolve(defaultColor)
        return
      }

      const avgR = Math.round(r / count)
      const avgG = Math.round(g / count)
      const avgB = Math.round(b / count)
      // 略微压暗，避免顶部过亮影响文字可读性
      const darken = 0.72
      const finalR = Math.max(0, Math.round(avgR * darken))
      const finalG = Math.max(0, Math.round(avgG * darken))
      const finalB = Math.max(0, Math.round(avgB * darken))
      resolve(`rgb(${finalR}, ${finalG}, ${finalB})`)
    }
    img.onerror = () => resolve(defaultColor)
    img.src = imageUrl
  })
}
