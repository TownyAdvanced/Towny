package com.palmergames.bukkit.towny.object.metadata;

// This class represents unparsed metadata. 
// Only exists if no plugin deserializers their metadata, 
class RawDataField extends CustomDataField<String> {
	private final String typeID;
	
	RawDataField(String typeID, String key, String value, String label) {
		super(key, value, label);
		this.typeID = typeID;
	}

	@Override
	public String getTypeID() {
		return typeID;
	}

	@Override
	protected boolean canParseFromString(String strValue) {
		return false;
	}

	@Override
	public void setValueFromString(String strValue) {
		setValue(strValue);
	}

	@Override
	public String displayFormattedValue() {
		return "UNLOADED - " + getValue();
	}

	// Never display a RawDataField on the status board of an object.
	@Override
	public boolean shouldDisplayInStatus() {
		return false;
	}

	@Override
	public RawDataField clone() {
		return new RawDataField(typeID, getKey(), getValue(), this.label);
	}
}
