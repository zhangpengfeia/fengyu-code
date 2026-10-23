package com.ruoyi.education.config;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.TimeZone;

@Configuration
public class EducationJson {
    @Bean Jackson2ObjectMapperBuilderCustomizer educationJsonCustomizer() {
        return builder->builder.serializerByType(Long.class,ToStringSerializer.instance)
            .timeZone(TimeZone.getTimeZone("UTC")).simpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    }
}
