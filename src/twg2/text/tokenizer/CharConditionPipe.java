package twg2.text.tokenizer;

import java.util.Arrays;
import java.util.Collection;

import twg2.arrays.ArrayManager;
import twg2.arrays.ArrayUtil;
import twg2.collections.builder.ListBuilder;
import twg2.parser.condition.ParserCondition;
import twg2.parser.condition.text.CharParser;
import twg2.parser.condition.text.CharParserMatchable;
import twg2.parser.condition.text.CharParserPredicate;
import twg2.parser.textFragment.TextFragmentRef;
import twg2.parser.textFragment.TextFragmentRefImplMut;
import twg2.parser.textParser.TextParser;
import twg2.text.stringUtils.StringJoin;

/** A compound {@link CharParser} that does not complete until all internal conditions have been completed
 * @author TeamworkGuy2
 * @since 2015-2-13
 */
public class CharConditionPipe {

	@SafeVarargs
	public static <S extends CharParser> OptionalSuffix<S> createPipeAllRequired(String name, S... requiredConditions) {
		return new OptionalSuffix<S>(name, false, new CharParser[][] {
			requiredConditions
		});
	}


	public static <S extends CharParser> OptionalSuffix<S> createPipeAllRequired(String name, Iterable<? extends S> requiredConditions) {
		var requiredCondList = ListBuilder.mutable(requiredConditions);
		var requiredSets = new CharParser[][] {
			requiredCondList.toArray(new CharParser[requiredCondList.size()])
		};

		return new OptionalSuffix<S>(name, false, requiredSets);
	}


	public static <S extends CharParser> RepeatableSeparator<S> createPipeRepeatableSeparator(String name, Iterable<? extends S> requiredConditions, Iterable<? extends S> separatorConditions) {
		var conds = new CharParser[separatorConditions != null ? 2 : 1][];
		@SuppressWarnings("unchecked")
		var requiredCondsList = (requiredConditions instanceof Collection<?> ? (Collection<S>)requiredConditions : ListBuilder.mutable(requiredConditions));
		conds[0] = requiredCondsList.toArray(new CharParser[requiredCondsList.size()]);

		if(separatorConditions != null) {
			@SuppressWarnings("unchecked")
			var separatorSet = (separatorConditions instanceof Collection<?> ? (Collection<S>)separatorConditions : ListBuilder.mutable(separatorConditions));
			conds[1] = separatorSet.toArray(new CharParser[separatorSet.size()]);
		}

		var parser = new RepeatableSeparator<S>(name, false, conds);

		if(separatorConditions == null) {
			parser.firstConditionSetOptional = true;
		}

		return parser;
	}


	@SafeVarargs
	public static <S extends CharParser> OptionalSuffix<S> createPipeOptionalSuffix(String name, Iterable<? extends S> requiredConditions, Iterable<? extends S>... optionalConditions) {
		var conds = new CharParser[optionalConditions.length + 1][];
		@SuppressWarnings("unchecked")
		var requiredCondsList = (requiredConditions instanceof Collection<?> ? (Collection<S>)requiredConditions : ListBuilder.mutable((Iterable<S>)requiredConditions));
		conds[0] = requiredCondsList.toArray(new CharParser[requiredCondsList.size()]);

		int i = 1;
		for(Iterable<? extends S> condSet : optionalConditions) {
			@SuppressWarnings("unchecked")
			var condList = (condSet instanceof Collection<?> ? (Collection<S>)condSet : ListBuilder.mutable(condSet));
			conds[i] = condList.toArray(new CharParser[condList.size()]);
			i++;
		}

		return new OptionalSuffix<S>(name, false, conds);
	}



	/** An abstract list of lists of {@link ParserCondition}
	 * @param <T> The type of {@link ParserCondition} in this pipe
	 * @author TeamworkGuy2
	 * @since 2015-2-13
	 */
	public static abstract class BasePipe<T extends ParserCondition> implements CharParser {
		final boolean canReuse;
		/** FIFO list of conditions in this pipe.
		 * The first sub-array conditions are required.
		 * Subsequent sub-arrays are optional
		 */
		final ParserCondition[][] conditionSets;
		int curSetIndex;
		int curCondIndex;
		T curCondition; // the current condition
		boolean anyComplete;
		boolean failed;
		String name;


