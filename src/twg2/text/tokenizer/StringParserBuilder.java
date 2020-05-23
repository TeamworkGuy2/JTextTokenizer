package twg2.text.tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import twg2.collections.primitiveCollections.CharArrayList;
import twg2.parser.condition.ParserCondition;
import twg2.parser.condition.text.CharParser;
import twg2.parser.condition.text.CharParserMatchable;
import twg2.parser.condition.text.CharParserPredicate;
import twg2.tuple.Tuples;

/**
 * @author TeamworkGuy2
 * @since 2015-6-25
 */
public class StringParserBuilder {
	private List<Entry<CharParserPredicate, CharParser>> parsers;
	private boolean compound;
	private String name;


	public StringParserBuilder(String name) {
		this.parsers = new ArrayList<>();
		this.name = name;
	}


	@SuppressWarnings("unchecked")
	public CharParserFactory build() {
		return new CharParserMatchableFactory<CharParser>(name, compound, parsers.toArray(new Entry[parsers.size()]));
	}


	/**
	 * @see StringConditions.Start
	 * @see StringConditions.End
	 * @see CharConditionPipe.AllRequired
	 */
	public StringParserBuilder addStartEndMarkers(String name, String start, String end, Inclusion includeEnd) {
		var startFilter = new StringConditions.Literal(name + "-start", new String[] { start }, Inclusion.INCLUDE);
		var endFilter = new StringConditions.End(name + "-end", new String[] { end }, includeEnd);
		var cond = new CharConditionPipe.OptionalSuffix<StringConditions.End>(name, false, new ParserCondition[][] { { startFilter, endFilter } });
		this.parsers.add(Tuples.of(cond.getFirstCharMatcher(), cond));
		return this;
	}


	/**
	 * @see StringConditions.Start
	 * @see CharConditions.End
	 * @see CharConditionPipe.AllRequired
	 */
	public StringParserBuilder addStartEndMarkers(String name, String start, char end, Inclusion includeEnd) {
		var startFilter = new StringConditions.Literal(name + "-start", new String[] { start }, Inclusion.INCLUDE);
		var endFilter = new CharConditions.End(name + "-end", CharArrayList.of(end), includeEnd);
		var cond = new CharConditionPipe.OptionalSuffix<CharConditions.End>(name, false, new ParserCondition[][] { { startFilter, endFilter } });
		this.parsers.add(Tuples.of(cond.getFirstCharMatcher(), cond));
		return this;
	}


	/**
	 * @see CharConditions.Start
	 * @see StringConditions.End
	 * @see CharConditionPipe.AllRequired
	 */
	public StringParserBuilder addStartEndMarkers(String name, char start, String end, Inclusion includeEnd) {
		var startFilter = new CharConditions.Literal(name + "-start", CharArrayList.of(start), Inclusion.INCLUDE);
		var endFilter = new StringConditions.End(name + "-end", new String[] { end }, includeEnd);
		var cond = new CharConditionPipe.OptionalSuffix<StringConditions.End>(name, false, new ParserCondition[][] { { startFilter, endFilter } });
		this.parsers.add(Tuples.of(cond.getFirstCharMatcher(), cond));
		return this;
	}


	/**
	 * @see CharConditions.Start
	 * @see CharConditions.End
	 * @see CharConditionPipe.AllRequired
	 */
	public StringParserBuilder addStartEndMarkers(String name, char start, char end, Inclusion includeEnd) {
		var startFilter = new CharConditions.Literal(name + "-start", CharArrayList.of(start), Inclusion.INCLUDE);
		var endFilter = new CharConditions.End(name + "-end", CharArrayList.of(end), includeEnd);
		var cond = new CharConditionPipe.OptionalSuffix<CharConditions.End>(name, false, new ParserCondition[][] { { startFilter, endFilter } });
		this.parsers.add(Tuples.of(cond.getFirstCharMatcher(), cond));
		return this;
	}


	/**
	 * @see CharConditions.Start
	 * @see CharConditions.EndNotPrecededBy
	 * @see CharConditionPipe.AllRequired
	 */
	public StringParserBuilder addStartEndNotPrecededByMarkers(String name, char start, char notPreced, char end, Inclusion includeEnd) {
		var startFilter = new CharConditions.Literal(name + "-start", CharArrayList.of(start), Inclusion.INCLUDE);
		var endFilter = new CharConditions.EndNotPrecededBy(name + "-end", CharArrayList.of(end), includeEnd, CharArrayList.of(notPreced));
		var cond = new CharConditionPipe.OptionalSuffix<CharConditions.EndNotPrecededBy>(name, false, new ParserCondition[][] { { startFilter, endFilter } });
		this.parsers.add(Tuples.of(cond.getFirstCharMatcher(), cond));
		return this;
	}


	/**
	 * @see StringConditions.Literal
	 */
	public StringParserBuilder addStringLiteralMarker(String name, String str) {
		var cond = new StringConditions.Literal(name, new String[] { str }, Inclusion.INCLUDE);
		this.parsers.add(Tuples.of(cond.getFirstCharMatcher(), cond));
		return this;
	}


	/**
	 * @see CharConditions.Literal
	 */
	public StringParserBuilder addCharLiteralMarker(String name, char ch) {
		var cond = new CharConditions.Literal(name, CharArrayList.of(ch), Inclusion.INCLUDE);
		this.parsers.add(Tuples.of(cond.getFirstCharMatcher(), cond));
		return this;
	}


	public StringParserBuilder addCharMatcher(String name, char[] chars) {
		var cond = new CharConditions.Contains(name, CharArrayList.of(chars), Inclusion.INCLUDE);
		this.parsers.add(Tuples.of(cond.getFirstCharMatcher(), cond));
		return this;
	}


	public StringParserBuilder addConditionMatcher(CharParserMatchable conditionFilter) {
		this.parsers.add(Tuples.of(conditionFilter.getFirstCharMatcher(), conditionFilter));
		return this;
	}


	public StringParserBuilder addConditionMatcher(CharParser condition, CharParserMatchable conditionFilter) {
		this.parsers.add(Tuples.of(conditionFilter.getFirstCharMatcher(), condition));
		return this;
	}


	public StringParserBuilder isCompound(boolean compound) {
		this.compound = compound;
		return this;
	}

}
