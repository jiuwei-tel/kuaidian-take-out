import request from './request'

/**
 * 文件上传
 * 后端走阿里云 OSS（sky.alioss.* 配置在 application-dev.yml 里）。
 * 返回 Result<String>，data 就是图片的可访问 URL。
 * 如果 OSS 的 access-key 已失效，这里会返回 code=0 + "文件上传失败"，
 * 页面会提示你去手填图片地址。
 */
export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/admin/common/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000,
  })
}
