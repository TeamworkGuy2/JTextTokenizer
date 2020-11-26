package twg2.text.tokenizer.analytics;

/**
 * @author TeamworkGuy2
 * @since 2016-10-29
 */
public interface TokenizationLogger {

	public void logCountCompoundCharParserMatch(int count);

	public void logCountCompoundCharParserAcceptNext(int count);

	public void logCountCreateParser(int count);

	public void logCountTextFragmentsConsumed(int count);
}
