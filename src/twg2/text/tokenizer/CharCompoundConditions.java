package twg2.text.tokenizer;

import java.util.Collection;

import twg2.arrays.ArrayManager;
import twg2.parser.condition.text.CharParser;
import twg2.parser.textFragment.TextFragmentRef;
import twg2.parser.textFragment.TextFragmentRefImplMut;
import twg2.parser.textParser.TextParser;
import twg2.text.stringUtils.StringJoin;

/**
 * @author TeamworkGuy2
 * @since 2015-3-07
 */
public class CharCompoundConditions {


	/** A collection of {@link CharParser ParserConditions}
	 * @author TeamworkGuy2
	 * @since 2015-2-21
	 */
	public static abstract class BaseFilter implements CharParser {
		CharParser[] originalConds;
		CharParser[] matchingConds;
		int matchingCondsSize;
		boolean anyComplete = false;
		boolean failed = false;
		int acceptedCount;
		TextFragmentRefImplMut coords = new TextFragmentRefImplMut();
		Runnable resetFunc;
		String name;


		public BaseFilter(String name, boolean doCopyConds, Collection<CharParser> conds) {
			this(name, doCopyConds, conds.toArray(new CharParser[conds.size()]));
		}


		@SafeVarargs
		public BaseFilter(String name, boolean doCopyConds, CharParser... conds) {
			int condsCnt = conds.length;
			this.originalConds = conds;

			CharParser[] copyConds = conds;
			if(doCopyConds) {
				copyConds = new CharParser[condsCnt];
				for(int i = 0; i < condsCnt; i++) {
					copyConds[i] = conds[i].copy();
				}
			}

			this.matchingConds = new CharParser[condsCnt];
	        System.arraycopy(copyConds, 0, this.matchingConds, 0, condsCnt);
	        this.matchingCondsSize = condsCnt;

	        this.anyComplete = false;
			this.name = name;
		}


		@Override
		public String name() {
			return name;
		}


		@Override
		public TextFragmentRef getMatchedTextCoords() {
			return coords;
		}


		@Override
		public boolean isComplete() {
			return anyComplete && !failed;
		}


		@Override
		public boolean isFailed() {
			return failed;
		}


		@Override
		public boolean canRecycle() {
			return true;
		}


		@Override
		public CharParser recycle() {
			this.reset();
			return this;
		}


		@Override
		public String toString() {
			return StringJoin.join(originalConds, ", or ");
		}


		// package-private
		void reset() {
			var origCnt = originalConds.length;
			ArrayManager.clearAndAddAll(matchingConds, originalConds, 0, origCnt);
			matchingCondsSize = origCnt;
			anyComplete = false;
			failed = false;
			coords = new TextFragmentRefImplMut();
			acceptedCount = 0;

			if(resetFunc != null) {
				resetFunc.run();
			}
		}


		/** Remove {@code matches} who's {@link CharParser#acceptNext(char, TextParser)} method return false for {@code ch}
		 * @return 0 if no match, 1 if match found, 2 if match completed
		 */
		private byte updateMatches(char ch, TextParser buf) {
			var matches = this.matchingConds;
			int size = this.matchingCondsSize;
			byte found = 0;
			// reverse iterate through the bag so we don't have to adjust the loop variable when we remove elements
			for(int i = size - 1; i > -1; i--) {
				CharParser cond = matches[i];
				if(!cond.isFailed()) {
					if(!cond.acceptNext(ch, buf)) {
						ArrayManager.removeUnordered(matches, size, i);
						size--;
					}
					else {
						found = 1;
						if(cond.isComplete()) {
							found = 2;
						}
					}
				}
				else {
					ArrayManager.removeUnordered(matches, size, i);
					size--;
				}
			}
			this.matchingCondsSize = size;
			return found;
		}

	}




	/** Accept input that matches any of the parse conditions
	 * @author TeamworkGuy2
	 * @since 2015-2-10
	 */
	public static class Filter extends BaseFilter {

		public Filter(String name, boolean doCopyConds, CharParser[] conds) {
			super(name, doCopyConds, conds);
		}


		@Override
		public boolean acceptNext(char ch, TextParser buf) {
			byte found = super.updateMatches(ch, buf);
			if(found == 2) {
				super.anyComplete = true;
			}

			if(found > 0) {
				if(super.acceptedCount == 0) {
					super.coords.setStart(buf);
				}
				super.acceptedCount++;
				if(super.anyComplete) {
					super.coords.setEnd(buf);
				}

				// TODO debugging - ensure that this compound condition ends up with the same parsed text fragment as the sub-conditions
				//if(cond.anyComplete && !condI.getCompleteMatchedTextCoords().equals(cond.coords)) {
				//	throw new RuntimeException("CharCompoundConditions " + condI.getCompleteMatchedTextCoords().toString() + " not equal to sub " + cond + " " + cond.coords.toString());
				//}

				return true;
			}
			else {
				super.failed = true;
				super.anyComplete = false;
				return false;
			}
		}


		@Override
		public Filter copy() {
			return new Filter(super.name, true, super.originalConds);
		}

	}




	/** Accept input until a full match for this parse condition is encountered
	 * @author TeamworkGuy2
	 * @since 2015-2-10
	 */
	public static class EndFilter extends BaseFilter {

		public EndFilter(String name, boolean doCopyConds, CharParser[] conds) {
			super(name, doCopyConds, conds);
		}


		@Override
		public boolean acceptNext(char ch, TextParser buf) {
			if(super.isComplete()) {
				super.failed = true;
				return false;
			}

			byte found = super.updateMatches(ch, buf);
			if(found == 2) {
				super.anyComplete = true;
			}

			if(found > 0) {
				if(super.acceptedCount == 0) {
					super.coords.setStart(buf);
				}
				super.acceptedCount++;
				if(super.anyComplete) {
					super.coords.setEnd(buf);
				}
			}
			else {
				super.reset();
			}
			return true;
		}


		@Override
		public EndFilter copy() {
			return new EndFilter(super.name, true, super.originalConds);
		}

	}

}
