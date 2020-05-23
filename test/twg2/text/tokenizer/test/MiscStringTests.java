package twg2.text.tokenizer.test;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import twg2.arrays.ArrayUtil;
import twg2.junitassist.checks.CheckTask;
import twg2.junitassist.checks.TestData;
import twg2.parser.textParser.TextIteratorParser;
import twg2.parser.textParser.TextParser;
import twg2.text.stringSearch.StringCompare;
import twg2.text.stringSearch.StringIndex;

/**
 * @author TeamworkGuy2
 * @since 2016-0-0
 */
public class MiscStringTests {

	@Test
	public void startsWithTest() {
		List<TestData<String, String>> startsWithStrs = Arrays.asList(
				TestData.matchFalse("this a lz3", "thisa"),
				TestData.matchTrue("this a lz3", "this"),
				TestData.matchFalse("a", "ab"),
				TestData.matchTrue("a", "a"),
				TestData.matchFalse("", "a"),
				TestData.matchTrue("", "")
		);

		for(TestData<String, String> test : startsWithStrs) {
			Assert.assertTrue(test.isShouldInputEqualExpected() ==
					StringCompare.startsWith(test.getInput().toCharArray(), 0, test.getExpected().toCharArray(), 0));
		}
	}


	@Test
	public void indexOfTest() {
		String searchString = "this 32a this 32.1f is_a string";
		String[] strs = new String[] {"32a", "32z", " ", "  ", "this", "string"};
		Integer[] expect = { 5, -1, 4, -1, 0, 25 };
		char[] searchChars = searchString.toCharArray();

		CheckTask.assertTests(strs, expect, (str) -> StringIndex.indexOf(searchChars, 0, str, 0));
	}


	@Test
	public void lineBufferTest() {
		String line = "Abbb abab [very awesome] 132\n" +
				"few 142345 52132";

		TextParser tool = TextIteratorParser.of(line);
		StringBuilder dst = new StringBuilder();

		tool.nextIf('A', dst);
		tool.nextIf((c) -> (c == 'b'), 2, dst);
		check(tool.nextIf('b', dst), 1, "could not read 'b'");
		check(tool.nextIf(' ', dst), 1, "could not read ' '");
		check(tool.nextIf('a', 'b', 0, dst), 4, "could not read 'a' or 'b'");
		tool.nextIf((c) -> true, 0, dst);

		tool.nextIf((c) -> true, 3, dst);
		tool.nextIf((c) -> (ArrayUtil.indexOf(new char[] {'1', '2', '3', '4', '5', ' '}, c) > -1), 0, dst);
		tool.nextIf((c) -> (ArrayUtil.indexOf(new char[] {'1', '2', '3', '4', '5', ' '}, c) > -1), 0, dst);

		Assert.assertEquals("parsed: '" + dst.toString() + "', does not match: '" + line + "'", line, dst.toString());
	}


	private static void check(int input, int expected, String message) {
		Assert.assertEquals(message + " (input: " + input + ", expected: " + expected + ")", expected, input);
	}

}
