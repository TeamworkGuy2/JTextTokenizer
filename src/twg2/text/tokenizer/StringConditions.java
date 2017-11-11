package twg2.text.tokenizer;

import java.util.Arrays;
import java.util.Collection;

import twg2.arrays.ArrayUtil;
import twg2.collections.dataStructures.Bag;
import twg2.parser.Inclusion;
import twg2.parser.condition.text.CharParser;
import twg2.parser.condition.text.CharParserMatchable;
import twg2.parser.condition.text.CharParserPredicate;
import twg2.parser.textFragment.TextFragmentRef;
import twg2.parser.textFragment.TextFragmentRefImplMut;
import twg2.parser.textParser.ParserPos;
import twg2.parser.textParser.TextParser;

/** {@link CharParserMatchable} string conditions, currently includes:<br>
 *  - Literal<br>
 *  - Start<br>
 *  - End<br>
 * @author TeamworkGuy2
 * @since 2015-2-13
 */
public class StringConditions {


	/**
	 * @author TeamworkGuy2
	 * @since 2015-2-21
	 */
	public static abstract class BaseStringParser implements CharParserMatchable {
		String[] originalStrs;
		char[] firstChars;
		Bag<String> matchingStrs;
		boolean anyComplete = false;
		boolean failed = false;
		/** count all accepted characters (including characters not explicitly part of 'matchingChars') */
		int acceptedCount = 0;
		Inclusion includeMatchInRes;
		StringBuilder dstBuf = new StringBuilder();
		TextFragmentRefImplMut coords = new TextFragmentRefImplMut();
		CharParserPredicate firstCharMatcher;
		String name;


		// package-private
		BaseStringParser(String name, Collection<String> strs, Inclusion includeCondMatchInRes) {
			this(name, strs.toArray(ArrayUtil.EMPTY_STRING_ARRAY), includeCondMatchInRes);
		}


		// package-private
		BaseStringParser(String name, String[] strs, Inclusion includeCondMatchInRes) {
			this.originalStrs = strs;
			this.firstChars = new char[strs.length];
			for(int i = 0, size = strs.length; i < size; i++) {
				this.firstChars[i] = strs[i].charAt(0);
			}
			this.matchingStrs = new Bag<String>(this.originalStrs, 0, this.originalStrs.length);
			this.anyComplete = false;
			this.includeMatchInRes = includeCondMatchInRes;
			this.name = name;
			this.firstCharMatcher = (char ch, TextParser buf) -> {
				return ArrayUtil.indexOf(firstChars, ch) > -1;
			};
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
		public TextFragmentRef getMatchedTextCoords() {
			return coords;
		}


		@Override
		public CharParserPredicate getFirstCharMatcher() {
			return firstCharMatcher;
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
			return "one " + Arrays.toString(this.originalStrs);
		}


		// package-private
		void reset() {
			matchingStrs.clearAndAddAll(originalStrs);
			anyComplete = false;
			failed = false;
			dstBuf.setLength(0);
			coords = new TextFragmentRefImplMut();
			acceptedCount = 0;
		}


		/** Remove {@code matches} who's {@link String#charAt(int)} {@code off} do not match {@code ch}
		 * @return 0 if no match, 1 if match found, 2 if match completed
		 */
		// package-private
		static byte updateMatches(char ch, int off, Bag<String> matches) {
			byte found = 0;
			// reverse iterate through the bag so we don't have to adjust the loop counter when we remove elements
			for(int i = matches.size() - 1; i > -1; i--) {
				String str = matches.get(i);
				int strLen = str.length();
				// ignore string shorter than the current search offset (technically, if the precondition filter starts at offset 0, none of these should exist)
				if(strLen > off) {
					if(str.charAt(off) != ch) {
						matches.remove(i);
					}
					else {
						found = 1;
						if(strLen == off + 1) {
							found = 2;
						}
					}
				}
				else {
					matches.remove(i);
				}
			}
			return found;
		}


		public static BaseStringParser copyTo(BaseStringParser src, BaseStringParser dst) {
			dst.originalStrs = src.originalStrs;
			dst.includeMatchInRes = src.includeMatchInRes;
			return dst;
		}

	}




	/** A matcher that finds exact string sequences
	 */
	public static class Literal extends Start {

