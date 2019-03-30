package twg2.text.tokenizer;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import twg2.collections.builder.ListUtil;
import twg2.parser.condition.text.CharParser;
import twg2.parser.condition.text.CharParserMatchable;
import twg2.parser.condition.text.CharParserPredicate;
import twg2.parser.textParser.TextParser;
import twg2.tuple.Tuples;

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
	public CharParserMatchableFactory(String name, boolean compound, Iterable<CharParserMatchable> parserConditions) {
		this(name, compound, ListUtil.map(parserConditions, (c) -> Tuples.of(c.getFirstCharMatcher(), c)).toArray(new Entry[0]));
	}


	@SafeVarargs
	public CharParserMatchableFactory(String name, boolean compound, Entry<CharParserPredicate, P>... parserConditions) {
		this.name = name;
		this.compound = compound;

		// optimization for single condition sets
		if(parserConditions.length == 1) {
			@SuppressWarnings("unchecked")
			var conds = (P[])new CharParser[] { parserConditions[0].getValue() };
			this.conditions = conds;
			this.conditionSet = parserConditions[0].getValue();
			this.firstCharConds = new CharParserPredicate[] { parserConditions[0].getKey() };
		}
		else {
			@SuppressWarnings("unchecked")
			P[] conds = (P[])new CharParser[parserConditions.length];
			var charConds = new CharParserPredicate[parserConditions.length];
			int i = 0;
			for(Map.Entry<CharParserPredicate, P> entry : parserConditions) {
				conds[i] = entry.getValue();
				charConds[i] = entry.getKey();
				i++;
			}
			this.conditions = conds;
			this.firstCharConds = charConds;
			this.conditionSet = new CharCompoundConditions.StartFilter(name, false, conds);
		}
	}


	@Override
	public boolean isCompound() {
		return compound;
	}


	@Override
	public boolean isMatch(char ch, TextParser buf) {
		var charConds = this.firstCharConds;
		for(int i = 0, size = charConds.length; i < size; i++) {
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

}
