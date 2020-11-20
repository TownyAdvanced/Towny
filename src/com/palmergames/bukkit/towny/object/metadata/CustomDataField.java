package com.palmergames.bukkit.towny.object.metadata;

import com.palmergames.bukkit.towny.exceptions.InvalidMetadataTypeException;

public abstract class CustomDataField<T> implements Cloneable {
    private final CustomDataFieldType type;
    private T value;
    private final String key;
    
    protected String label;
    
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
    
    public abstract void setValueFromString(String strValue);

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
        CustomDataFieldType type = CustomDataFieldType.fromValue(Integer.parseInt(tokens[0]));
        String key = tokens[1];
        CustomDataField field = null;
        
        switch (type) {
            case IntegerField:
                field = new IntegerDataField(key);
                break;
            case StringField:
                field = new StringDataField(key);
                break;
            case BooleanField:
                field = new BooleanDataField(key);
                break;
            case DecimalField:
                field = new DecimalDataField(key);
                break;
			case LongField:
				field = new LongDataField(key);
				break;
        }
        
        if (field.canParseFromString(tokens[2]))
        	field.setValueFromString(tokens[2]);
        
		String label;
		if (tokens[3] == null || tokens[3].equalsIgnoreCase("nil"))
			label = null;
		else
			label = tokens[3];
        
		field.setLabel(label);
		
        return field;
    }

    // Overridable validation function
	protected boolean canParseFromString(String strValue) {
    	return true;
	}
	
	public final void isValidType(String str) throws InvalidMetadataTypeException {
    	if (!canParseFromString(str))
    		throw new InvalidMetadataTypeException(this.type);
	}

	/**
	 * Formats and colors the value of the custom data field object.
	 * @return the formatted value of this data field.
	 */
	public abstract String displayFormattedValue();
    
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
    
    public abstract CustomDataField clone();
    
	/**
	 * Returns a duplicate instance of the object.
	 * @deprecated Use {@link #clone()} instead.
	 * @return See {@link #clone()}.
	 */
	@Deprecated
    public CustomDataField newCopy() {
    	return clone();
    }
    
}
