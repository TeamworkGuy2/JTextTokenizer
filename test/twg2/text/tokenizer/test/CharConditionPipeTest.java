package twg2.text.tokenizer.test;

import static twg2.text.tokenizer.test.ParserTestUtils.parseTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import twg2.collections.primitiveCollections.CharArrayList;
import twg2.functions.predicates.CharPredicate;
import twg2.parser.condition.text.CharParserMatchable;
import twg2.parser.condition.text.CharParserPredicate;
import twg2.parser.textParser.TextParser;
import twg2.ranges.CharSearchSet;
import twg2.text.tokenizer.CharConditionPipe;
import twg2.text.tokenizer.CharConditions;
import twg2.text.tokenizer.Inclusion;
import twg2.text.tokenizer.StringConditions;

/**
 * @author TeamworkGuy2
 * @since 2015-11-29
 */
public class CharConditionPipeTest {

	@Test
	public void allRequiredTest() throws IOException {
		String name = "AllRequired";
		var condSet = list(
			new CharConditions.Literal("<", CharArrayList.of('<'), Inclusion.INCLUDE),
			new CharConditions.End(">", CharArrayList.of('>'), Inclusion.INCLUDE),
			new StringConditions.Literal("test", new String[] { "test" }, Inclusion.INCLUDE),
			new StringConditions.End("!", new String[] { "!" }, Inclusion.INCLUDE)
		);

		var pipeCond = CharConditionPipe.createPipeAllRequired(name, condSet);

		parseTest(false, false, name, pipeCond, "<abc>");
		parseTest(false, false, name, pipeCond, "<abc>test");
		parseTest(false, true, name, pipeCond, "<abc>te;");
		parseTest(false, false, name, pipeCond, "<abc>test;");

		parseTest(true, false, name, pipeCond, "<abc>test!");
		parseTest(true, false, name, pipeCond, "<abc>test stuff!");
	}


	@Test
	public void optionalSuffixTest() throws IOException {
		String name = "OptionalSuffix";
		var condSet0 = list(
			new CharConditions.Literal("<", CharArrayList.of('<'), Inclusion.INCLUDE),
			new CharConditions.End(">", CharArrayList.of('>'), Inclusion.INCLUDE)
		);

		var condSetOptional = list(
			new StringConditions.Literal("test", new String[] { "test" }, Inclusion.INCLUDE),
			new StringConditions.Literal("'[]'", new String[] { "[]" }, Inclusion.INCLUDE)
		);

		var endCondSetOptional = list(
			new StringConditions.Literal("...", new String[] { "..." }, Inclusion.INCLUDE)
		);

		var innerPipeCond = CharConditionPipe.createPipeOptionalSuffix(name, condSet0, condSetOptional);

		var pipeCond = CharConditionPipe.createPipeOptionalSuffix(name, list(innerPipeCond), endCondSetOptional);

		parseTest(false, true, name, pipeCond, "==");
		parseTest(false, false, name, pipeCond, "<abc");
		parseTest(false, false, name, pipeCond, "<abc>te");

		parseTest(true, false, name, pipeCond, "<abc>");
		parseTest(true, false, name, pipeCond, "<abc>te;", "<abc>");
		parseTest(true, false, name, pipeCond, "<abc>test");
		parseTest(true, false, name, pipeCond, "<abc>[]");
		parseTest(true, false, name, pipeCond, "<abc>test[]");
	}


	@Test
	public void repeatableSparatorTest() {
		String name = "RepeatableSeparator";
		var condSet0 = list(
			new CharConditions.Literal("<", CharArrayList.of('<'), Inclusion.INCLUDE),
			new CharConditions.End(">", CharArrayList.of('>'), Inclusion.INCLUDE)
		);

		var condSetOptional = list(
			new StringConditions.Literal("separator", new String[] { ", " }, Inclusion.INCLUDE)
		);

		var pipeCond = CharConditionPipe.createPipeRepeatableSeparator(name, condSet0, condSetOptional);

		parseTest(false, true, name, pipeCond, ", ");
		parseTest(false, false, name, pipeCond, "<<, ");
		parseTest(false, false, name, pipeCond, "<abc>,");
		parseTest(false, false, name, pipeCond, "<abc>, ");

		parseTest(true, false, name, pipeCond, "<abc>");
		parseTest(true, false, name, pipeCond, "<abc>;", "<abc>");
		parseTest(true, false, name, pipeCond, "<abc>,;", "<abc>");
		parseTest(true, false, name, pipeCond, "<abc>, <test>");
		parseTest(true, false, name, pipeCond, "<abc>, <test>, <1>");
		parseTest(true, false, name, pipeCond, "<abc>, <test>, <1>, <this works!>");
	}


