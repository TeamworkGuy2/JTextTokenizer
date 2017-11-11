package twg2.text.tokenizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
	public static <S extends CharParser> AllRequired<S> createPipeAllRequired(String name, Iterable<S>... requiredConditionSets) {
		List<List<S>> requiredSets = new ArrayList<List<S>>();
		for(Iterable<S> requiredCondSet : requiredConditionSets) {
			requiredSets.add(ListBuilder.mutable(requiredCondSet));
		}

		return new AllRequired<S>(name, requiredSets);
	}



	@SafeVarargs
	public static <S extends CharParser> AllRequiredPlain<S> createPipePlainAllRequired(String name, boolean ignoreFirstConditionCoords, Iterable<S>... requiredConditionSets) {
		List<List<S>> requiredSets = new ArrayList<List<S>>();
		for(Iterable<S> requiredCondSet : requiredConditionSets) {
			requiredSets.add(ListBuilder.mutable(requiredCondSet));
		}

		return new AllRequiredPlain<S>(name, ignoreFirstConditionCoords, requiredSets);
	}


	public static <S extends CharParser> RepeatableSeparator<S> createPipeRepeatableSeparator(String name, Iterable<? extends S> requiredConditions, Iterable<? extends S> separatorConditions) {
		List<List<? extends S>> conds = new ArrayList<List<? extends S>>();
		conds.add(ListBuilder.mutable(requiredConditions));

		if(separatorConditions != null) {
			List<? extends S> separatorSet = ListBuilder.mutable(separatorConditions);
			conds.add(separatorSet);
		}

		return new RepeatableSeparator<S>(name, conds);
	}


	@SuppressWarnings("unchecked")
	@SafeVarargs
	public static <S extends CharParser> OptionalSuffix<S> createPipeOptionalSuffix(String name, Iterable<? extends S> requiredConditions, Iterable<? extends S>... optionalConditions) {
		List<List<S>> conditionSets = new ArrayList<List<S>>();
		conditionSets.add(ListBuilder.mutable((Iterable<S>)requiredConditions));

		for(Iterable<S> condSet : (Iterable<S>[])optionalConditions) {
			conditionSets.add(ListBuilder.mutable(condSet));
		}

		return new OptionalSuffix<S>(name, conditionSets);
	}


	@SuppressWarnings("unchecked")
	@SafeVarargs
	public static <S extends CharParser> BasePipe<S> createPipeOptionalSuffixesAny(String name, Iterable<? extends S> requiredConditions, Iterable<? extends S>... optionalConditions) {
		List<List<S>> conditionSets = new ArrayList<List<S>>();
		conditionSets.add(ListBuilder.mutable((Iterable<S>)requiredConditions));

		for(Iterable<S> condSet : (Iterable<S>[])optionalConditions) {
			conditionSets.add(ListBuilder.mutable(condSet));
		}

		return new OptionalSuffixesAny<S>(name, conditionSets);
	}



	/** An abstract list of lists of {@link ParserCondition}
	 * @param <T> The type of {@link ParserCondition} in this pipe
	 * @author TeamworkGuy2
	 * @since 2015-2-13
	 */
	public static abstract class BasePipe<T extends ParserCondition> implements CharParser {
		final boolean canReuse;
		final List<List<T>> conditionSets; // FIFO list of conditions in this pipe
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


		@SafeVarargs
		public BasePipe(String name, T... filters) {
			List<T> condSet0 = new ArrayList<>();
			this.conditionSets = new ArrayList<>();
			this.conditionSets.add(condSet0);

			Collections.addAll(condSet0, filters);
			this.curCondition = condSet0.get(0);
			this.canReuse = ParserCondition.canRecycleAll(condSet0);
			this.name = name;
		}


		public BasePipe(String name, Collection<T> filters) {
			List<T> condSet0 = new ArrayList<>();
			this.conditionSets = new ArrayList<>();
			this.conditionSets.add(condSet0);

			condSet0.addAll(filters);
			this.curCondition = condSet0.get(0);
			this.canReuse = ParserCondition.canRecycleAll(condSet0);
			this.name = name;
		}


		public BasePipe(String name, List<? extends List<T>> filterSets) {
			@SuppressWarnings("unchecked")
			List<List<T>> filterSetsCast = (List<List<T>>)filterSets;
			this.conditionSets = filterSetsCast;
			this.curCondition = this.conditionSets.size() > 0 ? this.conditionSets.get(0).get(0) : null;
			boolean reusable = true;
			for(List<T> filterSet : filterSets) {
				reusable &= ParserCondition.canRecycleAll(filterSet);
			}
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
			for(int i = 0, size = conditionSets.size(); i < size; i++) {
				List<T> condSet = conditionSets.get(i);
				for(int ii = 0, sizeI = condSet.size(); ii < sizeI; ii++) {
					@SuppressWarnings("unchecked")
					T condCopy = (T)condSet.get(ii).copyOrReuse();
					condSet.set(ii, condCopy);
				}
			}
			curCondition = conditionSets.size() > 0 && conditionSets.get(0).size() > 0 ? conditionSets.get(0).get(0) : null;
		}


		@Override
		public String toString() {
			return conditionSetToString(conditionSets, ", then ", "", '(', ')');
		}


		public static <S extends ParserCondition> List<List<S>> copyConditionSets(BasePipe<S> src) {
			List<List<S>> condCopies = new ArrayList<>(src.conditionSets.size());
			for(int i = 0, size = src.conditionSets.size(); i < size; i++) {
				List<S> condSet = src.conditionSets.get(i);
				List<S> condCopy = new ArrayList<>(condSet.size());
				condCopies.add(condCopy);
				for(int ii = 0, sizeI = condSet.size(); ii < sizeI; ii++) {
					@SuppressWarnings("unchecked")
					S copy = (S)condSet.get(ii).copy();
					condCopy.add(copy);
				}
			}

			return condCopies;
		}


		public static <S extends ParserCondition> BasePipe<S> copyTo(BasePipe<S> src, BasePipe<S> dst) {
			dst.subseqentConditionSetsOptional = src.subseqentConditionSetsOptional;
			return dst;
		}


		public static String conditionSetToString(List<? extends List<?>> lists, String joiner, String prefixFirst, char prefixDelimiter, char suffixDelimiter) {
			StringBuilder sb = new StringBuilder();
			sb.append(prefixFirst);
			int maxI = lists.size() - 1;
			for(int i = 0; i < maxI; i++) {
				sb.append(prefixDelimiter);
				sb.append(StringJoin.join(lists.get(i), joiner));
				sb.append(suffixDelimiter);
				sb.append(joiner);
			}
			if(maxI > -1) {
				sb.append(prefixDelimiter);
				sb.append(StringJoin.join(lists.get(maxI), joiner));
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
		public BasePipeMatchable(String name, CharParserMatchable firstFilter, T filter) {
			super(name, (T)firstFilter);
			List<T> condSet = super.conditionSets.get(0);
			condSet.add(filter);

			initFirstChars(firstFilter);
		}


		@SuppressWarnings("unchecked")
		@SafeVarargs
		public BasePipeMatchable(String name, CharParserMatchable firstFilter, T... filters) {
			super(name, (T)firstFilter);
			List<T> condSet = super.conditionSets.get(0);
			Collections.addAll(condSet, filters);

			initFirstChars(firstFilter);
		}


		@SuppressWarnings("unchecked")
		public BasePipeMatchable(String name, CharParserMatchable firstFilter, Collection<T> filters) {
			super(name, (T)firstFilter);
			List<T> condSet = super.conditionSets.get(0);
			condSet.addAll(filters);

			initFirstChars(firstFilter);
		}


		@SuppressWarnings("unchecked")
		public BasePipeMatchable(String name, List<? extends List<? extends T>> filterSets) {
			super(name, (List<List<T>>)filterSets);
			initFirstChars((CharParserMatchable)super.conditionSets.get(0).get(0));
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
		public AllRequired(String name, CharParserMatchable filter, S... filters) {
			super(name, filter, filters);
		}


		public AllRequired(String name, CharParserMatchable filter, Collection<S> filters) {
			super(name, filter, filters);
		}


		public AllRequired(String name, List<? extends List<S>> filterSets) {
			super(name, filterSets);
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
				if(this.curCondIndex < this.conditionSets.get(this.curSetIndex).size()) {
					this.curCondition = this.conditionSets.get(this.curSetIndex).get(this.curCondIndex);
				}
				else if(this.curSetIndex < this.conditionSets.size() - 1) {
					this.curSetIndex++;
					this.curCondIndex = 0;
					this.curCondition = this.conditionSets.get(this.curSetIndex).get(this.curCondIndex);
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
			AllRequired<S> copy = new AllRequired<>(name, BasePipe.copyConditionSets(this));
			BasePipe.copyTo(this, copy);
			return copy;
		}

	}




	// TODO nearly duplicate of AllRequired, combine and refactor WithMarks into a separate factory parameter
	public static class AllRequiredPlain<S extends CharParser> extends BasePipe<S> {
		private TextFragmentRefImplMut coords = null;
		private boolean ignoreFirstConditionCoords;


		public AllRequiredPlain(String name, boolean ignoreFirstConditionCoords, Collection<S> filters) {
			super(name, filters);
			this.ignoreFirstConditionCoords = ignoreFirstConditionCoords;
		}


		public AllRequiredPlain(String name, boolean ignoreFirstConditionCoords, List<? extends List<S>> filterSets) {
			super(name, filterSets);
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
				if(this.curCondIndex < this.conditionSets.get(this.curSetIndex).size()) {
					this.curCondition = this.conditionSets.get(this.curSetIndex).get(this.curCondIndex);
				}
				else if(this.curSetIndex < this.conditionSets.size() - 1) {
					this.curSetIndex++;
					this.curCondIndex = 0;
					this.curCondition = this.conditionSets.get(this.curSetIndex).get(this.curCondIndex);
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
			AllRequiredPlain<S> copy = new AllRequiredPlain<>(name, this.ignoreFirstConditionCoords, BasePipe.copyConditionSets(this));
			BasePipe.copyTo(this, copy);
			return copy;
		}

	}




	/** Accepts multiple sets of {@link CharParser}, the first set is required, all remaining sets are optional.
	 * Parsers are extracted from the sets via the virtual sub-class overridden {@link #nextCondition()} method.
	 */
	public static abstract class AcceptMultiple<S extends CharParser> extends BasePipeMatchable<S> {

		public AcceptMultiple(String name, CharParserMatchable filter, Collection<S> filters) {
			super(name, filter, filters);
		}


		public AcceptMultiple(String name, List<? extends List<? extends S>> filterSets) {
			super(name, filterSets);
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

		public OptionalSuffix(String name, CharParserMatchable filter, Collection<S> filters) {
			super(name, filter, filters);
			setup();
		}


		public OptionalSuffix(String name, List<? extends List<S>> filterSets) {
			super(name, filterSets);
			setup();
		}


		private final void setup() {
			super.subseqentConditionSetsOptional = true;
		}


		@Override
		public S nextCondition() {
			List<S> curCondSet = super.conditionSets.get(super.curSetIndex);
			@SuppressWarnings("unchecked")
			S condCopy = (S)super.curCondition.copyOrReuse();
			curCondSet.set(super.curCondIndex, condCopy);

			super.curCondIndex++;

			// advance to the next condition in the current set
			if(super.curCondIndex < curCondSet.size()) {
				return curCondSet.get(super.curCondIndex);
			}
			// advance to the next set of conditions
			else if(super.curSetIndex < super.conditionSets.size() - 1) {
				super.anyComplete = true;
				super.curSetIndex++;
				super.curCondIndex = 0;
				curCondSet = super.conditionSets.get(super.curSetIndex);
				return curCondSet.size() > 0 ? curCondSet.get(super.curCondIndex) : null;
			}
			// or there are no conditions left (this precondition filter is complete)
			else {
				super.anyComplete = true;
				return null;
			}
		}


		@Override
		public CharParser copy() {
			OptionalSuffix<S> copy = new OptionalSuffix<>(name, BasePipe.copyConditionSets(this));
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


		public OptionalSuffixesAny(String name, CharParserMatchable filter, Collection<S> filters) {
			super(name, filter, filters);
			setup();
		}


		public OptionalSuffixesAny(String name, List<? extends List<? extends S>> filterSets) {
			super(name, filterSets);
			setup();
		}


		private final void setup() {
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
			List<S> curCondSet = super.conditionSets.get(super.curSetIndex);
			@SuppressWarnings("unchecked")
			S condCopy = (S)super.curCondition.copyOrReuse();
			curCondSet.set(super.curCondIndex, condCopy);

			super.curCondIndex++;

			// advance to the next condition in the current set
			if(super.curCondIndex < curCondSet.size()) {
				return curCondSet.get(super.curCondIndex);
			}
			// advance to the next set of conditions
			else if(super.curSetIndex < super.conditionSets.size() - 1) {
				super.anyComplete = true;
				super.curSetIndex++;
				super.curCondIndex = 0;
				curCondSet = super.conditionSets.get(super.curSetIndex);
				return curCondSet.size() > 0 ? curCondSet.get(super.curCondIndex) : null;
			}
			// or there are no conditions left (this precondition filter is complete)
			else {
				super.anyComplete = true;
				return null;
			}
		}


		@Override
		public CharParser copy() {
			OptionalSuffixesAny<S> copy = new OptionalSuffixesAny<>(name, BasePipe.copyConditionSets(this));
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

		public RepeatableSeparator(String name, CharParserMatchable filter, Collection<S> filters) {
			super(name, filter, filters);
			setup();
		}


		public RepeatableSeparator(String name, List<? extends List<? extends S>> filterSets) {
			super(name, filterSets);
			setup();
		}


		private final void setup() {
			if(super.conditionSets.size() > 2) {
				// technically this allows for 1 element repeating parsers
				throw new IllegalStateException("a repeatable separator pipe condition can only contain 2 condition sets, the element parser and the separator parser");
			}
			super.subseqentConditionSetsOptional = false;
		}


		@Override
		public S nextCondition() {
			List<S> curCondSet = super.conditionSets.get(super.curSetIndex);
			@SuppressWarnings("unchecked")
			S condCopy = (S)super.curCondition.copyOrReuse();
			curCondSet.set(super.curCondIndex, condCopy);

			super.curCondIndex++;

			if(super.curCondIndex < curCondSet.size()) {
				return curCondSet.get(super.curCondIndex);
			}
			// advance to the separator parser or back to the element parser
			else {
				// advance to the separator parser
				if(super.curSetIndex == 0) {
					super.anyComplete = true; // the element parser just completed, so it's a valid parser stop point
					super.curSetIndex = (super.curSetIndex + 1) % super.conditionSets.size(); // allows for 1 condition
				}
				// cycle back to the element parser set
				else if(super.curSetIndex > 0) {
					super.curSetIndex = 0;
				}
				else {
					throw new AssertionError("illegal repeatable separator pipe condition state (set index: " + super.curSetIndex + ")");
				}

				super.curCondIndex = 0;
				curCondSet = super.conditionSets.get(super.curSetIndex);
				for(int i = 0, size = curCondSet.size(); i < size; i++) {
					@SuppressWarnings("unchecked")
					S curCond = (S)curCondSet.get(i).copyOrReuse();
					curCondSet.set(i, curCond);
				}
				return curCondSet.size() > 0 ? curCondSet.get(super.curCondIndex) : null;
			}
		}


		@Override
		public CharParser copy() {
			RepeatableSeparator<S> copy = new RepeatableSeparator<>(name, BasePipe.copyConditionSets(this));
			BasePipe.copyTo(this, copy);
			return copy;
		}


		@Override
		public String toString() {
			return BasePipe.conditionSetToString(super.conditionSets, ", separator ", "element ", '(', ')');
		}

	}


}
