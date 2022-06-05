package badda3mon.cheese.controller.additional;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Utils {
	public static String getCurrentDate(String format){
		Date date = Calendar.getInstance().getTime();

		SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.getDefault());
		return dateFormat.format(date);
	}
}
