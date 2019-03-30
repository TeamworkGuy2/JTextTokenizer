package twg2.text.tokenizer.test;

import static twg2.text.tokenizer.test.ParserTestUtils.parseTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import twg2.collections.primitiveCollections.CharArrayList;
import twg2.parser.Inclusion;
import twg2.text.tokenizer.CharConditionPipe;
import twg2.text.tokenizer.CharConditions;
import twg2.text.tokenizer.StringConditions;

/**
 * @author TeamworkGuy2
 * @since 2015-11-29
 */
public class CharConditionPipeFilterTest {

	@Test
	public void allrequiredTest() throws IOException {
		String name = "AllRequired";
		var condSet0 = list(
			new CharConditions.Start("<", CharArrayList.of('<'), Inclusion.INCLUDE),
			new CharConditions.End(">", CharArrayList.of('>'), Inclusion.INCLUDE)
		);

		var condSet1 = list(
			new StringConditions.Literal("test", new String[] { "test" }, Inclusion.INCLUDE)
		);

		var condSet2 = list(
			new StringConditions.End("!", new String[] { "!" }, Inclusion.INCLUDE)
		);

		var pipeCond = CharConditionPipe.createPipeAllRequired(name, condSet0, condSet1, condSet2);
		// TODO testing CharConditionPipe.setupPipeAllRequiredFilter(pipeCond);

		parseTest(false, false, name, pipeCond, "<abc>");

		parseTest(false, false, name, pipeCond.copyOrReuse(), "<abc>test");

		parseTest(false, true, name, pipeCond.copyOrReuse(), "<abc>te;");

		parseTest(false, false, name, pipeCond.copyOrReuse(), "<abc>test;");

		parseTest(true, false, name, pipeCond.copyOrReuse(), "<abc>test stuff!");
	}


	@Test
	public void optionalSuffixTest() throws IOException {
		String name = "OptionalSuffix";
		var condSet0 = list(
			new CharConditions.Start("<", CharArrayList.of('<'), Inclusion.INCLUDE),
			new CharConditions.End(">", CharArrayList.of('>'), Inclusion.INCLUDE)
		);

		var condSetOptional = list(
			new StringConditions.Literal("test", new String[] { "test" }, Inclusion.INCLUDE)
		);

		var pipeCond = CharConditionPipe.createPipeOptionalSuffix(name, condSet0, condSetOptional);
		// TODO testing CharConditionPipe.setupPipeOptionalSuffixFilter(pipeCond);

		parseTest(true, false, name, pipeCond, "<abc>");

		parseTest(false, false, name, pipeCond.copyOrReuse(), "<abc>te");

		parseTest(false, true, name, pipeCond.copyOrReuse(), "<abc>te;");

		parseTest(true, false, name, pipeCond.copyOrReuse(), "<abc>test");
	}


	@Test
	public void repeatableSeparatedText() {
		String name = "RepeatableSeparator";
		var condSet0 = list(
			new CharConditions.Start("<", CharArrayList.of('<'), Inclusion.INCLUDE),
			new CharConditions.End(">", CharArrayList.of('>'), Inclusion.INCLUDE)
		);

		var condSetOptional = list(
			new StringConditions.Literal("separator", new String[] { ", " }, Inclusion.INCLUDE)
		);

		var pipeCond = CharConditionPipe.createPipeRepeatableSeparator(name, condSet0, condSetOptional);
		// TODO testing CharConditionPipe.setupPipeRepeatableSeparatorFilter(pipeCond);

		parseTest(true, false, name, pipeCond, "<abc>");

		parseTest(false, false, name, pipeCond.copyOrReuse(), "<abc>,");

		parseTest(false, true, name, pipeCond.copyOrReuse(), "<abc>,;");

		parseTest(false, false, name, pipeCond.copyOrReuse(), "<abc>, ");

		parseTest(true, false, name, pipeCond.copyOrReuse(), "<abc>, <test>");

		parseTest(true, false, name, pipeCond.copyOrReuse(), "<abc>, <test>, <1>");

		parseTest(true, false, name, pipeCond.copyOrReuse(), "<abc>, <test>, <1>, <this works!>");
	}


	@SafeVarargs
	private static <T> ArrayList<T> list(T... args) {
		return new ArrayList<T>(Arrays.asList(args));
	}

}
