package badda3mon.cheese.controller.additional;

public class Topics {
	public static final String BASE_CHANNEL = "cheese_test";
	public static final String AMOUNT_CHANNEL = BASE_CHANNEL + "/%d/amount";
	public static final String CHEESE_IN_DB_COUNT = BASE_CHANNEL + "/%d/vol";
	public static final String CHEESE_IN_COOKING_COUNT_SET = BASE_CHANNEL + "/set";
	public static final String CHEESE_IN_COOKING_COUNT_GET = BASE_CHANNEL + "/get";

	public static final String CHEESE_LAST_SET = BASE_CHANNEL + "/last/set";

	public static final String CHEESE_LAST_GET = BASE_CHANNEL + "/last/get";
}
