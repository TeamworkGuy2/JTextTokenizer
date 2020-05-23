package twg2.text.tokenizer.test;

import org.junit.Assert;

import twg2.parser.condition.text.CharParser;
import twg2.parser.textParser.TextCharsParser;
import twg2.parser.textParser.TextParser;

/**
 * @author TeamworkGuy2
 * @since 2015-11-28
 */
public interface ParserTestUtils {

	/** Run a hasNext()/acceptNext() loop using the given parser 'cond'.
	 * The test is run with the condition recycled (if possible) and copied to ensure that results are the same.
	 */
	public static void parseTest(boolean expectComplete, boolean expectFailed, String name, CharParser cond, String src) {
		cond = cond.copyOrReuse();
		_parseTest(expectComplete, expectFailed, name, cond, src, src);
		cond = cond.copy();
		_parseTest(expectComplete, expectFailed, name, cond, src, src);
	}


	/** Run a hasNext()/acceptNext() loop using the given parser 'cond'.
	 * The test is run with the condition recycled (if possible) and copied to ensure that results are the same.
	 */
	public static void parseTest(boolean expectComplete, boolean expectFailed, String name, CharParser cond, String src, String expectedParsedResult) {
		cond = cond.copyOrReuse();
		_parseTest(expectComplete, expectFailed, name, cond, src, expectedParsedResult);
		cond = cond.copy();
		_parseTest(expectComplete, expectFailed, name, cond, src, expectedParsedResult);
	}


	public static void _parseTest(boolean expectComplete, boolean expectFailed, String name, CharParser cond, String src, String srcExpect) {
		TextParser buf = TextCharsParser.of(src);

		while(buf.hasNext()) {
			char ch = buf.nextChar();
			cond.acceptNext(ch, buf);
		}

		boolean isComplete = cond.isComplete();
		Assert.assertEquals(name + " '" + src + "' isComplete() ", expectComplete, isComplete);
		boolean isFailed = cond.isFailed();
		Assert.assertEquals(name + " '" + src + "' isFailed() ", expectFailed, isFailed);

		if(isComplete && srcExpect != null) {
			CharSequence parsedText = cond.getMatchedTextCoords().getText(src);
			Assert.assertEquals(srcExpect, parsedText);
		}
	}

}
