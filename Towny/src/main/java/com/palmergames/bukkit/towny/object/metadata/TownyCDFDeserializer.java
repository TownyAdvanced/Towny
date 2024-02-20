package com.palmergames.bukkit.towny.object.metadata;

public class TownyCDFDeserializer {
	
	private static <T extends CustomDataField<?>> T deserializeDF(T cdf, String value) {
		if (value != null && cdf.canParseFromString(value))
			cdf.setValueFromString(value);
		
		return cdf;
	} 
	
	static final DataFieldDeserializer<IntegerDataField> INTEGER_DF = 
		(key, value) -> deserializeDF(new IntegerDataField(key), value);

	static final DataFieldDeserializer<BooleanDataField> BOOLEAN_DF =
		(key, value) -> deserializeDF(new BooleanDataField(key), value);

	static final DataFieldDeserializer<StringDataField> STRING_DF =
		(key, value) -> deserializeDF(new StringDataField(key), value);

	static final DataFieldDeserializer<DecimalDataField> DECIMAL_DF =
		(key, value) -> deserializeDF(new DecimalDataField(key), value);

	static final DataFieldDeserializer<LongDataField> LONG_DF =
		(key, value) -> deserializeDF(new LongDataField(key), value);
	
	static final DataFieldDeserializer<ByteDataField> BYTE_DF =
		(key, value) -> deserializeDF(new ByteDataField(key), value);

	static final DataFieldDeserializer<LocationDataField> LOCATION_DF =
			(key, value) -> deserializeDF(new LocationDataField(key), value);
	
	static final DataFieldDeserializer<ListDataField> LIST_DF =
		(key, value) -> deserializeDF(new ListDataField(key), value);

}
