/**
 * 来单提示音用 WebAudio 现场合成，不引入音频文件。
 */
let ctx = null

function getCtx() {
  if (!ctx) {
    const AC = window.AudioContext || window.webkitAudioContext
    if (!AC) return null
    ctx = new AC()
  }
  return ctx
}

function tone(audioCtx, freq, startAt, duration) {
  const osc = audioCtx.createOscillator()
  const gain = audioCtx.createGain()
  osc.type = 'sine'
  osc.frequency.value = freq
  gain.gain.setValueAtTime(0, startAt)
  gain.gain.linearRampToValueAtTime(0.22, startAt + 0.012)
  gain.gain.exponentialRampToValueAtTime(0.0001, startAt + duration)
  osc.connect(gain)
  gain.connect(audioCtx.destination)
  osc.start(startAt)
  osc.stop(startAt + duration + 0.02)
}

export function playOrderChime() {
  try {
    const audioCtx = getCtx()
    if (!audioCtx) return
    if (audioCtx.state === 'suspended') audioCtx.resume()
    const t = audioCtx.currentTime
    tone(audioCtx, 880, t, 0.16)
    tone(audioCtx, 1318.5, t + 0.13, 0.34)
  } catch {
    /* 浏览器不允许播放时静默失败，不打扰用户 */
  }
}