		{
			this.curSetIndex = 0;
			this.curCondIndex = 0;
		}


		@SuppressWarnings("unchecked")
		public BasePipe(String name, boolean copy, T firstFilter, ParserCondition[] subsequentFilters) {
			ParserCondition[] condSet0 = new ParserCondition[subsequentFilters.length + 1];
			condSet0[0] = firstFilter;
			System.arraycopy(subsequentFilters, 0, condSet0, 1, subsequentFilters.length);
			copyConditionsInPlace(copy, condSet0);
			firstFilter = (T)condSet0[0]; // read back from array after copy

			this.conditionSets = new ParserCondition[][] { condSet0 };
			this.curCondition = firstFilter;
			this.canReuse = ParserCondition.canRecycleAll(condSet0);
			this.name = name;
		}


		@SuppressWarnings("unchecked")
		public BasePipe(String name, boolean copy, Collection<T> filters) {
			ParserCondition[] condSet0 = copyConditionsInPlace(copy, filters.toArray(new ParserCondition[0]));

			this.conditionSets = new ParserCondition[][] { condSet0 };
			this.curCondition = (T)condSet0[0];
			this.canReuse = ParserCondition.canRecycleAll(condSet0);
			this.name = name;
		}


		protected BasePipe(String name, boolean copy, ParserCondition[][] filterSets) {
			int filterSetsCnt = filterSets.length;
			var condCopies = new ParserCondition[filterSetsCnt][];
			boolean reusable = true;
			for(int i = 0; i < filterSetsCnt; i++) {
				var filters = filterSets[i];
				var filtersCopy = copyConditionsInPlace(copy, Arrays.copyOf(filters, filters.length));
				condCopies[i] = filtersCopy;
				reusable &= ParserCondition.canRecycleAll(filtersCopy);
			}
			@SuppressWarnings("unchecked")
			var curCond = filterSetsCnt > 0 ? (T)condCopies[0][0] : null;

			this.conditionSets = condCopies;
			this.curCondition = curCond;
			this.canReuse = reusable;
			this.name = name;
		}


		@Override
		public String name() {
			return name;
		}


		@Override
		public boolean isComplete() {
			return !failed && anyComplete;
		}


		@Override
		public boolean isFailed() {
			return failed;
		}


		@Override
		public boolean canRecycle() {
			return canReuse;
		}


		@Override
		public CharParser recycle() {
			this.reset();
			return this;
		}


		void reset() {
			curCondIndex = 0;
			curSetIndex = 0;
			anyComplete = false;
			failed = false;
			for(int i = 0, size = conditionSets.length; i < size; i++) {
				var condSet = conditionSets[i];
				for(int ii = 0, sizeI = condSet.length; ii < sizeI; ii++) {
					@SuppressWarnings("unchecked")
					T condCopy = (T)condSet[ii].copyOrReuse();
					condSet[ii] = condCopy;
				}
			}
			@SuppressWarnings("unchecked")
			var curCond = conditionSets.length > 0 && conditionSets[0].length > 0 ? (T)conditionSets[0][0] : null;
			curCondition = curCond;
		}


		@Override
		public String toString() {
			return conditionSetToString(conditionSets, ", then ", "", '(', ')');
		}


		public static ParserCondition[] copyConditionsInPlace(boolean copy, ParserCondition[] conds) {
			if(copy) {
				for(int i = 0, size = conds.length; i < size; i++) {
					var condCopy = conds[i].copy();
					conds[i] = condCopy;
				}
			}
			return conds;
		}


