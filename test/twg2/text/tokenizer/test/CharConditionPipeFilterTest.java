package twg2.text.tokenizer.test;

import static twg2.text.tokenizer.test.ParserTestUtils.parseTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import twg2.collections.primitiveCollections.CharArrayList;
import twg2.parser.Inclusion;
import twg2.parser.condition.text.CharParser;
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
		List<CharParser> condSet0 = new ArrayList<>(Arrays.asList(
				new CharConditions.Start("<", CharArrayList.of('<'), Inclusion.INCLUDE),
				new CharConditions.End(">", CharArrayList.of('>'), Inclusion.INCLUDE)
		));

		List<CharParser> condSet1 = new ArrayList<>(Arrays.asList(
				new StringConditions.Literal("test", new String[] { "test" }, Inclusion.INCLUDE)
		));

		List<CharParser> condSet2 = new ArrayList<>(Arrays.asList(
				new StringConditions.End("!", new String[] { "!" }, Inclusion.INCLUDE)
		));

		List<List<CharParser>> condSets = new ArrayList<>(Arrays.asList(
				condSet0,
				condSet1,
				condSet2
		));

		CharConditionPipe.BasePipe<CharParser> pipeCond = new CharConditionPipe.AllRequired<CharParser>(name, condSets);
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
		List<CharParser> condSet0 = new ArrayList<>(Arrays.asList(
				new CharConditions.Start("<", CharArrayList.of('<'), Inclusion.INCLUDE),
				new CharConditions.End(">", CharArrayList.of('>'), Inclusion.INCLUDE)
		));

		List<CharParser> condSetOptional = new ArrayList<>(Arrays.asList(
				new StringConditions.Literal("test", new String[] { "test" }, Inclusion.INCLUDE)
		));

		List<List<CharParser>> condSets = new ArrayList<>(Arrays.asList(
				condSet0,
				condSetOptional
		));

		CharConditionPipe.OptionalSuffix<CharParser> pipeCond = new CharConditionPipe.OptionalSuffix<CharParser>(name, condSets);
		// TODO testing CharConditionPipe.setupPipeOptionalSuffixFilter(pipeCond);

		parseTest(true, false, name, pipeCond, "<abc>");

		parseTest(false, false, name, pipeCond.copyOrReuse(), "<abc>te");

		parseTest(false, true, name, pipeCond.copyOrReuse(), "<abc>te;");

		parseTest(true, false, name, pipeCond.copyOrReuse(), "<abc>test");
	}


	@Test
	public void repeatableSeparatedText() {
		String name = "RepeatableSeparator";
		List<CharParser> condSet0 = new ArrayList<CharParser>(Arrays.asList(
				new CharConditions.Start("<", CharArrayList.of('<'), Inclusion.INCLUDE),
				new CharConditions.End(">", CharArrayList.of('>'), Inclusion.INCLUDE)
		));

		List<CharParser> condSetOptional = new ArrayList<CharParser>(Arrays.asList(
				new StringConditions.Literal("separator", new String[] { ", " }, Inclusion.INCLUDE)
		));

		List<List<CharParser>> condSets = new ArrayList<>(Arrays.asList(
				condSet0,
				condSetOptional
		));

		CharConditionPipe.RepeatableSeparator<CharParser> pipeCond = new CharConditionPipe.RepeatableSeparator<CharParser>(name, condSets);
		// TODO testing CharConditionPipe.setupPipeRepeatableSeparatorFilter(pipeCond);

		parseTest(true, false, name, pipeCond, "<abc>");

		parseTest(false, false, name, pipeCond.copyOrReuse(), "<abc>,");

		parseTest(false, true, name, pipeCond.copyOrReuse(), "<abc>,;");

		parseTest(false, false, name, pipeCond.copyOrReuse(), "<abc>, ");

		parseTest(true, false, name, pipeCond.copyOrReuse(), "<abc>, <test>");

		parseTest(true, false, name, pipeCond.copyOrReuse(), "<abc>, <test>, <1>");

		parseTest(true, false, name, pipeCond.copyOrReuse(), "<abc>, <test>, <1>, <this works!>");
	}

}
