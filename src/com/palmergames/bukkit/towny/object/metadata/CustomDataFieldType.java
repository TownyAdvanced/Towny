package com.palmergames.bukkit.towny.object.metadata;

public enum CustomDataFieldType {
    
	IntegerField(0, "Integer"),
	StringField(1, "String"),
	BooleanField(2, "Boolean"),
	DecimalField(3,"Decimal"),
	LongField(4, "Long");
    
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
    
    // Order of the declaration should not be relied to determine type, when a type index exists.
    public static CustomDataFieldType fromValue(int value) {
    	CustomDataFieldType type = null;
    	
    	switch (value) {
			case 0:
				type = IntegerField;
				break;
			case 1:
				type = StringField;
				break;
			case 2:
				type = BooleanField;
				break;
			case 3:
				type = DecimalField;
				break;
			case 4:
				type = LongField;
				break;
			default:
		}
		
		return type;
	}
}