		public static String conditionSetToString(Object[][] lists, String joiner, String prefixFirst, char prefixDelimiter, char suffixDelimiter) {
			StringBuilder sb = new StringBuilder();
			sb.append(prefixFirst);
			int maxI = lists.length - 1;
			for(int i = 0; i < maxI; i++) {
				var list = lists[i];
				sb.append(prefixDelimiter);
				StringJoin.join(list, 0, list.length, joiner, sb);
				sb.append(suffixDelimiter);
				sb.append(joiner);
			}
			if(maxI > -1) {
				sb.append(prefixDelimiter);
				StringJoin.join(lists[maxI], 0, lists[maxI].length, joiner, sb);
				sb.append(suffixDelimiter);
			}
			return sb.toString();
		}

	}




	/** A {@link BasePipe} with the same type of {@link CharParser} from start to end
	 * @param <T> the type of parser mark conditions in this pipe
	 * @author TeamworkGuy2
	 * @since 2015-2-22
	 */
	public static abstract class BasePipeMatchable<T extends CharParser> extends BasePipe<T> implements CharParserMatchable {
		private CharParserPredicate firstCharFilter;
		private TextFragmentRefImplMut coords = null;


		@SuppressWarnings("unchecked")
		@SafeVarargs
		public BasePipeMatchable(String name, boolean copy, CharParserMatchable firstFilter, T... filters) {
			super(name, copy, (T)firstFilter, filters);
			initFirstChars(firstFilter);
		}


		@SuppressWarnings("unchecked")
		public BasePipeMatchable(String name, boolean copy, CharParserMatchable firstFilter, Collection<T> filters) {
			super(name, copy, (T)firstFilter, filters.toArray(new CharParser[0]));
			initFirstChars(firstFilter);
		}


		protected BasePipeMatchable(String name, boolean copy, ParserCondition[][] filterSets) {
			super(name, copy, filterSets);
			initFirstChars((CharParserMatchable)super.conditionSets[0][0]);
		}


		private final void initFirstChars(CharParserMatchable firstFilter) {
			this.firstCharFilter = firstFilter.getFirstCharMatcher();
		}


		@Override
		public CharParserPredicate getFirstCharMatcher() {
			return this.firstCharFilter;
		}


		@Override
		public TextFragmentRef getMatchedTextCoords() {
			return this.coords;
		}


		@Override
		void reset() {
			super.reset();
			this.coords = null;
		}

	}




	/** Accepts multiple sets of {@link CharParser}, the first set is required, all remaining sets are optional.
	 * Parsers are extracted from the sets via the virtual sub-class overridden {@link #nextCondition()} method.
	 */
	public static abstract class AcceptMultiple<S extends CharParser> extends BasePipeMatchable<S> {
		boolean firstConditionSetOptional = false;

		public AcceptMultiple(String name, boolean copy, CharParserMatchable filter, Collection<S> filters) {
			super(name, copy, filter, filters);
		}


		protected AcceptMultiple(String name, boolean copy, ParserCondition[][] filterSets) {
			super(name, copy, filterSets);
		}


		/** Logic for getting the next condition based on the current set and condition indices
		 * @return the next condition to parse or null if there are no remaining conditions
		 */
		public abstract S nextCondition();


