package com.palmergames.bukkit.towny.object.metadata;

import com.palmergames.bukkit.towny.exceptions.InvalidMetadataTypeException;

public abstract class CustomDataField<T> {
    private final CustomDataFieldType type;
    private T value;
    private final String key;
    
    private String label;
    
    public CustomDataField(String key, CustomDataFieldType type, T value, String label)
    {
        this.type = type;
        this.setValue(value);
        this.key = key;
        this.label = label;
    }

	public CustomDataField(String key, CustomDataFieldType type, T value)
	{
		this(key, type, value, null);
	}

	public CustomDataField(String key, CustomDataFieldType type, String label)
	{
		this(key, type, null, label);
	}

    public CustomDataField(String key, CustomDataFieldType type)
    {
        this(key, type, null, null);
    }

    public CustomDataFieldType getType() {
        return type;
    }

    public T getValue() {
        
        return value;
    }

    public void setValue(T value) {
        // TODO: Save to yml
        
        this.value = value;
    }

    public String getKey() {
        return key;
    }
    
    public String getLabel() {
    	if (hasLabel())
    		return label;
    	else
    		return "nil";
	}
	
	public boolean hasLabel() {
    	return label != null;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	@Override
    public String toString() {
        String out = "";
        
        // Type
        out += type.getValue().toString();
        
        // Key
        out += "," + getKey();
        
        // Value
        out += "," + getValue();
        
        // Label
        out += "," + getLabel();
        
        return out;
    }

	/**
	 * @param str - The metadata string to load
	 * @return - The data field defined by the string
	 */
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
                break;
            case BooleanField:
                field = new BooleanDataField(key, Boolean.parseBoolean(tokens[2]));
                break;
            case DecimalField:
                field = new DecimalDataField(key, Double.parseDouble(tokens[2]));
                break;
        }
        
		String label;
		if (tokens[3] == null || tokens[3].equalsIgnoreCase("nil"))
			label = null;
		else
			label = tokens[3];
        
		field.setLabel(label);
		
        return field;
    }
    
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
