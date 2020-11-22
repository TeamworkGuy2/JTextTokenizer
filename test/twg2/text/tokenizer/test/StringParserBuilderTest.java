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
	public void startEndNotPrecededByMarkers() throws IOException {
		// single-character start and end markers and single-character escape markers
		String[] strs =   { "\"a \\\" b \\\"", "\"\" !", "alpha", "\"a \n\\\"\\\" z\" echo" };
		String[] expect = { "\"a \" b \"",     "\"\"",   "",      "\"a \n\"\" z\"" };
		boolean[] matched = { true,            true,     false,   true };
		boolean[] completed = { false,         true,     false,   true };

		var spb = new StringParserBuilder("stringParserBuilderTest1");
		spb.addStartEndNotPrecededByMarkers("string literal", '"', '\\', '"', Inclusion.INCLUDE);
		CharParserFactory parser = spb.build();

		Function<String, String> escSeqDecoder = EscapeSequences.unicodeEscapeDecoder();
		CheckTask.assertTests(strs, expect, (String s, Integer i) -> {
			var dst = new StringBuilder();
			var textSrc = TextIteratorParser.of(s);
			Assert.assertEquals("i=" + i, matched[i], parser.isMatch(s.charAt(0), textSrc));

			CharParser cond = parser.createParser();
			Assert.assertEquals("i=" + i, completed[i], cond.readConditional(textSrc, dst));
			return escSeqDecoder.apply(dst.toString());
		});

		StringBuilder dst = new StringBuilder();
		CharParser cond2 = parser.createParser();
		Assert.assertFalse(cond2.readConditional(TextIteratorParser.of("-\"st\"-"), dst));
		Assert.assertEquals("", dst.toString());
	}


	@Test
	public void startEndNotPrecededByMarkers2() throws IOException {
		// single-character start and end markers and single-character escape markers
		String[] strs =   { "\"a \\\" b \\\"", "\"\" !", "alpha", "\"a \n\\\"\\\" z\" echo" };
		String[] expect = { "\"a \\\" b \\\"", "\"\"",   "a",     "\"a \n\\\"\\\" z\"" };

		var spb = new StringParserBuilder("stringParserBuilderTest2");
		spb.addCharLiteralMarker("a", 'a');
		spb.addStartEndNotPrecededByMarkers("string literal", '"', '\\', '"', Inclusion.INCLUDE);
		CharParserFactory parser1 = spb.build();

		CheckTask.assertTests(strs, expect, (String s, Integer i) -> {
			var dst = new StringBuilder();
			var textSrc = TextIteratorParser.of(s);
			Assert.assertTrue(parser1.isMatch(s.charAt(0), textSrc));

			CharParser cond = parser1.createParser();
			cond.readConditional(textSrc, dst);
			return dst.toString();
		});
	}


	@Test
	public void addStartEndMarkers() throws IOException {
		// single-character start and end markers and single-character escape markers
		String[] strs =   { "[abcdef] -", "@[] + 1", "<tag /> <z/>", "{{start}} -", "@[@[a]b]" };
		String[] expect = { "[abcdef]",   "@[]",     "<tag />",      "{{start}}", "@[@[a]" };

		var spb = new StringParserBuilder("startEndMarkers");
		spb.addStartEndMarkers("[]", '[', ']', Inclusion.INCLUDE);
		spb.addStartEndMarkers("@[]", "@[", ']', Inclusion.INCLUDE);
		spb.addStartEndMarkers("</>", '<', "/>", Inclusion.INCLUDE);
		spb.addStartEndMarkers("{{}}", "{{", "}}", Inclusion.INCLUDE);
		CharParserFactory parser1 = spb.build();

		CheckTask.assertTests(strs, expect, (String s, Integer i) -> {
			var dst = new StringBuilder();
			var textSrc = TextIteratorParser.of(s);
			Assert.assertTrue(parser1.isMatch(s.charAt(0), textSrc));

			CharParser cond = parser1.createParser();
			cond.readConditional(textSrc, dst);
			return dst.toString();
		});
	}


	@Test
	public void charLiteralMarkers() throws IOException {
		String[] strs =   { "@start -", "start; end;", ";", "@@" };
		String[] expect = { "@", "start", ";", "@" };

		var spb = new StringParserBuilder("charLiteralMarkers");
		spb.addCharLiteralMarker("@;", '@', ';');
		spb.addCharMatcher("start", new char[] { 's', 't', 'a', 'r', 't' });
		CharParserFactory parser1 = spb.build();

		CheckTask.assertTests(strs, expect, (String s, Integer i) -> {
			var dst = new StringBuilder();
			var textSrc = TextIteratorParser.of(s);
			Assert.assertTrue(parser1.isMatch(s.charAt(0), textSrc));

			CharParser cond = parser1.createParser();
			cond.readConditional(textSrc, dst);
			return dst.toString();
		});
	}


	@Test
	public void stringLiteralMarkers() throws IOException {
		String[] strs =   { "@start -", "start; end;", "end -", "@@" };
		String[] expect = { "@", "start", "end", "@" };

		var spb = new StringParserBuilder("charLiteralMarkers");
		spb.addStringLiteralMarker("@", "@");
		spb.addStringLiteralMarker("start", "start", "end");
		CharParserFactory parser1 = spb.build();

		CheckTask.assertTests(strs, expect, (String s, Integer i) -> {
			var dst = new StringBuilder();
			var textSrc = TextIteratorParser.of(s);
			Assert.assertTrue(parser1.isMatch(s.charAt(0), textSrc));

			CharParser cond = parser1.createParser();
			cond.readConditional(textSrc, dst);
			return dst.toString();
		});
	}

}
