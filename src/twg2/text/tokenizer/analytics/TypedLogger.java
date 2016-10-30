package twg2.text.tokenizer.analytics;

import java.io.IOException;

/**
 * @author TeamworkGuy2
 * @since 2016-10-29
 */
public interface TypedLogger<E, S> {

	public void logMsg(E action, String msg);

	public void logCount(E action, long count);

	public void logDuration(E action, double durationMilliseconds);

	public void toJson(String srcName, boolean includeSurroundingBrackets, Appendable dst, S st) throws IOException;

	public String toString(String srcName, boolean includeClassName);

}
