package ca.xshade.util;

import java.util.Comparator;
import java.util.Hashtable;

public class Sorting {
	
	@SuppressWarnings("rawtypes")
	public static void main(String[] args) {
		Hashtable<Object,Object> table = new Hashtable<Object,Object>();
		table.put(1, 4);
		table.put(0, 3);
		table.put(3, 0);
		table.put(2, 2);
		table.put(4, 1);
		
		KeyValueTable kvTable = new KeyValueTable(table);
		print(kvTable);
		kvTable.sortByKey();print(kvTable);
		kvTable.sortByValue();print(kvTable);
	}
	
	public static void print(KeyValueTable<?,?> table) {
		for (KeyValue<?, ?> index : table.getKeyValues())
			System.out.print("[" + index.key + " : " + index.value + "]\n");
		System.out.print("\n");
	}
	
	static class ValueSort implements Comparator<Object> {
		@Override
		public int compare(Object o1, Object o2) {
			if (!(o1 instanceof KeyValue && o2 instanceof KeyValue))
				return o1.hashCode() - o2.hashCode();
			
			KeyValue<?, ?> k1 = (KeyValue<?, ?>)o1;
			KeyValue<?, ?> k2 = (KeyValue<?, ?>)o2;
			return k1.value.hashCode() - k2.value.hashCode();
	    }
	}
	
	static class KeySort implements Comparator<Object> {
		@Override
		public int compare(Object o1, Object o2) {
			if (!(o1 instanceof KeyValue && o2 instanceof KeyValue))
				return o1.hashCode() - o2.hashCode();
			
			KeyValue<?, ?> k1 = (KeyValue<?, ?>)o1;
			KeyValue<?, ?> k2 = (KeyValue<?, ?>)o2;
			return k1.key.hashCode() - k2.key.hashCode();
	    }
	}
}