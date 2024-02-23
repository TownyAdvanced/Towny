package com.palmergames.bukkit.towny.object.metadata;

import com.palmergames.bukkit.towny.exceptions.InvalidMetadataTypeException;
import com.palmergames.bukkit.towny.utils.TownyComponents;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CustomDataField<T> implements Cloneable {
    T value;
    final String key;
    
    protected String label;
    
    public CustomDataField(String key, T value, String label) {
        this.setValue(value);
        this.key = key;
        this.label = label;
    }

	public CustomDataField(String key, T value) {
		this(key, value, null);
	}

	public CustomDataField(String key, String label) {
		this(key, null, label);
	}

    public CustomDataField(String key) {
        this(key, null, null);
    }

	/**
	 * Gets the type id for the given CustomDataField class. 
	 * This value is attached to the class, and not a specific instance. 
	 * Used for serialization purposes. 
	 * 
	 * @return type id of the given CustomDataField class.
	 */
	@NotNull
	public abstract String getTypeID();

    public T getValue() {
        
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

	/**
	 * Sets the value based on the given input.
	 * Used when admins want to edit metadata in-game.
	 * 
	 * @param strValue input.
	 */
	public abstract void setValueFromString(String strValue);

	/**
	 * Serializes the current value to a string. 
	 * Used for saving the CustomDataField object.
	 * 
	 * @return serialized string
	 */
	@Nullable
	protected String serializeValueToString() {
    	return String.valueOf(getValue());
	}

	@NotNull
    public String getKey() {
        return key;
    }
    
    public boolean shouldDisplayInStatus() {
    	return hasLabel();
	}
    
	@NotNull
    public String getLabel() {
    	if (hasLabel())
    		return label;
    	else
    		return "nil";
	}

	/**
	 * Get label as a formatted component.
	 * <p> 
	 * This function is intentionally overridable by child classes.
	 * 
	 * @return formatted label component.
	 */
	@NotNull
	public Component getLabelAsComp() {
		return TownyComponents.miniMessage(getLabel());
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
		return getClass().getSimpleName() + "{" +
			"type=" + getTypeID() +
			", key='" + key + '\'' +
			", value=" + value + '\'' +
			", label='" + label + '\'' +
			'}';
	}

	/**
	 * Determines whether the given input can be parsed to the appropriate value.
	 * Used to parse admin input for in-game metadata editing.
	 * 
	 * @param strValue admin input
	 * @return whether the string can be parsed or not
	 */
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
	protected abstract String displayFormattedValue();

	/**
	 * Get the value as a formatted component.
	 * <p>
	 * This function is intentionally overridable by child classes.
	 * 
	 * @return formatted component of value.
	 */
	public Component formatValueAsComp() {
		return TownyComponents.miniMessage(displayFormattedValue());
	}
    
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
    
    public abstract @NotNull CustomDataField<T> clone();
}