		@Override
		public boolean acceptNext(char ch, TextParser buf) {
			if(super.curCondition == null) {
				super.failed = true;
				return false;
			}

			boolean res = super.curCondition.acceptNext(ch, buf);
			// when complete
			if(super.curCondition.isComplete()) {
				TextFragmentRef curCondCoords = super.curCondition.getMatchedTextCoords();
				super.coords = super.coords == null ? TextFragmentRef.copyMutable(curCondCoords) : TextFragmentRef.span(super.coords, curCondCoords, super.coords);

				// get the next condition, or null
				super.curCondition = nextCondition();

				// required parser done, optional parsers next
				if(super.curSetIndex > 0 || this.firstConditionSetOptional) {
					if(super.curCondition != null && buf.hasNext()) {
						// try to read optional parser condition, rewinds the buf after checking ahead
						var matchCond = peekOptionalConditionSet(buf);

						// an optional condition read-ahead matched/completed, so use that condition
						if(matchCond != null) {
							super.anyComplete = false;

							// reset the matched condition - since it was used to peek ahead
							var condSet = super.conditionSets[super.curSetIndex];
							int matchIdx = ArrayUtil.indexOf(condSet, matchCond);
							@SuppressWarnings("unchecked")
							var condCopy = (S)condSet[matchIdx].copyOrReuse();
							condSet[matchIdx] = condCopy;
							// set the current condition and index to the condition which fully matched or matched until input ran out
							super.curCondition = condCopy;
							super.curCondIndex = matchIdx;
						}
						// if the optional condition failed
						else {
							super.anyComplete = true;
						}

						return res;
					}
					// no further parser input available, but since the required parser is already complete, isComplete() is valid
					else {
						super.anyComplete = true;
						super.curCondition = null;
					}
				}
			}

			if(!res) {
				// only fail if the current condition is required
				if(super.curSetIndex == 0) {
					super.failed = true;
				}
				else {
					// reset the current optional condition - since it failed reading the current character
					@SuppressWarnings("unchecked")
					S condCopy = super.curCondition = (S)super.curCondition.copyOrReuse();
					super.conditionSets[super.curSetIndex][super.curCondIndex] = condCopy;
				}
			}

			return res;
		}


		/** Peek ahead to try to match the parser input to any of the current set of conditions.
		 * Rewind the parser once all conditions fail, one condition completes, or input runs out.
		 * @return the first condition (by 'conditionSets[]' sub-index) which matches the input {@code buf} until {@link #isComplete()} or has not failed when input runs out.
		 */
		public S peekOptionalConditionSet(TextParser buf) {
			int cnt = 0;
			var condSet = super.conditionSets[super.curSetIndex];
			int size = condSet.length;
			int matchesCnt = size;
			S matchCompleted = null;
			// copy the current condition set array to use as a temp bag of matching conditions
			var matches = new ParserCondition[size];
			System.arraycopy(condSet, 0, matches, 0, size);

			outer:
			while(buf.hasNext() && matchesCnt > 0) {
				char nextCh = buf.nextChar();
				// iterate through optional conditions that still match, eliminate conditions that don't match this char
				for(int i = 0; i < matchesCnt; i++) {
					@SuppressWarnings("unchecked")
					var cond = (S)matches[i];
					var res = cond.acceptNext(nextCh, buf);
					if(!res) {
						ArrayManager.removeUnordered(matches, size, i);
						matchesCnt--;
						i--;
					}
					else if(cond.isComplete()) {
						matchCompleted = cond;
						cnt++;
						break outer;
					}
				}
				cnt++;
			}

			if(cnt > 0) {
				buf.unread(cnt);
			}

			// if there are conditions still not finished (i.e. the text parser ran out of input) then use one of those as the matches
			int matchIdx = matchCompleted != null ? ArrayUtil.indexOf(condSet, matchCompleted) : (matchesCnt > 0 ? ArrayUtil.indexOf(condSet, matches[0]) : -1);

			// reset condition set after testing (except for the match if one was found)
			for(int i = 0; i < size; i++) {
				if(i != matchIdx) {
					condSet[i] = condSet[i].copyOrReuse();
				}
			}

			// return the matching condition (might have completed or not)
			@SuppressWarnings("unchecked")
			var resCond = matchIdx > -1 ? (S)condSet[matchIdx] : null;
			return resCond;
		}

	}




	public static class OptionalSuffix<S extends CharParser> extends AcceptMultiple<S> {

		public OptionalSuffix(String name, boolean copy, CharParserMatchable filter, Collection<S> filters) {
			super(name, copy, filter, filters);
		}


		protected OptionalSuffix(String name, boolean copy, ParserCondition[][] filterSets) {
			super(name, copy, filterSets);
		}


