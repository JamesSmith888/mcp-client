package com.jim.mcpclient.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author marco.zheng   (cq_zheng@trip.com)
 * @version 1.0
 * @date 2022/03/09
 * @desc Json工具类
 * @see
 * @since 1.0
 */
public class JsonUtils {

    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);
    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        // 对象的所有字段全部列入
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        // 忽略大小写敏感
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        // 枚举使用默认值处理
        objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);
        //支持NULL和空字符串
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        // 忽略空Bean转json的错误
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 所有的日期格式都统一为以下的样式，即yyyy-MM-dd HH:mm:ss
        // 忽略 在json字符串中存在，但是在java对象中不存在对应属性的情况。防止错误
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private JsonUtils() {
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * 对象转Json格式字符串
     *
     * @param obj 对象
     * @return Json格式字符串
     */
    public static <T> String toJsonString(T obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 对象转Json格式字符串(格式化的Json字符串)
     *
     * @param obj 对象
     * @return 美化的Json格式字符串
     */
    public static <T> String toPrettyJsonString(T obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 字符串转换为自定义对象
     *
     * @param str   要转换的字符串
     * @param clazz 自定义对象的class对象
     * @return 自定义对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T parse(String str, Class<T> clazz) {
        if (ObjectUtils.isEmpty(str) || clazz == null) {
            return null;
        }
        try {
            return objectMapper.readValue(str, clazz);
        } catch (Exception e) {
            log.warn("Json parse failed for string: {}, will return original string", str, e);
            return (T) str;
        }
    }

//    public static void main(String[] args) {
//        String s1 = toJsonString("123");
//        System.out.println(parse(s1, new TypeReference<String>() {
//        }));
//        System.out.println(isJsonType(s1));
//        System.out.println(s1);
//        System.out.println(parse(s1, String.class));
//        System.out.println(parse(s1, Object.class));
//        String s = parse("123", String.class);
//        System.out.println(s);
//    }

    @SuppressWarnings("unchecked")
    public static <T> T parse(String str, TypeReference<T> typeReference) {
        if (StringUtils.isEmpty(str) || typeReference == null) {
            return null;
        }
        try {
            return objectMapper.readValue(str, typeReference);
        } catch (IOException e) {
            log.warn("Json parse failed for string: {}, will return original string", str, e);
            return (T) str;
        }
    }

    /**
     * json字符串转成map的
     *
     * @param str
     * @return
     */
    public static <T> Map<String, T> parseMap(String str) {
        return parse(str, new TypeReference<Map<String, T>>() {
        });
    }

    public static <T> List<T> parseArray(String str, Class<T> clazz) {
        try {
            return mapJsonToObjectList(str, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T convert(Object obj, TypeReference<T> typeReference) {
        try {
            return getObjectMapper().convertValue(obj, typeReference);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T convert(Object obj, Class<T> clazz) {
        try {
            return getObjectMapper().convertValue(obj, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> List<T> mapJsonToObjectList(String json, Class<T> clazz) throws JsonProcessingException {
        TypeFactory t = TypeFactory.defaultInstance();
        return getObjectMapper().readValue(json, t.constructCollectionType(ArrayList.class, clazz));
    }

    public static <T> T parse(String str, Class<?> collectionClazz, Class<?>... elementClasses) throws RuntimeException {
        JavaType javaType = getObjectMapper().getTypeFactory().constructParametricType(collectionClazz, elementClasses);
        try {
            return getObjectMapper().readValue(str, javaType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode getJsonNode(String json) {
        try {
            return getObjectMapper().readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String compressJson(String content) {
        if (StringUtils.isBlank(content)) {
            return content;
        }
        Pattern p = Pattern.compile("\\s*|\t|\r|\n");
        Matcher m = p.matcher(content);
        return m.replaceAll("");
    }

    public static String readField(String json, String name) throws IOException {
        if (json != null) {
            ObjectNode object = getObjectMapper().readValue(json, ObjectNode.class);
            JsonNode node = object.get(name);
            return (node == null ? null : node.textValue());
        }
        return null;
    }

    public static boolean isJsonType(String str) {
        boolean result = false;
        str = str.trim();
        if (str.startsWith("{") && str.endsWith("}") || str.startsWith("[") && str.endsWith("]")) {
            result = true;
        }
        return result;
    }

}
