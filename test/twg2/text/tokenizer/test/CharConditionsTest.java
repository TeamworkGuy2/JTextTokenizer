package twg2.text.tokenizer.test;

import static twg2.text.tokenizer.test.ParserTestUtils.parseTest;

import org.junit.Test;

import twg2.collections.primitiveCollections.CharArrayList;
import twg2.parser.Inclusion;
import twg2.parser.condition.text.CharParser;
import twg2.ranges.CharSearchSet;
import twg2.text.tokenizer.CharConditions;

/**
 * @author TeamworkGuy2
 * @since 2015-2-12
 */
public class CharConditionsTest {


	@Test
	public void startCharCondition() {
		String name = "StartCharCondition";
		CharParser cond = new CharConditions.Start(name, CharArrayList.of('A', 'B', '='), Inclusion.INCLUDE);

		parseTest(true, false, name, cond, "A");
		parseTest(true, false, name, cond, "B");
		parseTest(true, false, name, cond, "=");
		parseTest(false, true, name, cond, "a");
		parseTest(false, true, name, cond, "AA");
		parseTest(false, true, name, cond, "+A");
	}


	@Test
	public void endCharCondition() {
		String name = "EndCondition";
		CharParser cond = new CharConditions.End(name, CharArrayList.of('\'', '"', '!'), Inclusion.INCLUDE);

		parseTest(true, false, name, cond, "abc'");
		parseTest(true, false, name, cond, "!");
		parseTest(true, false, name, cond, "stuff-n-things\"");
		parseTest(false, true, name, cond, "=!=");
	}


	@Test
	public void endNotPrecededByCharCondition() {
		String name = "EndNotPrecededByCondition";
		CharArrayList notPreced = CharArrayList.of('\\', '@');
		CharParser cond = new CharConditions.EndNotPrecededBy(name, CharArrayList.of('\'', '"', '!'), Inclusion.INCLUDE, notPreced);

		parseTest(true, false, name, cond, "stuff\"");
		parseTest(true, false, name, cond, "!");
		parseTest(true, false, name, cond, "invalid\\! valid!");
		parseTest(false, false, name, cond, "abcdefghijklmnopqrstuvwxyz@'");
		parseTest(false, true, name, cond, "=!=");
		parseTest(false, false, name, cond, "\\!");
	}


	@Test
	public void containsFirstSpecialCharCondition() {
		String name = "ContainsFirstSpecialCondition";
		CharParser cond = newIdentifierTokenizer();

		parseTest(true, false, name, cond, " - $_AzaZ90");
		parseTest(true, false, name, cond, "A");
		parseTest(true, false, name, cond, "A0a");
		parseTest(false, true, name, cond, "A0a ");
		parseTest(false, true, name, cond, "&amp;");
		parseTest(true, false, name, cond, "&*; withValidEndingRunOn");
	}


	/**
	 * @return condition for a string of contiguous characters matching those allowed in identifiers (i.e. 'mySpecialLoopCount', '$thing', or '_stspr')
	 */
	private static CharConditions.ContainsFirstSpecial newIdentifierTokenizer() {
		CharSearchSet firstCharSet = new CharSearchSet();
		firstCharSet.addChar('$');
		firstCharSet.addChar('_');
		firstCharSet.addRange('a', 'z');
		firstCharSet.addRange('A', 'Z');

		CharSearchSet charSet = firstCharSet.copy();
		charSet.addRange('0', '9');

		return new CharConditions.ContainsFirstSpecial("identifier", charSet::contains, firstCharSet.toCharList().toArray(), Inclusion.INCLUDE, charSet);
	}
}
