package com.butterfly.framework.core.rpc;

/**
 * 序列化接口，定义对象与字节数组之间的转换规范
 */
public interface Serializer {
    
    /**
     * 将对象序列化为字节数组
     * @param object 待序列化的对象
     * @return 序列化后的字节数组
     */
    byte[] serialize(Object object);
    
    /**
     * 将字节数组反序列化为指定类型的对象
     * @param data 待反序列化的字节数组
     * @param clazz 目标对象类型
     * @param <T> 泛型类型
     * @return 反序列化后的对象
     */
    <T> T deserialize(byte[] data, Class<T> clazz);

}