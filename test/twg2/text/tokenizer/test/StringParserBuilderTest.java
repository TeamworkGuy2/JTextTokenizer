package twg2.text.tokenizer.test;

import java.io.IOException;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import twg2.junitassist.checks.CheckTask;
import twg2.parser.condition.text.CharParser;
import twg2.parser.textParser.TextIteratorParser;
import twg2.parser.textParserUtils.EscapeSequences;
import twg2.text.tokenizer.CharParserFactory;
import twg2.text.tokenizer.Inclusion;
import twg2.text.tokenizer.StringParserBuilder;

/**
 * @author TeamworkGuy2
 * @since 2020-05-16
 */
public class StringParserBuilderTest {

	@Test
	public void stringParserBuilderTest1() throws IOException {
		// single-character start and end markers and single-character escape markers
		String[] strs =   { "\"a \\\" b \\\"", "\"\" !", "alpha", "\"a \n\\\"\\\" z\" echo" };
		String[] expect = { "\"a \" b \"",     "\"\"",   "",      "\"a \n\"\" z\"" };
		boolean[] successes = { false,         true,     false,   true };

		var spb = new StringParserBuilder("stringParserBuilderTest1");
		spb.addStartEndNotPrecededByMarkers("string literal", '"', '\\', '"', Inclusion.INCLUDE);
		CharParserFactory parser = spb.build();

		Function<String, String> escSeqDecoder = EscapeSequences.unicodeEscapeDecoder();
		CheckTask.assertTests(strs, expect, (String s, Integer i) -> {
			StringBuilder dst = new StringBuilder();
			CharParser cond = parser.createParser();
			Assert.assertEquals("i=" + i, successes[i], cond.readConditional(TextIteratorParser.of(s), dst));
			return escSeqDecoder.apply(dst.toString());
		});

		StringBuilder dst = new StringBuilder();
		CharParser cond2 = parser.createParser();
		Assert.assertFalse(cond2.readConditional(TextIteratorParser.of("-\"st\"-"), dst));
		Assert.assertEquals("", dst.toString());
	}


	@Test
	public void stringParserBuilderTest2() throws IOException {
		// single-character start and end markers and single-character escape markers
		String[] strs =   { "\"a \\\" b \\\"", "\"\" !", "alpha", "\"a \n\\\"\\\" z\" echo" };
		String[] expect = { "\"a \\\" b \\\"", "\"\"",   "a",     "\"a \n\\\"\\\" z\"" };

		var spb = new StringParserBuilder("stringParserBuilderTest2");
		spb.addCharLiteralMarker("a", 'a');
		spb.addStartEndNotPrecededByMarkers("string literal", '"', '\\', '"', Inclusion.INCLUDE);
		CharParserFactory parser1 = spb.build();

		CheckTask.assertTests(strs, expect, (String s, Integer i) -> {
			StringBuilder dst = new StringBuilder();
			CharParser cond = parser1.createParser();
			cond.readConditional(TextIteratorParser.of(s), dst);
			return dst.toString();
		});
	}

}