		public Literal(String name, String[] strs, Inclusion includeCondMatchInRes) {
			super(name, strs, includeCondMatchInRes);
		}


		@Override
		public Literal copy() {
			return new Literal(name, originalStrs, includeMatchInRes);
		}

	}




	/** A matcher that finds starts-with sequences
	 */
	public static class Start extends BaseStringParser {

		public Start(String name, String[] strs, Inclusion includeCondMatchInRes) {
			super(name, strs, includeCondMatchInRes);
		}


		@Override
		public boolean acceptNext(char ch, TextParser buf) {
			int off = super.dstBuf.length();
			if(super.acceptedCount > off) {
				super.failed = true;
				return false;
			}
			byte found = updateMatches(ch, off, super.matchingStrs);
			if(found == 2) {
				super.anyComplete = true;
			}

			if(found > 0) {
				if(super.acceptedCount == 0) {
					super.coords.setStart(buf);
				}
				super.acceptedCount++;
				super.dstBuf.append(ch);
				if(super.anyComplete) {
					super.coords.setEnd(buf);
				}
				return true;
			}
			else {
				super.failed = true;
				super.anyComplete = false;
				return false;
			}
		}


		@Override
		public Start copy() {
			return new Start(super.name, super.originalStrs, super.includeMatchInRes);
		}

	}




	/** A matcher that finds ends-with sequences
	 */
	public static class End extends BaseStringParser {

		public End(String name, String[] strs, Inclusion includeCondMatchInRes) {
			super(name, strs, includeCondMatchInRes);
		}


		@Override
		public boolean acceptNext(char ch, TextParser buf) {
			int off = super.dstBuf.length();
			if(super.isComplete()) {
				// if this end sequence is still matched by adding the next character
				if(findMoreRecentMatch(ch, buf)) {
					return true;
				}
				else {
					super.failed = true;
					return false;
				}
			}

			byte found = updateMatches(ch, off, super.matchingStrs);
			if(found == 2) {
				super.anyComplete = true;
			}

			if(found > 0) {
				if(super.acceptedCount == 0) {
					super.coords.setStart(buf);
				}
				super.acceptedCount++;
				super.dstBuf.append(ch);
				if(super.anyComplete) {
					super.coords.setEnd(buf);
				}
			}
			else {
				boolean match = findMoreRecentMatch(ch, buf);
				if(!match) {
					super.reset();
				}
			}
			return true;
		}


		@Override
		public End copy() {
			return new End(super.name, super.originalStrs, super.includeMatchInRes);
		}


		/** Check for a shorter matching sequence in currently matched chars and update this end condition's buffer and coords to start at the shorter sub-match
		 * @return true if a shorter match exists
		 */
		private boolean findMoreRecentMatch(char ch, TextParser buf) {
			// start i = 1 because this method should only get called when current sequence is a match and another characters is available
			for(int i = 1, size = super.dstBuf.length(); i < size; i++) {
				this.partialReset();
				for(int j = i; j < size; j++) {
					byte found = updateMatches(super.dstBuf.charAt(j), j - i, super.matchingStrs);
					if(found == 2) {
						super.anyComplete = true;
					}
				}
				// newest char
				byte found = updateMatches(ch, size - i, super.matchingStrs);
				if(found == 2) {
					super.anyComplete = true;
				}
				// found match
				if(super.matchingStrs.size() > 0) {
					super.dstBuf.delete(0, i);
					super.dstBuf.append(ch);
					super.acceptedCount = size - i + 1;
					int pos = super.coords.getOffsetStart() + i;
					int lineNum = buf.getLineNumbers().getLineNumber(pos);
					int columnNum = pos - buf.getLineNumbers().getLineOffset(lineNum);
					super.coords.setStart(new ParserPos.Impl(pos, lineNum + 1, columnNum + 1));
					if(super.anyComplete) {
						super.coords.setEnd(buf);
					}
					return true;
				}
			}
			return false;
		}


		// package-private
		void partialReset() {
			matchingStrs.clearAndAddAll(originalStrs);
			anyComplete = false;
			failed = false;
			//dstBuf.setLength(0);
			//coords = new TextFragmentRefImplMut();
			acceptedCount = 0;
		}

	}

}
