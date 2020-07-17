package com.palmergames.bukkit.towny.object.metadata;

public enum CustomDataFieldType {
    IntegerField(0, "Integer"), StringField(1, "String"), BooleanField(2, "Boolean"), DecimalField(3,"Decimal");
    
    private final Integer value;
    private final String typeName;
    
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
