package twg2.text.tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import twg2.collections.primitiveCollections.CharArrayList;
import twg2.parser.Inclusion;
import twg2.parser.condition.text.CharParser;
import twg2.parser.condition.text.CharParserMatchable;
import twg2.parser.condition.text.CharParserPredicate;
import twg2.text.tokenizer.CharConditionPipe.AllRequired;
import twg2.tuple.Tuples;

/**
 * @author TeamworkGuy2
 * @since 2015-2-13
 */
public class StringBoundedParserBuilder {
	private List<Entry<CharParserPredicate, CharParser>> filters;
	private boolean compound;
	private String name;


	public StringBoundedParserBuilder(String name) {
		this.filters = new ArrayList<>();
		this.name = name;
	}


	@SuppressWarnings("unchecked")
	public CharParserFactory build() {
		return new CharParserMatchableFactory<CharParser>(name, compound, filters.toArray(new Entry[filters.size()]));
	}


	public StringBoundedParserBuilder addStartEndMarkers(String name, String start, String end, Inclusion includeEnd) {
		StringConditions.Start startFilter = new StringConditions.Start(name + "-start", new String[] { start }, Inclusion.INCLUDE);
		StringConditions.End endFilter = new StringConditions.End(name + "-end", new String[] { end }, includeEnd);
		AllRequired<StringConditions.End> cond = new CharConditionPipe.AllRequired<>(name, startFilter, endFilter);
		this.filters.add(Tuples.of(cond.getFirstCharMatcher(), cond));
		return this;
	}


	public StringBoundedParserBuilder addStartEndMarkers(String name, String start, char end, Inclusion includeEnd) {
		StringConditions.Start startFilter = new StringConditions.Start(name + "-start", new String[] { start }, Inclusion.INCLUDE);
		CharConditions.End endFilter = new CharConditions.End(name + "-end", CharArrayList.of(end), includeEnd);
		AllRequired<CharConditions.End> cond = new CharConditionPipe.AllRequired<>(name, startFilter, endFilter);
		this.filters.add(Tuples.of(cond.getFirstCharMatcher(), cond));
		return this;
	}


	public StringBoundedParserBuilder addStartEndMarkers(String name, char start, String end, Inclusion includeEnd) {
		CharConditions.Start startFilter = new CharConditions.Start(name + "-start", CharArrayList.of(start), Inclusion.INCLUDE);
		StringConditions.End endFilter = new StringConditions.End(name + "-end", new String[] { end }, includeEnd);
		AllRequired<StringConditions.End> cond = new CharConditionPipe.AllRequired<>(name, startFilter, endFilter);
		this.filters.add(Tuples.of(cond.getFirstCharMatcher(), cond));
		return this;
	}


	public StringBoundedParserBuilder addStartEndMarkers(String name, char start, char end, Inclusion includeEnd) {
		CharConditions.Start startFilter = new CharConditions.Start(name + "-start", CharArrayList.of(start), Inclusion.INCLUDE);
		CharConditions.End endFilter = new CharConditions.End(name + "-end", CharArrayList.of(end), includeEnd);
		AllRequired<CharConditions.End> cond = new CharConditionPipe.AllRequired<>(name, startFilter, endFilter);
		this.filters.add(Tuples.of(cond.getFirstCharMatcher(), cond));
		return this;
	}


	public StringBoundedParserBuilder addStartEndNotPrecededByMarkers(String name, char start, char notPreced, char end, Inclusion includeEnd) {
		CharConditions.Start startFilter = new CharConditions.Start(name + "-start", CharArrayList.of(start), Inclusion.INCLUDE);
		CharConditions.EndNotPrecededBy endFilter = new CharConditions.EndNotPrecededBy(name + "-end", CharArrayList.of(end), includeEnd, CharArrayList.of(notPreced));
		AllRequired<CharConditions.EndNotPrecededBy> cond = new CharConditionPipe.AllRequired<>(name, startFilter, endFilter);
		this.filters.add(Tuples.of(cond.getFirstCharMatcher(), cond));
		return this;
	}


	public StringBoundedParserBuilder addStringLiteralMarker(String name, String str) {
		StringConditions.Literal cond = new StringConditions.Literal(name, new String[] { str }, Inclusion.INCLUDE);
		this.filters.add(Tuples.of(cond.getFirstCharMatcher(), cond));
		return this;
	}


	public StringBoundedParserBuilder addCharLiteralMarker(String name, char ch) {
		CharConditions.Literal cond = new CharConditions.Literal(name, CharArrayList.of(ch), Inclusion.INCLUDE);
		this.filters.add(Tuples.of(cond.getFirstCharMatcher(), cond));
		return this;
	}


	public StringBoundedParserBuilder addConditionMarker(CharParser condition, CharParserMatchable conditionFilter) {
		this.filters.add(Tuples.of(conditionFilter.getFirstCharMatcher(), condition));
		return this;
	}


	public StringBoundedParserBuilder isCompound(boolean compound) {
		this.compound = compound;
		return this;
	}

}
