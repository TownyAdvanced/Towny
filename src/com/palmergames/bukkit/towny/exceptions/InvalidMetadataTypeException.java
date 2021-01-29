package com.palmergames.bukkit.towny.exceptions;

import com.palmergames.bukkit.towny.object.metadata.CustomDataField;

public class InvalidMetadataTypeException extends TownyException {
    private static final long serialVersionUID = 2335936343233569066L;
    
    public InvalidMetadataTypeException(CustomDataField<?> cdf) {
        super("The given string for type " + cdf.getClass().getName() + " is not valid!");
    }
}
