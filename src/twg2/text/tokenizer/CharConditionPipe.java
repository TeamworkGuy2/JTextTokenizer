package twg2.text.tokenizer;

import java.util.Arrays;
import java.util.Collection;

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
	public static <S extends CharParser> AllRequired<S> createPipeAllRequired(String name, Iterable<? extends S>... requiredConditionSets) {
		var requiredSets = new CharParser[requiredConditionSets.length][];
		int i = 0;
		for(Iterable<? extends S> requiredCondSet : requiredConditionSets) {
			var requiredCondList = ListBuilder.mutable(requiredCondSet);
			requiredSets[i] = requiredCondList.toArray(new CharParser[requiredCondList.size()]);
			i++;
		}

		return new AllRequired<S>(name, false, requiredSets);
	}


	@SafeVarargs
	public static <S extends CharParser> AllRequiredPlain<S> createPipePlainAllRequired(String name, boolean ignoreFirstConditionCoords, Iterable<? extends S>... requiredConditionSets) {
		var requiredSets = new CharParser[requiredConditionSets.length][];
		int i = 0;
		for(Iterable<? extends S> requiredCondSet : requiredConditionSets) {
			@SuppressWarnings("unchecked")
			var requiredCondList = (requiredCondSet instanceof Collection<?> ? (Collection<S>)requiredCondSet : ListBuilder.mutable(requiredCondSet));
			requiredSets[i] = requiredCondList.toArray(new CharParser[requiredCondList.size()]);
			i++;
		}

		return new AllRequiredPlain<S>(name, false, ignoreFirstConditionCoords, requiredSets);
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

		return new RepeatableSeparator<S>(name, false, conds);
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


	@SuppressWarnings("unchecked")
	@SafeVarargs
	public static <S extends CharParser> BasePipe<S> createPipeOptionalSuffixesAny(String name, Iterable<? extends S> requiredConditions, Iterable<? extends S>... optionalConditions) {
		var conds = new CharParser[optionalConditions.length + 1][];
		var requiredCondsList = (requiredConditions instanceof Collection<?> ? (Collection<S>)requiredConditions : ListBuilder.mutable((Iterable<S>)requiredConditions)); 
		conds[0] = requiredCondsList.toArray(new CharParser[requiredCondsList.size()]);

		int i = 1;
		for(Iterable<? extends S> condSet : optionalConditions) {
			var condList = (condSet instanceof Collection<?> ? (Collection<?>)condSet : ListBuilder.mutable(condSet));
			conds[i] = condList.toArray(new CharParser[condList.size()]);
			i++;
		}

		return new OptionalSuffixesAny<S>(name, false, conds);
	}



	/** An abstract list of lists of {@link ParserCondition}
	 * @param <T> The type of {@link ParserCondition} in this pipe
	 * @author TeamworkGuy2
	 * @since 2015-2-13
	 */
	public static abstract class BasePipe<T extends ParserCondition> implements CharParser {
		final boolean canReuse;
		final ParserCondition[][] conditionSets; // FIFO list of conditions in this pipe
		int curSetIndex;
		int curCondIndex;
		T curCondition; // the current condition
		boolean anyComplete;
		boolean failed;
		/** true if conditionSets after the initial one are optional, false if not */
		boolean subseqentConditionSetsOptional;
		StringBuilder dstBuf;
		String name;


		{
			this.curSetIndex = 0;
			this.curCondIndex = 0;
			this.dstBuf = new StringBuilder();
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
		public StringBuilder getParserDestination() {
			return this.dstBuf;
		}


		@Override
		public void setParserDestination(StringBuilder parserDestination) {
			this.dstBuf = parserDestination;
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
			dstBuf.setLength(0);
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
			if(!copy) {
				return conds;
			}

			for(int i = 0, size = conds.length; i < size; i++) {
				var condCopy = conds[i].copy();
				conds[i] = condCopy;
			}

			return conds;
		}


		protected static <S extends ParserCondition> BasePipe<S> copyTo(BasePipe<S> src, BasePipe<S> dst) {
			dst.subseqentConditionSetsOptional = src.subseqentConditionSetsOptional;
			return dst;
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
		public BasePipeMatchable(String name, boolean copy, CharParserMatchable firstFilter, T filter) {
			super(name, copy, (T)firstFilter, new CharParser[] { filter });
			initFirstChars(firstFilter);
		}


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




	public static class AllRequired<S extends CharParser> extends BasePipeMatchable<S> {

		@SafeVarargs
		public AllRequired(String name, boolean copy, CharParserMatchable filter, S... filters) {
			super(name, copy, filter, filters);
		}


		public AllRequired(String name, boolean copy, CharParserMatchable filter, Collection<S> filters) {
			super(name, copy, filter, filters);
		}


		protected AllRequired(String name, boolean copy, ParserCondition[][] filterSets) {
			super(name, copy, filterSets);
		}


		@Override
		public boolean acceptNext(char ch, TextParser buf) {
			if(this.curCondition == null) {
				this.failed = true;
				return false;
			}

			boolean res = this.curCondition.acceptNext(ch, buf);
			// when complete
			if(this.curCondition.isComplete()) {
				TextFragmentRef curCondCoords = this.curCondition.getMatchedTextCoords();
				super.coords = super.coords == null ? TextFragmentRef.copyMutable(curCondCoords) : TextFragmentRef.span(super.coords, curCondCoords, super.coords);

				// get the next condition
				this.curCondIndex++;
				if(this.curCondIndex < this.conditionSets[this.curSetIndex].length) {
					@SuppressWarnings("unchecked")
					var curCond = (S)this.conditionSets[this.curSetIndex][this.curCondIndex];
					this.curCondition = curCond;
				}
				else if(this.curSetIndex < this.conditionSets.length - 1) {
					this.curSetIndex++;
					this.curCondIndex = 0;
					@SuppressWarnings("unchecked")
					var curCond = (S)this.conditionSets[this.curSetIndex][this.curCondIndex];
					this.curCondition = curCond;
				}
				// or there are no conditions left (this precondition filter is complete)
				else {
					this.anyComplete = true;
					this.curCondition = null;
				}
			}

			if(!res) {
				this.failed = true;
			}
			else {
				this.dstBuf.append(ch);
			}

			return res;
		}


		@Override
		public CharParser copy() {
			var copy = new AllRequired<S>(name, true, this.conditionSets);
			BasePipe.copyTo(this, copy);
			return copy;
		}

	}




	// TODO nearly duplicate of AllRequired, combine and refactor WithMarks into a separate factory parameter
	public static class AllRequiredPlain<S extends CharParser> extends BasePipe<S> {
		private TextFragmentRefImplMut coords = null;
		private boolean ignoreFirstConditionCoords;


		public AllRequiredPlain(String name, boolean copy, boolean ignoreFirstConditionCoords, Collection<S> filters) {
			super(name, copy, filters);
			this.ignoreFirstConditionCoords = ignoreFirstConditionCoords;
		}


		protected AllRequiredPlain(String name, boolean copy, boolean ignoreFirstConditionCoords, ParserCondition[][] filterSets) {
			super(name, copy, filterSets);
			this.ignoreFirstConditionCoords = ignoreFirstConditionCoords;
		}


		@Override
		public TextFragmentRef getMatchedTextCoords() {
			return this.coords;
		}


		@Override
		public boolean acceptNext(char ch, TextParser buf) {
			if(this.curCondition == null) {
				this.failed = true;
				return false;
			}

			boolean res = this.curCondition.acceptNext(ch, buf);
			// when complete
			if(this.curCondition.isComplete()) {
				TextFragmentRef curCondCoords = this.curCondition.getMatchedTextCoords();
				if(!this.ignoreFirstConditionCoords || super.curCondIndex > 0) {
					this.coords = this.coords == null ? TextFragmentRef.copyMutable(curCondCoords) : TextFragmentRef.span(this.coords, curCondCoords, this.coords);
				}

				// get the next condition
				this.curCondIndex++;
				if(this.curCondIndex < this.conditionSets[this.curSetIndex].length) {
					@SuppressWarnings("unchecked")
					var curCond = (S)this.conditionSets[this.curSetIndex][this.curCondIndex];
					this.curCondition = curCond;
				}
				else if(this.curSetIndex < this.conditionSets.length - 1) {
					this.curSetIndex++;
					this.curCondIndex = 0;
					@SuppressWarnings("unchecked")
					var curCond = (S)this.conditionSets[this.curSetIndex][this.curCondIndex];
					this.curCondition = curCond;
				}
				// or there are no conditions left (this precondition filter is complete)
				else {
					this.anyComplete = true;
					this.curCondition = null;
				}
			}

			if(!res) {
				this.failed = true;
			}
			else {
				this.dstBuf.append(ch);
			}

			return res;
		}


		@Override
		void reset() {
			super.reset();
			this.coords = null;
		}


		@Override
		public CharParser copy() {
			var copy = new AllRequiredPlain<S>(name, true, this.ignoreFirstConditionCoords, this.conditionSets);
			BasePipe.copyTo(this, copy);
			return copy;
		}

	}




	/** Accepts multiple sets of {@link CharParser}, the first set is required, all remaining sets are optional.
	 * Parsers are extracted from the sets via the virtual sub-class overridden {@link #nextCondition()} method.
	 */
	public static abstract class AcceptMultiple<S extends CharParser> extends BasePipeMatchable<S> {

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
			if(this.curCondition == null) {
				this.failed = true;
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
				if(super.curSetIndex > 0) {
					if(super.curCondition != null && buf.hasNext()) {
						// peek at next buffer character, if optional parser accepts, lock into parsing the optional parser
						char nextCh = buf.nextChar();
						boolean nextRes = super.curCondition.acceptNext(nextCh, buf);
						buf.unread(1);
						@SuppressWarnings("unchecked")
						S condCopy = (S)super.curCondition.copyOrReuse();
						super.curCondition = condCopy;

						if(nextRes) {
							super.anyComplete = false;
						}
						// else, the required parser is complete, so isComplete() is valid
						else {
							super.anyComplete = true;
							super.curCondition = null;
						}
					}
					// no further parser input available, but since the required parser is already complete, isComplete() is valid
					else if(super.subseqentConditionSetsOptional) {
						super.anyComplete = true;
						super.curCondition = null;
					}
				}
			}

			if(!res) {
				super.failed = true;
			}
			else {
				super.dstBuf.append(ch);
			}

			return res;
		}

	}




	public static class OptionalSuffix<S extends CharParser> extends AcceptMultiple<S> {

		public OptionalSuffix(String name, boolean copy, CharParserMatchable filter, Collection<S> filters) {
			super(name, copy, filter, filters);
			super.subseqentConditionSetsOptional = true;
		}


		protected OptionalSuffix(String name, boolean copy, ParserCondition[][] filterSets) {
			super(name, copy, filterSets);
			super.subseqentConditionSetsOptional = true;
		}


		@Override
		public S nextCondition() {
			var curCondSet = super.conditionSets[super.curSetIndex];
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
			// advance to the next set of conditions
			else if(super.curSetIndex < super.conditionSets.length - 1) {
				super.anyComplete = true;
				super.curSetIndex++;
				super.curCondIndex = 0;
				curCondSet = super.conditionSets[super.curSetIndex];
				@SuppressWarnings("unchecked")
				var retCond = curCondSet.length > 0 ? (S)curCondSet[super.curCondIndex] : null;
				return retCond;
			}
			// or there are no conditions left (this precondition filter is complete)
			else {
				super.anyComplete = true;
				return null;
			}
		}


		@Override
		public CharParser copy() {
			var copy = new OptionalSuffix<S>(name, true, this.conditionSets);
			BasePipe.copyTo(this, copy);
			return copy;
		}


		@Override
		public String toString() {
			return BasePipe.conditionSetToString(super.conditionSets, ", optional then ", "", '(', ')');
		}

	}




	/** Parse a list of conditions, all conditions are optional and are parsed in order.
	 * Each condition is parsed until {@link CharParser#acceptNext(char, TextParser)} returns false.
	 * @author TeamworkGuy2
	 * @since 2016-2-7
	 */
	public static class OptionalSuffixesAny<S extends CharParser> extends BasePipeMatchable<S> {
		private boolean prevCallLookAheadSucceeded;
		private char prevCallLookAheadChar;


		public OptionalSuffixesAny(String name, boolean copy, CharParserMatchable filter, Collection<S> filters) {
			super(name, copy, filter, filters);
			super.subseqentConditionSetsOptional = true;
		}


		protected OptionalSuffixesAny(String name, boolean copy, ParserCondition[][] filterSets) {
			super(name, copy, filterSets);
			super.subseqentConditionSetsOptional = true;
		}


		@Override
		public boolean acceptNext(char ch, TextParser buf) {
			if(this.curCondition == null) {
				this.failed = true;
				return false;
			}

			// if the previous call succeeded by using a look ahead char matching the current 'ch', then return true without checking it again (don't pass 'ch' to 'curCondition' again, because parsers are stateful)
			if(this.prevCallLookAheadSucceeded && this.prevCallLookAheadChar == ch) {
				return true;
			}

			this.prevCallLookAheadSucceeded = false;

			final boolean initRes = super.curCondition.acceptNext(ch, buf);
			boolean res = initRes;
			boolean complete;
			// when complete
			if((complete = super.curCondition.isComplete()) || super.curCondition.isFailed()) {
				if(complete) {
					TextFragmentRef curCondCoords = super.curCondition.getMatchedTextCoords();
					super.coords = super.coords == null ? TextFragmentRef.copyMutable(curCondCoords) : TextFragmentRef.span(super.coords, curCondCoords, super.coords);
				}

				// check if the parser will accept more input (for optional repeat parsers), else find the next parser that will
				if(!res || buf.hasNext()) {
					// peek at next buffer character or reuse the current character (if unused), and test if the current completed parser will accept it (for repeatable parsers)
					char nextCh = initRes ? buf.nextChar() : ch;
					boolean nextRes = false;

					if(complete) {
						nextRes = super.curCondition.acceptNext(nextCh, buf);

						if(nextRes) {
							super.anyComplete = false;
							if(initRes) {
								this.prevCallLookAheadSucceeded = true;
								this.prevCallLookAheadChar = nextCh;
							}
							res = nextRes;
						}
					}
					// the current parser is complete AND will not accept more input, so
					if(!nextRes) {
						boolean nextRes2 = res;
						// while we have the peek ahead char, check if the next parser will accept it
						super.curCondition = nextCondition();
						while(super.curCondition != null) {
							nextRes2 = super.curCondition.acceptNext(nextCh, buf);
							// if this was a look ahead char
							if(initRes) {
								// since this was the first char the condition accepted, just copy and reuse, instead of setting the look ahead flag and char
								@SuppressWarnings("unchecked")
								S condCopy = (S)super.curCondition.copyOrReuse();
								super.curCondition = condCopy;
							}
							// if the next optional parser accepts the character, then lock into parsing the optional parser, else the loop checks parser until none remain
							if(nextRes2) {
								super.anyComplete = false;
								res = nextRes2;
								break;
							}
							super.curCondition = nextCondition();
						}
					}
					if(initRes) {
						buf.unread(1);
					}
				}
				// no further parser input available, but since at least one parser is already complete, isComplete() is valid
				else if(complete) {
					super.anyComplete = true;
					super.curCondition = null;
				}
			}

			if(!res) {
				super.failed = true;
			}
			else {
				super.dstBuf.append(ch);
			}

			return res;
		}


		public S nextCondition() {
			var curCondSet = super.conditionSets[super.curSetIndex];
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
			// advance to the next set of conditions
			else if(super.curSetIndex < super.conditionSets.length - 1) {
				super.anyComplete = true;
				super.curSetIndex++;
				super.curCondIndex = 0;
				curCondSet = super.conditionSets[super.curSetIndex];
				@SuppressWarnings("unchecked")
				var retCond = curCondSet.length > 0 ? (S)curCondSet[super.curCondIndex] : null;
				return retCond;
			}
			// or there are no conditions left (this precondition filter is complete)
			else {
				super.anyComplete = true;
				return null;
			}
		}


		@Override
		public CharParser copy() {
			var copy = new OptionalSuffixesAny<S>(name, true, this.conditionSets);
			BasePipe.copyTo(this, copy);
			return copy;
		}


		@Override
		public String toString() {
			return BasePipe.conditionSetToString(super.conditionSets, ", optional then ", "", '(', ')');
		}

	}




	/** Accept one or two parser conditions and parse them repeatedly, this parser is complete once the first parser condition is completed once
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
			super.subseqentConditionSetsOptional = false;
		}


		@Override
		public S nextCondition() {
			var curCondSet = super.conditionSets[super.curSetIndex];
			@SuppressWarnings("unchecked")
			S condCopy = (S)super.curCondition.copyOrReuse();
			curCondSet[super.curCondIndex] = condCopy;

			super.curCondIndex++;

			if(super.curCondIndex < curCondSet.length) {
				@SuppressWarnings("unchecked")
				var retCond = (S)curCondSet[super.curCondIndex];
				return retCond;
			}
			// advance to the separator parser or back to the element parser
			else {
				// advance to the separator parser
				if(super.curSetIndex == 0) {
					super.anyComplete = true; // the element parser just completed, so it's a valid parser stop point
					super.curSetIndex = (super.curSetIndex + 1) % super.conditionSets.length; // allows for 1 condition
				}
				// cycle back to the element parser set
				else if(super.curSetIndex > 0) {
					super.curSetIndex = 0;
				}
				else {
					throw new AssertionError("illegal repeatable separator pipe condition state (set index: " + super.curSetIndex + ")");
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
		public CharParser copy() {
			var copy = new RepeatableSeparator<S>(name, true, this.conditionSets);
			BasePipe.copyTo(this, copy);
			return copy;
		}


		@Override
		public String toString() {
			return BasePipe.conditionSetToString(super.conditionSets, ", separator ", "element ", '(', ')');
		}

	}


}