	@Test
	public void repeatableSingleSeparatorLiteralTest() {
		String name = "RepeatableSeparator";
		var condSet0 = list(
			new StringConditions.Literal("<", new String[] { "[]" }, Inclusion.INCLUDE)
		);

		var pipeCond = CharConditionPipe.createPipeRepeatableSeparator(name, condSet0, null);

		parseTest(false, true, name, pipeCond, " ");
		parseTest(false, true, name, pipeCond, "[[ ");
		parseTest(false, true, name, pipeCond, "[] ");
		parseTest(false, true, name, pipeCond, "[[]]");
		parseTest(false, false, name, pipeCond, "[");

		parseTest(true, false, name, pipeCond, "[]");
		parseTest(true, false, name, pipeCond, "[][]");
		parseTest(true, false, name, pipeCond, "[][][]");
	}


	@Test
	public void repeatableSingleSeparatorCompoundTest() {
		String name = "RepeatableSeparator";
		var condSet0 = list(
			new CharConditions.Literal("<", CharArrayList.of('<'), Inclusion.INCLUDE),
			new CharConditions.End(">", CharArrayList.of('>'), Inclusion.INCLUDE)
		);

		var innerPipeCond = CharConditionPipe.createPipeRepeatableSeparator(name + "Inner", condSet0, null);

		var pipeCond = CharConditionPipe.createPipeOptionalSuffix(name, list(new StringConditions.Literal("empty", new String[] { "!" }, Inclusion.INCLUDE)), list(innerPipeCond));

		parseTest(false, true, name, pipeCond, " ! ");
		parseTest(false, false, name, pipeCond, "!<abc>;");
		parseTest(false, false, name, pipeCond, "!<< ");
		parseTest(false, false, name, pipeCond, "!<aa");

		parseTest(true, false, name, pipeCond, "! ", "!");
		parseTest(true, false, name, pipeCond, "!<abc>");
		parseTest(true, false, name, pipeCond, "!<a><b>");
		parseTest(true, false, name, pipeCond, "!<abc><>");
		parseTest(true, false, name, pipeCond, "!<abc><<>");
		parseTest(true, false, name, pipeCond, "!<abc><test><this works!>");
	}


	@Test
	public void compoundIdentifierTokenizerTest() {
		var parser = createCompoundIdentifierTokenizer();
		var name = parser.name();

		parseTest(false, false, name, parser, "");
		parseTest(false, false, name, parser, "one.");
		parseTest(false, true, name, parser, "12");
		parseTest(false, true, name, parser, "one..", "one.");
		parseTest(false, true, name, parser, "two.?");

		parseTest(true, false, name, parser, "one");
		parseTest(true, false, name, parser, "one.two?");
		parseTest(true, false, name, parser, "one[]");
		parseTest(true, false, name, parser, "one.two?[]");
		parseTest(true, false, name, parser, "a - b", "a");
	}


	/**
	 * @return a compound identifier parser (i.e. can parse 'Aa.Bb.Cc' as one identifier token')
	 */
	public static CharParserMatchable createCompoundIdentifierTokenizer() {
		var identifierParser = Arrays.asList(newIdentifierTokenizer());
		var separatorParser = Arrays.asList(new CharConditions.Literal("identifier namespace separator", CharArrayList.of('.'), Inclusion.INCLUDE));

		return CharConditionPipe.createPipeOptionalSuffix("compound identifier (nullable)",
			Arrays.asList(CharConditionPipe.createPipeRepeatableSeparator("compound identifier", identifierParser, separatorParser)),
			Arrays.asList(new CharConditions.Literal("nullable '?' suffix", CharArrayList.of('?'), Inclusion.INCLUDE),
					new StringConditions.Literal("array '[]' suffix", new String[] { "[]" }, Inclusion.INCLUDE)
			)
		);
	}


	/**
	 * @return a basic parser for a string of contiguous characters matching those allowed in identifiers (i.e. 'anotherVar', '$thing', or '_stspr')
	 */
	public static CharConditions.BaseCharParserMatchable newIdentifierTokenizer() {
		var firstCharSet = new CharSearchSet();
		firstCharSet.addChar('$');
		firstCharSet.addChar('_');
		firstCharSet.addRange('a', 'z');
		firstCharSet.addRange('A', 'Z');
		CharParserPredicate firstCharCheck = (char ch, TextParser parser) -> (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || ch == '_' || ch == '$';

		CharPredicate charCheck = (char ch) -> {
			return (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || ch == '_' || ch == '$';
		};

		var cond = new CharConditions.ContainsFirstSpecial("identifier", charCheck, firstCharCheck, firstCharSet.toCharList().toArray(), Inclusion.INCLUDE);
		return cond;
	}


	@SafeVarargs
	private static <T> ArrayList<T> list(T... args) {
		return new ArrayList<T>(Arrays.asList(args));
	}

}
