package com.palmergames.bukkit.towny.object.metadata;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.HashMap;
import java.util.Map;

public enum CustomDataFieldType {
    IntegerField(0, "Integer"), StringField(1, "String"), BooleanField(2, "Boolean"), DecimalField(3,"Decimal");
    
    private Integer value;
    private String typeName;
    
    CustomDataFieldType(Integer type, String typeName) {
        this.value = type;
        this.typeName = typeName;
    }

    public Integer getValue() {
        return value;
    }

    public String getTypeName() {
        return typeName;
    }
}
