package com.ruoyi.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云OSS配置
 * 
 * @author 若依
 */
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig
{
    /** 服务接入点，形如 oss-cn-beijing.aliyuncs.com，不含协议和路径 */
    private static String endpoint;

    /** 存储空间名称 */
    private static String bucketName;

    /** 访问密钥ID */
    private static String accessKeyId;

    /** 访问密钥 */
    private static String accessKeySecret;

    public static String getEndpoint()
    {
        return endpoint;
    }

    /**
     * Spring Boot 绑定配置时调用。此处统一清洗取值，
     * 因为配置来自环境变量，很容易写成带 https:// 前缀、末尾带斜杠、
     * 或首尾含空格换行的形态；这类值传给SDK后会在拼接请求地址时抛出
     * 难以定位的 URISyntaxException，所以在入口就把它规整成纯主机名。
     */
    public void setEndpoint(String endpoint)
    {
        OssConfig.endpoint = normalizeEndpoint(endpoint);
    }

    public static String getBucketName()
    {
        return bucketName;
    }

    public void setBucketName(String bucketName)
    {
        OssConfig.bucketName = trim(bucketName);
    }

    public static String getAccessKeyId()
    {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId)
    {
        OssConfig.accessKeyId = trim(accessKeyId);
    }

    public static String getAccessKeySecret()
    {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret)
    {
        OssConfig.accessKeySecret = trim(accessKeySecret);
    }

    /**
     * 公共读Bucket的外网访问前缀，用于拼接文件完整地址
     */
    public static String getDomain()
    {
        return "https://" + bucketName + "." + endpoint + "/";
    }

    /**
     * 接入点归一化：去掉协议前缀、末尾斜杠和首尾空白
     */
    private static String normalizeEndpoint(String value)
    {
        String result = trim(value);
        if (result == null || result.isEmpty())
        {
            return result;
        }
        if (result.startsWith("https://"))
        {
            result = result.substring("https://".length());
        }
        else if (result.startsWith("http://"))
        {
            result = result.substring("http://".length());
        }
        if (result.endsWith("/"))
        {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * 去除首尾空白。密钥从控制台复制时极易带上空格或换行，
     * 而这种错误不会体现在报错信息里，只在签名比对时失败
     */
    private static String trim(String value)
    {
        return value == null ? null : value.trim();
    }
}
