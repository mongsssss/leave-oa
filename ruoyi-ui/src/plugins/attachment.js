/**
 * 附件地址处理
 *
 * 数据库 attachment_url 里存的是文件标识而不是完整网址，目前有两种形态：
 *   leave/2026/11/06/xxx.jpg   阿里云OSS中的key
 *   /profile/upload/...        早期本地上传留下的历史数据
 * 多个附件用逗号拼接存放在同一个字段里。
 *
 * 这里统一负责「标识 → 可访问地址」的还原和类型判断，业务页面不再关心分隔符、
 * 域名、历史数据兼容这些细节，以后接入CDN或换Bucket只改这一处。
 */

// OSS外网访问域名，配置在 .env.development / .env.production，不写在业务代码里
const ossDomain = process.env.VUE_APP_OSS_DOMAIN

// 后端接口前缀，开发环境为 /dev-api，由 webpack 代理到 8080
const baseApi = process.env.VUE_APP_BASE_API

// 多附件在字段里的分隔符，与后端 CommonController.FILE_DELIMITER 保持一致
const DELIMITER = ','

// 可以直接用图片方式渲染的类型；其余类型只能提供下载链接
const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp']

export default {
  /**
   * 把附件字段拆成标识数组
   * @param {String} attachmentUrl 数据库原始字段值
   * @return {Array} 标识数组，字段为空时返回空数组
   */
  list(attachmentUrl) {
    if (!attachmentUrl) {
      return []
    }
    return attachmentUrl.split(DELIMITER).filter(item => !!item)
  },

  /**
   * 附件数量
   * @param {String} attachmentUrl 数据库原始字段值
   * @return {Number} 个数
   */
  count(attachmentUrl) {
    return this.list(attachmentUrl).length
  },

  /**
   * 单个文件标识转完整访问地址
   * @param {String} key 文件标识
   * @return {String} 可访问地址，无法解析时返回空串
   */
  url(key) {
    if (!key) {
      return ''
    }
    // 已是带协议的完整地址，直接返回
    if (key.indexOf('http') === 0) {
      return key
    }
    // 历史本地上传数据，走后端 /profile 静态资源映射
    if (key.indexOf('/profile') === 0) {
      return baseApi + key
    }
    // 其余按OSS的key处理
    if (!ossDomain) {
      console.error('[attachment] 缺少 VUE_APP_OSS_DOMAIN 配置，附件地址无法生成')
      return ''
    }
    return ossDomain + key
  },

  /**
   * 标识数组批量转地址，供图片预览左右切换使用
   * @param {String} attachmentUrl 数据库原始字段值
   * @return {Array} 地址数组
   */
  urlList(attachmentUrl) {
    return this.list(attachmentUrl).map(item => this.url(item))
  },

  /**
   * 取第一个附件标识
   * @param {String} attachmentUrl 数据库原始字段值
   * @return {String} 标识，无附件时返回空串
   */
  first(attachmentUrl) {
    const list = this.list(attachmentUrl)
    return list.length > 0 ? list[0] : ''
  },

  /**
   * 第一个附件的访问地址，列表页缩略图和下载链接都用它
   * @param {String} attachmentUrl 数据库原始字段值
   * @return {String} 地址
   */
  firstUrl(attachmentUrl) {
    return this.url(this.first(attachmentUrl))
  },

  /**
   * 按标识后缀判断是否为图片
   * @param {String} key 文件标识
   * @return {Boolean} 是否可用图片方式展示
   */
  isImage(key) {
    if (!key) {
      return false
    }
    const index = key.lastIndexOf('.')
    if (index === -1) {
      return false
    }
    return imageExtensions.indexOf(key.substring(index + 1).toLowerCase()) !== -1
  },

  /**
   * 第一个附件是否为图片，决定列表页展示缩略图还是下载链接
   * @param {String} attachmentUrl 数据库原始字段值
   * @return {Boolean} 是否按图片展示
   */
  isFirstImage(attachmentUrl) {
    return this.isImage(this.first(attachmentUrl))
  }
}
