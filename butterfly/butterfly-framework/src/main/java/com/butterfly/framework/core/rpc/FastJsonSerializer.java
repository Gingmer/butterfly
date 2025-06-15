package com.butterfly.framework.core.rpc;

import com.alibaba.fastjson.JSON;
import java.nio.charset.StandardCharsets;

/**
 * FastJSON实现的序列化器，将对象与JSON字节数组互相转换
 */
public class FastJsonSerializer implements Serializer {
    
    @Override
    public byte[] serialize(Object object) {
        // 使用FastJSON将对象序列化为UTF-8编码的字节数组
        return JSON.toJSONBytes(object, StandardCharsets.UTF_8);
    }
    
    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        // 使用FastJSON将字节数组反序列化为指定类型的对象
        return JSON.parseObject(data, clazz, StandardCharsets.UTF_8);
    }
}