package com.ruoyi.common.utils.file;

import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.ruoyi.common.config.OssConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;

/**
 * 阿里云OSS上传处理
 * 
 * @author ruoyi
 */
public class OssUtils
{
    private static final Logger log = LoggerFactory.getLogger(OssUtils.class);

    /** 附件在Bucket中的目录前缀 */
    private static final String ATTACHMENT_PREFIX = "leave";

    /**
     * 允许上传的文件后缀。
     * 没有直接复用 MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION，因为其中含 zip/rmvb 等
     * 与请假业务无关的类型，公共读的Bucket一旦被人当成免费网盘会产生费用和风险。
     */
    private static final String[] ALLOWED_EXTENSION = { "jpg", "jpeg", "png", "gif", "bmp", "webp", "pdf", "doc",
            "docx", "xls", "xlsx", "ppt", "pptx", "txt" };

    /** OSS客户端内部维护HTTP连接池且线程安全，全局复用同一个实例 */
    private static volatile OSS client;

    /**
     * 上传文件到OSS
     * 
     * @param file 待上传文件
     * @return 文件在Bucket中的key
     */
    public static String upload(MultipartFile file)
    {
        checkConfig();
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("上传文件不能为空");
        }
        validate(file);

        String key = buildKey(file);
        try (InputStream in = file.getInputStream())
        {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            // ContentType决定浏览器是预览还是下载，为空时不设置，交由SDK按key后缀推断
            if (StringUtils.isNotEmpty(file.getContentType()))
            {
                metadata.setContentType(file.getContentType());
            }
            getClient().putObject(OssConfig.getBucketName(), key, in, metadata);
            log.info("上传OSS成功, 原文件名={}, key={}", file.getOriginalFilename(), key);
            return key;
        }
        catch (IOException e)
        {
            log.error("读取上传文件失败, key={}", key, e);
            throw new ServiceException("文件读取失败，请重新上传");
        }
        catch (Exception e)
        {
            log.error("上传OSS失败, key={}", key, e);
            throw new ServiceException("文件上传失败，请稍后重试");
        }
    }

    /**
     * 校验文件后缀与大小。
     * 复用 FileUploadUtils 的校验逻辑，使OSS和本地上传两条路径的限制保持一致；
     * 大小上限由 spring.servlet.multipart.max-file-size 在容器层先行拦截，此处兜底。
     */
    private static void validate(MultipartFile file)
    {
        try
        {
            FileUploadUtils.assertAllowed(file, ALLOWED_EXTENSION);
        }
        catch (Exception e)
        {
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 生成文件在Bucket中的key：业务前缀/日期/UUID.后缀
     * 不使用原始文件名，一是避免同名互相覆盖，二是避免姓名等信息出现在可公开访问的地址中
     */
    private static String buildKey(MultipartFile file)
    {
        String extension = FileUploadUtils.getExtension(file);
        if (StringUtils.isEmpty(extension))
        {
            throw new ServiceException("无法识别文件类型，上传被拒绝");
        }
        return ATTACHMENT_PREFIX + "/" + DateUtils.datePath() + "/" + IdUtils.fastSimpleUUID() + "." + extension;
    }

    /**
     * 配置缺失时直接给出原因，避免抛出难以定位的SDK内部异常
     */
    private static void checkConfig()
    {
        if (StringUtils.isEmpty(OssConfig.getEndpoint()) || StringUtils.isEmpty(OssConfig.getBucketName())
                || StringUtils.isEmpty(OssConfig.getAccessKeyId())
                || StringUtils.isEmpty(OssConfig.getAccessKeySecret()))
        {
            throw new ServiceException("OSS配置不完整，请检查环境变量 OSS_ENDPOINT / OSS_BUCKET_NAME / OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET");
        }
    }

    /**
     * 懒加载全局客户端。每次上传新建会反复建立连接、创建后台线程，
     * 复用单例后仅在首次使用时创建；AccessKey变更后需重启应用才会生效。
     */
    private static OSS getClient()
    {
        if (client == null)
        {
            synchronized (OssUtils.class)
            {
                if (client == null)
                {
                    client = new OSSClientBuilder().build(OssConfig.getEndpoint(), OssConfig.getAccessKeyId(),
                            OssConfig.getAccessKeySecret());
                }
            }
        }
        return client;
    }
}
