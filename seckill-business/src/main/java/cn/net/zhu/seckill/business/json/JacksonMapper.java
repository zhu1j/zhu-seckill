package cn.net.zhu.seckill.business.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * jackson转换
 *
 * @author 一只朱
 * @date 2026-09-07 16:24
 *
 * "Run the code. Run the world."
 */

public class JacksonMapper extends ObjectMapper {

    public JacksonMapper() {
        super();
        this.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
        this.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        simpleModule.addSerializer(long.class, ToStringSerializer.instance);
        registerModule(simpleModule);
    }
}
