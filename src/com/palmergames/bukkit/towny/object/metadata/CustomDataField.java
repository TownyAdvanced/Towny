package com.palmergames.bukkit.towny.object.metadata;

import com.palmergames.bukkit.towny.exceptions.InvalidMetadataTypeException;

public abstract class CustomDataField<T> implements Cloneable {
    private T value;
    private final String key;
    
    protected String label;
    
    public CustomDataField(String key, T value, String label)
    {
        this.setValue(value);
        this.key = key;
        this.label = label;
    }

	public CustomDataField(String key, T value)
	{
		this(key, value, null);
	}

	public CustomDataField(String key, String label)
	{
		this(key, null, label);
	}

    public CustomDataField(String key)
    {
        this(key, null, null);
    }
    
    public abstract String getTypeID();

    public T getValue() {
        
        return value;
    }

    public void setValue(T value) {
        // TODO: Save to yml
        
        this.value = value;
    }
    
    public abstract void setValueFromString(String strValue);
    
    protected String serializeValueToString() {
    	return String.valueOf(getValue());
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

	// Not used for serialization anymore. Just for human readable format.
	@Override
    public String toString() {
        String out = "";
        
        // Type
        out += getTypeID();
        
        // Key
        out += "," + getKey();
        
        // Value
        out += "," + getValue();
        
        // Label
        out += "," + getLabel();
        
        return out;
    }

    // Overridable validation function
	protected boolean canParseFromString(String strValue) {
    	return true;
	}
	
	public final void isValidType(String str) throws InvalidMetadataTypeException {
    	if (!canParseFromString(str))
    		throw new InvalidMetadataTypeException(this);
	}

	/**
	 * Formats and colors the value of the custom data field object.
	 * @return the formatted value of this data field.
	 */
	public abstract String displayFormattedValue();
    
    @Override
    public boolean equals(Object rhs) {
        if (rhs instanceof CustomDataField)
            return ((CustomDataField<?>) rhs).getKey().equals(this.getKey());
        
        return false;
    }
    
    @Override
    public int hashCode() {
        // Use the key as a unique id
        return getKey().hashCode();
    }
    
    public abstract CustomDataField<T> clone();
    
	/**
	 * @deprecated as of 0.96.3.0, use {@link #clone()} instead.
	 * Returns a duplicate instance of the object.
	 * @return See {@link #clone()}.
	 */
	@Deprecated
    public CustomDataField<T> newCopy() {
    	return clone();
    }
    
}