		@Override
		public S nextCondition() {
			var curCondSet = super.conditionSets[super.curSetIndex];
			// reset the current condition
			@SuppressWarnings("unchecked")
			S condCopy = (S)super.curCondition.copyOrReuse();
			curCondSet[super.curCondIndex] = condCopy;

			super.curCondIndex++;

			// advance to the next condition in the current set
			if(super.curCondIndex < curCondSet.length) {
				@SuppressWarnings("unchecked")
				var curCond = (S)curCondSet[super.curCondIndex];
				return curCond;
			}
			// advance to the next optional set of conditions
			else if(super.curSetIndex < super.conditionSets.length - 1) {
				super.anyComplete = true;
				super.curSetIndex++;
				super.curCondIndex = 0;
				curCondSet = super.conditionSets[super.curSetIndex];
				@SuppressWarnings("unchecked")
				var curCond = curCondSet.length > 0 ? (S)curCondSet[super.curCondIndex] : null;
				return curCond;
			}
			// or there are no conditions left (this precondition filter is complete)
			else {
				super.anyComplete = true;
				return null;
			}
		}


		@Override
		public OptionalSuffix<S> copy() {
			return new OptionalSuffix<S>(name, true, super.conditionSets);
		}


		@Override
		public String toString() {
			return BasePipe.conditionSetToString(super.conditionSets, ", optional then ", "", '(', ')');
		}

	}




	/** Accept one or two parser condition sets and parses them cyclically. This parser is complete once the first parser condition set completes once
	 * @author TeamworkGuy2
	 * @since 2016-2-7
	 * @param <S> the type of parsers in this set
	 */
	public static class RepeatableSeparator<S extends CharParser> extends AcceptMultiple<S> {

		public RepeatableSeparator(String name, boolean copy, CharParserMatchable filter, Collection<S> filters) {
			super(name, copy, filter, filters);
			setup();
		}


		protected RepeatableSeparator(String name, boolean copy, ParserCondition[][] filterSets) {
			super(name, copy, filterSets);
			setup();
		}


		private final void setup() {
			if(super.conditionSets.length > 2) {
				// technically this allows for 1 element repeating parsers
				throw new IllegalStateException("a repeatable separator pipe condition can only contain 2 condition sets, the element parser and the separator parser");
			}
		}


		@Override
		public S nextCondition() {
			var curCondSet = super.conditionSets[super.curSetIndex];
			// reset the current condition
			@SuppressWarnings("unchecked")
			S condCopy = (S)super.curCondition.copyOrReuse();
			curCondSet[super.curCondIndex] = condCopy;

			super.curCondIndex++;

			// advance to the next condition in the current set
			if(super.curCondIndex < curCondSet.length) {
				@SuppressWarnings("unchecked")
				var retCond = (S)curCondSet[super.curCondIndex];
				return retCond;
			}
			// advance to the separator parser or back to the element parser
			else {
				// advance to the separator parser
				if(super.curSetIndex == 0 && !super.firstConditionSetOptional) {
					super.anyComplete = true; // the element parser just completed, so it's a valid parser stop point
					super.curSetIndex = (super.curSetIndex + 1) % super.conditionSets.length; // allows for 1 condition
				}
				// cycle back to the element parser set
				else {
					super.anyComplete = false; // the separator parser just completed, so it's NOT a valid parser stop point, an element parser is expected to complete next
					super.curSetIndex = 0;
				}

				super.curCondIndex = 0;
				curCondSet = super.conditionSets[super.curSetIndex];
				for(int i = 0, size = curCondSet.length; i < size; i++) {
					@SuppressWarnings("unchecked")
					S curCond = (S)curCondSet[i].copyOrReuse();
					curCondSet[i] = curCond;
				}
				@SuppressWarnings("unchecked")
				var retCond = curCondSet.length > 0 ? (S)curCondSet[super.curCondIndex] : null;
				return retCond;
			}
		}


		@Override
		public RepeatableSeparator<S> copy() {
			var copy = new RepeatableSeparator<S>(name, true, super.conditionSets);
			copy.firstConditionSetOptional = super.firstConditionSetOptional;
			return copy;
		}


		@Override
		public String toString() {
			return BasePipe.conditionSetToString(super.conditionSets, ", separator ", "element ", '(', ')');
		}

	}


}
