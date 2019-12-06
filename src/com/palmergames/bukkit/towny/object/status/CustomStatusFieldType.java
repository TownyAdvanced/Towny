package com.palmergames.bukkit.towny.object.status;

public enum CustomStatusFieldType {
	IntegerField(0, "Integer"), StringField(1, "String"), 
	BalanceField(2, "Balance"), DecimalField(3, "Decimal"), 
	BooleanField(4, "Boolean");
	
	private Integer value;
	private String typeName;

	CustomStatusFieldType(Integer type, String typeName) {
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
