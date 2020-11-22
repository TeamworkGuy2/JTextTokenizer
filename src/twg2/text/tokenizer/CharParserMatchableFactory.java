package twg2.text.tokenizer;

import java.util.Arrays;

import twg2.collections.primitiveCollections.CharArrayList;
import twg2.collections.primitiveCollections.CharListSorted;
import twg2.parser.condition.text.CharParser;
import twg2.parser.condition.text.CharParserMatchable;
import twg2.parser.condition.text.CharParserPredicate;
import twg2.parser.textParser.TextParser;

/** A collection of {@link CharParser}s.
 * Accepts {@link CharParserMatchable} or custom behavior similar to {@link CharParserMatchable#getFirstCharMatcher()}
 * via separate function associated with {@link CharParser} arguments.
 * @author TeamworkGuy2
 * @since 2016-2-21
 */
public class CharParserMatchableFactory<P extends CharParser> implements CharParserFactory {
	@SuppressWarnings("unused")
	private String name;
	private P[] conditions;
	private CharParserPredicate[] firstCharConds;
	private CharParser conditionSet;
	private boolean compound;


	@SuppressWarnings("unchecked")
	public CharParserMatchableFactory(String name, boolean compound, CharParserMatchable... parsers) {
		this(name, compound, getOrCreateFirstCharPredicates(parsers, 200), (P[])parsers);
	}


	@SafeVarargs
	public CharParserMatchableFactory(String name, boolean compound, CharParserPredicate[] firstCharPredicates, P... parsers) {
		this.name = name;
		this.compound = compound;
		this.firstCharConds = firstCharPredicates;

		// optimization for single condition sets
		if(parsers.length == 1) {
			@SuppressWarnings("unchecked")
			var conds = (P[])new CharParser[] { parsers[0] };
			this.conditions = conds;
			this.conditionSet = parsers[0];
		}
		else {
			@SuppressWarnings("unchecked")
			P[] conds = (P[])new CharParser[parsers.length];
			int i = 0;
			for(P parser : parsers) {
				conds[i] = parser;
				i++;
			}
			this.conditions = conds;
			this.conditionSet = new CharCompoundConditions.Filter(name, false, conds);
		}
	}


	@Override
	public boolean isCompound() {
		return compound;
	}


	@Override
	public boolean isMatch(char ch, TextParser buf) {
		var charConds = this.firstCharConds;
		// there is always at least one charCond
		if(charConds[0].test(ch, buf)) {
			return true;
		}
		for(int i = 1, size = charConds.length; i < size; i++) {
			if(charConds[i].test(ch, buf)) {
				return true;
			}
		}
		return false;
	}


	@Override
	public CharParser createParser() {
		return conditionSet.copy();
	}


	@Override
	public String toString() {
		return (compound ? "compound " : "") + Arrays.toString(conditions);
	}


	/** Check if an array of {@link CharParserMatchable}'s all implement {@link CharParserMatchable#getFirstChars() getFirstChars()) (return non-null values).
	 * If so, build a distinct list of all the first chars from the char parsers and create a {@link CharParserPredicate} optimized for the number of first chars.
	 * If some of the char parsers don't implement {@code getFirstChars()} or there are too many unique first chars then just return the array of
	 * {@link CharParserMatchable#getFirstCharMatcher() getFirstCharMatcher()} results from the char parsers.
	 * @param charParsers the char parsers array, all elements are expected to be non-null
	 * @param maxFirstChars the maximum number of distinct first chars to optimize for, if there are more first chars, return the array of
	 * {@code getFirstCharMatcher()}'s retrieved from the char parsers.
	 * @return an array of first chars predicates, the array may contain a single optimized predicate to handle all the first chars, or simply the results
	 * array from calling {@code getFirstCharMatcher()} on all the char parsers. 
	 */
	public static CharParserPredicate[] getOrCreateFirstCharPredicates(CharParserMatchable[] charParsers, int maxFirstChars) {
		var allHaveFirstChars = true;

		for(var charParser : charParsers) {
			var firstChars = charParser.getFirstChars();
			if(firstChars == null) {
				allHaveFirstChars = false;
				break;
			}
		}

		// if all of the char parsers have a list of beginning chars, then create a composite first char matching predicate
		if(allHaveFirstChars) {
			// gather all the distinct first chars from the parsers
			var allFirstChars = new CharArrayList();
			for(var charParser : charParsers) {
				var chars = charParser.getFirstChars();
				for(int i = 0, size = chars.length; i < size; i++) {
					char ch = chars[i];
					// distinct chars
					if(!allFirstChars.contains(ch)) {
						allFirstChars.add(ch);
					}
				}
			}

			CharParserPredicate allCharsMatcher;

			// create an optimized first char matching predicate based on the number of first chars
			int count = allFirstChars.size();
			if(count == 1) {
				var ch1 = allFirstChars.get(0);
				allCharsMatcher = (ch, parser) -> {
					return ch1 == ch;
				};
			}
			else if(count == 2) {
				var ch1 = allFirstChars.get(0);
				var ch2 = allFirstChars.get(1);
				allCharsMatcher = (ch, parser) -> {
					return ch1 == ch || ch2 == ch;
				};
			}
			else if(count < 20) {
				allCharsMatcher = (ch, parser) -> {
					return allFirstChars.contains(ch);
				};
			}
			else if(count <= maxFirstChars) {
				var sortedFirstChars = new CharListSorted(count);
				sortedFirstChars.addAll(allFirstChars);
				allCharsMatcher = (ch, parser) -> {
					return sortedFirstChars.contains(ch);
				};
			}
			else {
				return getFirstCharMatchers(charParsers);
			}

			return new CharParserPredicate[] { allCharsMatcher };
		}
		else {
			return getFirstCharMatchers(charParsers);
		}
	}


	private static CharParserPredicate[] getFirstCharMatchers(CharParserMatchable[] parserMatchables) {
		int size = parserMatchables.length;
		var parserPredicates = new CharParserPredicate[size];
		for(int i = 0; i < size; i++) {
			parserPredicates[i] = parserMatchables[i].getFirstCharMatcher();
		}

		return parserPredicates;
	}

}
