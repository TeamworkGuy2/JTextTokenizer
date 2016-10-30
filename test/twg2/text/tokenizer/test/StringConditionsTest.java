package twg2.text.tokenizer.test;

import org.junit.Assert;
import org.junit.Test;

import twg2.parser.Inclusion;
import twg2.parser.condition.text.CharParser;
import twg2.parser.textParser.TextIteratorParser;
import twg2.parser.textParser.TextParser;
import twg2.text.tokenizer.StringConditions;

/**
 * @author TeamworkGuy2
 * @since 2015-2-12
 */
public class StringConditionsTest {


	@Test
	public void testStartStringCondition() {
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

		Boolean[] expect = { true, true, true, false };

		CharParser cond = new StringConditions.Start("testStartStringCondition", startMarkers, Inclusion.INCLUDE);

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
			Assert.assertTrue(cond.acceptNext((char)0, pos) == false);
			cond = cond.copyOrReuse();
			i++;
		}
	}


	@Test
	public void testEndStringCondition() {
		String[] endMarkers = new String[] {
				"-->",
				"\"\"\"",
				"!#"
		};

		String[] testStrs = new String[] {
				"<!-- comment -->",
				"\"stuff\"\"\"",
				"!#", "!#="
		};

		Boolean[] expect = { true, true, true, false };

		CharParser cond = new StringConditions.End("testEndStringCondition", endMarkers, Inclusion.INCLUDE);

		int i = 0;
		for(String testStr : testStrs) {
			for(int ii = 0, size = endMarkers.length; ii < size; ii++) {
				TextParser pos = TextIteratorParser.of(testStr);
				while(pos.hasNext()) {
					char ch = pos.nextChar();
					cond.acceptNext(ch, pos);
				}
				Assert.assertTrue(i + "." + ii, cond.isComplete() == expect[i]);
				cond = cond.copyOrReuse();
			}
			i++;
		}
	}

}