package com.palmergames.bukkit.towny.exceptions;

import com.palmergames.bukkit.towny.object.metadata.CustomDataFieldType;

public class InvalidMetadataTypeException extends TownyException {
    private static final long serialVersionUID = 2335936343233569066L;
    
    public InvalidMetadataTypeException(CustomDataFieldType type) {
        super("The given string for type " + type.getTypeName() + " is not valid!");
    }
}
