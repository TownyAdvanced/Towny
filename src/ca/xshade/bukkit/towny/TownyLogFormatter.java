package ca.xshade.bukkit.towny;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class TownyLogFormatter extends SimpleFormatter {
	private DateFormat dateFormat;
	static final String lineSep = System.getProperty("line.separator");
	
	@Override
	public synchronized String format(LogRecord record) {
		StringBuffer buf = new StringBuffer(180);
		if (dateFormat == null)
			dateFormat = DateFormat.getDateTimeInstance();
		
		buf.append('[');
		buf.append(dateFormat.format(new Date(record.getMillis())));
		buf.append("] ");
		buf.append(formatMessage(record));
		buf.append(lineSep);
		
		Throwable throwable = record.getThrown();
		if (throwable != null) {
			StringWriter sink = new StringWriter();
			throwable.printStackTrace(new PrintWriter(sink, true));
			buf.append(sink.toString());
		}
		return buf.toString();
	}
}
