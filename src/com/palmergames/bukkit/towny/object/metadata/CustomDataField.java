package com.palmergames.bukkit.towny.object.metadata;

import com.palmergames.bukkit.towny.exceptions.InvalidMetadataTypeException;

/**
 * @author Suneet Tipirneni (Siris)
 * @param <T> The type of data the data field will hold.
 */
public abstract class CustomDataField<T> {
    private CustomDataFieldType type;
    private T value;
    private String key;

	/**
	 * 
	 * @param key The keyname of the data field (must be unique).
	 * @param type What type the data field show be see: {@link com.palmergames.bukkit.towny.object.metadata.CustomDataFieldType}.
	 * @param value The default value for this field.
	 */
	public CustomDataField(String key, CustomDataFieldType type, T value)
    {
        this.type = type;
        this.setValue(value);
        this.key = key;
    }

	/**
	 *
	 * @param key The keyname of the data field (must be unique).
	 * @param type What type the data field show be see: {@link com.palmergames.bukkit.towny.object.metadata.CustomDataFieldType}.
	 */
    public CustomDataField(String key, CustomDataFieldType type)
    {
        this.type = type;
        this.value = null;
        this.key = key;
    }

    public CustomDataFieldType getType() {
        return type;
    }

    public T getValue() {
        
        return value;
    }

    public void setValue(T value) {
        
        this.value = value;
    }

    public String getKey() {
        return key;
    }

	/**
	 * 
	 * @return A string representation that is saved into the towny database.
	 */
    @Override
    public String toString() {
        String out = "";
        
        // Type
        out += type.getValue().toString();
        
        // Key
        out += "," + getKey();
        
        // Value
        out += "," + getValue();
        
        return out;
    }
    
    public static CustomDataField load(String str) {
        String[] tokens = str.split(",");
        CustomDataFieldType type = CustomDataFieldType.values()[Integer.parseInt(tokens[0])];
        String key = tokens[1];
        CustomDataField field = null;
        
        switch (type) {
            case IntegerField:
                Integer intValue = Integer.parseInt(tokens[2]);
                field = new IntegerDataField(key, intValue);
                break;
            case StringField:
                field = new StringDataField(key, tokens[2]);
            case BooleanField:
                field = new BooleanDataField(key, Boolean.parseBoolean(tokens[2]));
                break;
            case DecimalField:
                field = new DecimalDataField(key, Double.parseDouble(tokens[2]));
        }
        
        return field;
    }

	/**
	 * Type checks a custom data field via its string representation.
	 * @param str - The string representation of the object to be tested.
	 * @throws InvalidMetadataTypeException thrown when the string representation does not match the expected type.
	 */
	public void isValidType(String str) throws InvalidMetadataTypeException {
        switch (type) {
            case IntegerField:
                try {
                    Integer.parseInt(str);
                } catch (NumberFormatException e) {
                    throw new InvalidMetadataTypeException(type);
                }
                break;
            case BooleanField:
                // Apparently any string that isn't "true" is just evaluated to false.
                break;
            case DecimalField:
                try {
                    Double.parseDouble(str);
                } catch (NumberFormatException e) {
                    throw new InvalidMetadataTypeException(type);
                }
                break;
            default:
            	break;
        }
    }

	/**
	 * 
	 * @param rhs
	 * @return returns true if the id is matching false otherwise.
	 */
	@Override
    public boolean equals(Object rhs) {
        if (rhs instanceof CustomDataField)
            return ((CustomDataField) rhs).getKey().equals(this.getKey());
        
        return false;
    }
    
    @Override
    public int hashCode() {
        // Use the key as a unique id
        return getKey().hashCode();
    }

	/**
	 * 
	 * @return A new copy or a different instance with equivalent field values.
	 */
	public CustomDataField newCopy() {
        switch (type) {
            case BooleanField:
                return new BooleanDataField(getKey(), (Boolean)getValue());
            case IntegerField:
                return new IntegerDataField(getKey(), (Integer)getValue());
            case DecimalField:
                return new DecimalDataField(getKey(), (Double)getValue());
            case StringField:
                return new StringDataField(getKey(), (String)getValue());
        }
        
        return null;
    }
    
}
