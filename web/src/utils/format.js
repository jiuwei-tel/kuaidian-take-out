/**
 * 后端 JacksonObjectMapper 把 LocalDateTime 序列化成 "yyyy-MM-dd HH:mm"（注意不带秒），
 * 所以这里不要再自作主张地解析成 Date 再格式化，否则时区会把它改掉。
 * 这里只做「空值兜底」和「把 T 换成空格」两件事。
 */
export function fmtDateTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

export function fmtDate(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 10)
}

/** 金额显示：统一两位小数 */
export function fmtMoney(value) {
  const n = Number(value)
  if (value === null || value === undefined || Number.isNaN(n)) return '0.00'
  return n.toFixed(2)
}

/** 百分比显示：后端给的是 0~1 的小数 */
export function fmtPercent(value, digits = 1) {
  const n = Number(value)
  if (Number.isNaN(n)) return '0%'
  return `${(n * 100).toFixed(digits)}%`
}

/**
 * 后端各个报表接口返回的都是「逗号分隔的字符串」，例如：
 *   turnoverList: "406.0,1520.0,75.0"
 * 统一在这里拆开，顺便把空串过滤掉。
 */
export function splitList(str) {
  if (!str) return []
  return String(str)
    .split(',')
    .map((s) => s.trim())
    .filter((s) => s !== '')
}

/** 同上，但转成数字数组，供 ECharts 用 */
export function splitNumList(str) {
  return splitList(str).map((s) => {
    const n = Number(s)
    return Number.isNaN(n) ? 0 : n
  })
}

/** 后端返回的图片可能是完整 URL，也可能是空 */
export function fmtImage(url) {
  if (!url) return ''
  return url
}
