package com.butterfly.framework.core.rpc;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.SimplePropertyPreFilter;
import com.butterfly.framework.core.exception.SerializationException;

public class FastJsonSerializer implements Serializer {

    // 安全配置（生产环境建议通过配置类动态加载）
    private static final JSONWriter.Feature[] SERIALIZE_FEATURES = {
            JSONWriter.Feature.WriteClassName,
            JSONWriter.Feature.BrowserCompatible,
            JSONWriter.Feature.IgnoreNoneSerializable
    };

    private static final JSONReader.Feature[] DESERIALIZE_FEATURES = {
            JSONReader.Feature.SupportAutoType,
            JSONReader.Feature.UseNativeObject
    };

    private static final SimplePropertyPreFilter SENSITIVE_FILTER =
            new SimplePropertyPreFilter() {{
                getExcludes().add("password");
                getExcludes().add("token");
                getExcludes().add("secretKey");
            }};

    @Override
    public byte[] serialize(Object object) {
        if (object == null) {
            return new byte[0];
        }
        try {
            return JSON.toJSONBytes(object, SERIALIZE_FEATURES);
        } catch (Exception e) {
            throw new SerializationException("FastJSON2序列化失败", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        if (data == null || data.length == 0 || clazz == null) {
            return null;
        }
        try {
            return JSON.parseObject(data, clazz, DESERIALIZE_FEATURES);
        } catch (Exception e) {
            throw new SerializationException("FastJSON2反序列化失败", e);
        }
    }

    // 安全反序列化方法
    public <T> T safeDeserialize(byte[] data, Class<T> clazz) {
        try {
            return JSON.parseObject(
                    data,
                    clazz,
                    SENSITIVE_FILTER,
                    DESERIALIZE_FEATURES
            );
        } catch (Exception e) {
            throw new SerializationException("安全反序列化失败", e);
        }
    }
}