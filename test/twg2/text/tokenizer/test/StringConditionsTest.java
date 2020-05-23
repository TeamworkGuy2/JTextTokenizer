package twg2.text.tokenizer.test;

import static twg2.text.tokenizer.test.ParserTestUtils.parseTest;

import org.junit.Assert;
import org.junit.Test;

import twg2.parser.condition.text.CharParser;
import twg2.parser.textParser.TextIteratorParser;
import twg2.parser.textParser.TextParser;
import twg2.text.tokenizer.Inclusion;
import twg2.text.tokenizer.StringConditions;

/**
 * @author TeamworkGuy2
 * @since 2015-2-12
 */
public class StringConditionsTest {


	@Test
	public void literalStringCondition1() {
		String[] startMarkers = new String[] {
				"//",
				"\"",
				"/**",
				"!#"
		};

		String[] testStrs = new String[] {
				"// comment",
				"\"",
				"/** ",
				" !#"
		};

		boolean[] expect = { true, true, true, false };

		CharParser cond = new StringConditions.Literal("LiteralString", startMarkers, Inclusion.INCLUDE);

		int i = 0;
		for(String testStr : testStrs) {
			TextParser pos = TextIteratorParser.of(testStr);
			int chI = 0;
			for(char ch : testStr.toCharArray()) {
				pos.nextChar();
				if(chI >= startMarkers[i].length()) {
					break;
				}
				cond.acceptNext(ch, pos);
				chI++;
			}
			Assert.assertTrue(i + "." + chI, cond.isComplete() == expect[i]);
			Assert.assertFalse(cond.acceptNext((char)0, pos));
			cond = cond.copyOrReuse();
			i++;
		}
	}


	@Test
	public void literalStringCondition2() {
		String name = "LiteralString";
		CharParser cond = new StringConditions.Literal(name, ary("||", "::", "-10-"), Inclusion.INCLUDE);

		parseTest(false, false, name, cond, "");
		parseTest(false, true, name, cond, ":-");
		parseTest(false, true, name, cond, " ::");
		parseTest(true, false, name, cond, "-10-");
		parseTest(true, false, name, cond, "||");
	}


	@Test
	public void endStringCondition() {
		String[] endMarkers = new String[] {
				"-->",
				"\"\"",
				"!#"
		};

		String[] testStrs = new String[] {
				"<!-- comment --->",
				"\"stuff\"\"\"",
				"\"\"",
				"!#",
				"!!#",
				"!#="
		};

		boolean[] expect = { true, true, true, true, true, false };

		CharParser cond = new StringConditions.End("EndString", endMarkers, Inclusion.INCLUDE);

		int i = 0;
		for(String testStr : testStrs) {
			TextParser pos = TextIteratorParser.of(testStr);
			while(pos.hasNext()) {
				char ch = pos.nextChar();
				cond.acceptNext(ch, pos);
			}
			Assert.assertTrue("'" + testStr + "' complete: " + cond.isComplete() + ", expected: " + expect[i], cond.isComplete() == expect[i]);
			cond = cond.copyOrReuse();
			i++;
		}
	}


	@SafeVarargs
	private static <T> T[] ary(T... ts) {
		return ts;
	}

}
