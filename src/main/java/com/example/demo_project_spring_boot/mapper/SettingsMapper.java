package com.example.demo_project_spring_boot.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SettingsMapper {

    private final ObjectMapper objectMapper;

    public Map<String, Object> toMap(Object dto) {
        return objectMapper.convertValue(dto, new TypeReference<>() {
        });
    }

    public <T> T toDto(Map<String, Object> values, Class<T> dtoClass) {
        return objectMapper.convertValue(values, dtoClass);
    }
}

