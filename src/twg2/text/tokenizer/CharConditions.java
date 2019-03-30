package twg2.text.tokenizer;

import java.util.Arrays;

import twg2.collections.primitiveCollections.CharList;
import twg2.collections.primitiveCollections.CharListReadOnly;
import twg2.functions.predicates.CharPredicate;
import twg2.parser.Inclusion;
import twg2.parser.condition.text.CharParser;
import twg2.parser.condition.text.CharParserMatchable;
import twg2.parser.condition.text.CharParserPredicate;
import twg2.parser.textFragment.TextFragmentRef;
import twg2.parser.textFragment.TextFragmentRefImplMut;
import twg2.parser.textParser.TextParser;
import twg2.parser.textParserUtils.ReadIsMatching;

/**
 * @author TeamworkGuy2
 * @since 2015-2-13
 */
public class CharConditions {


	/**
	 * @author TeamworkGuy2
	 * @since 2016-2-20
	 */
	public static abstract class BaseCharParser implements CharParser {
		boolean anyComplete = false;
		boolean failed = false;
		char acceptedChar = 0;
		/** count all accepted characters (including characters not explicitly part of 'matchingChars') */
		int acceptedCount = 0;
		/** count accepted characters (only from 'matchingChars') */
		int matchCount = 0;
		CharListReadOnly notPreceding;
		boolean lastCharNotMatch;
		Inclusion includeMatchInRes;
		TextFragmentRefImplMut coords = new TextFragmentRefImplMut();
		StringBuilder dstBuf = new StringBuilder();
		/** Sets up accept and reset functions given this object */
		CharPredicate charMatcher;
		Object toStringSrc;
		String name;


		public BaseCharParser(String name, CharPredicate charMatcher, Inclusion includeCondMatchInRes, Object toStringSrc) {
			this.charMatcher = charMatcher;
			this.anyComplete = false;
			this.includeMatchInRes = includeCondMatchInRes;
			this.toStringSrc = toStringSrc;
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
		public StringBuilder getParserDestination() {
			return dstBuf;
		}


		@Override
		public void setParserDestination(StringBuilder parserDestination) {
			this.dstBuf = parserDestination;
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
			return "one " + (toStringSrc != null ? toStringSrc.toString() : charMatcher);
		}


		// package-private
		void acceptedCompletedChar(char ch, TextParser buf) {
			if(this.matchCount == 0) {
				this.coords.setStart(buf);
			}
			this.dstBuf.append(ch);
			this.acceptedChar = ch;
			this.acceptedCount++;
			this.matchCount++;
		}


		void acceptedMatchOrCompleteChar(char ch, TextParser buf) {
			if(this.matchCount == 0) {
				this.coords.setStart(buf);
			}

			if(this.anyComplete) {
				this.acceptedCount++;
				this.acceptedChar = ch;
				this.coords.setEnd(buf);
			}

			this.dstBuf.append(ch);
			this.matchCount++;
		}


		void reset() {
			anyComplete = false;
			failed = false;
			acceptedChar = 0;
			acceptedCount = 0;
			matchCount = 0;
			lastCharNotMatch = false;
			coords = new TextFragmentRefImplMut();
			dstBuf.setLength(0);
		}


		public static BaseCharParser copyTo(BaseCharParser src, BaseCharParser dst) {
			dst.includeMatchInRes = src.includeMatchInRes;
			dst.charMatcher = src.charMatcher;
			dst.toStringSrc = src.toStringSrc;
			dst.name = src.name;
			return dst;
		}

	}




	/**
	 * @author TeamworkGuy2
	 * @since 2015-2-21
	 */
	public static abstract class BaseCharParserMatchable extends BaseCharParser implements CharParserMatchable {
		char[] firstMatchChars;
		CharParserPredicate firstCharMatcher;


		public BaseCharParserMatchable(String name, CharPredicate charMatcher, CharParserPredicate firstCharMatcher, char[] firstMatchChars, Inclusion includeCondMatchInRes, Object toStringSrc) {
			super(name, charMatcher, includeCondMatchInRes, toStringSrc);
			this.firstMatchChars = firstMatchChars;
			if(firstCharMatcher != null) {
				this.firstCharMatcher = firstCharMatcher;
			}
			// performance optimization for single char matchers
			else if(firstMatchChars.length == 1) {
				this.firstCharMatcher = (char ch, TextParser buf) -> {
					return firstMatchChars[0] == ch;
				};
			}
			else {
				this.firstCharMatcher = (char ch, TextParser buf) -> {
					for(int i = 0, size = firstMatchChars.length; i < size; i++) {
						if(firstMatchChars[i] == ch) { return true; }
					}
					return false;
				};
			}
		}


		@Override
		public CharParserPredicate getFirstCharMatcher() {
			return firstCharMatcher;
		}


		@Override
		public String toString() {
			return "one " + (toStringSrc != null ? toStringSrc.toString() : Arrays.toString(firstMatchChars));
		}


		public static BaseCharParserMatchable copyTo(BaseCharParserMatchable src, BaseCharParserMatchable dst) {
			dst.firstMatchChars = src.firstMatchChars;
			dst.includeMatchInRes = src.includeMatchInRes;
			dst.charMatcher = src.charMatcher;
			dst.firstCharMatcher = src.firstCharMatcher;
			dst.toStringSrc = src.toStringSrc;
			dst.name = src.name;
			return dst;
		}

	}




	/**
	 * @author TeamworkGuy2
	 * @since 2015-2-10
	 */
	public static class Start extends BaseCharParserMatchable {

		public Start(String name, CharList chars, Inclusion includeCondMatchInRes) {
			super(name, chars::contains, null, chars.toArray(), includeCondMatchInRes, null);
		}


		public Start(String name, CharPredicate charMatcher, char[] firstMatchChars, Inclusion includeCondMatchInRes, Object toStringSrc) {
			super(name, charMatcher, null, firstMatchChars, includeCondMatchInRes, toStringSrc);
		}


		@Override
		public boolean acceptNext(char ch, TextParser buf) {
			if(super.anyComplete || super.failed) {
				super.failed = true;
				return false;
			}

			boolean match = super.charMatcher.test(ch);
			super.anyComplete = match;

			if(match) {
				super.acceptedCompletedChar(ch, buf);
				super.coords.setEnd(buf);
				return true;
			}
			else {
				super.failed = true;
				return false;
			}
		}


		@Override
		public Start copy() {
			return new Start(super.name, super.charMatcher, super.firstMatchChars, super.includeMatchInRes, super.toStringSrc);
		}

	}




	/**
	 */
	public static class Literal extends Start {

		public Literal(String name, CharList chars, Inclusion includeCondMatchInRes) {
			super(name, chars, includeCondMatchInRes);
		}


		public Literal(String name, CharPredicate charMatcher, char[] firstMatchChars, Inclusion includeCondMatchInRes, Object toStringSrc) {
			super(name, charMatcher, firstMatchChars, includeCondMatchInRes, toStringSrc);
		}


		@Override
		public Literal copy() {
			return new Literal(super.name, super.charMatcher, super.firstMatchChars, super.includeMatchInRes, super.toStringSrc);
		}

	}




	/** This conditions has two matchers, one for the first character and one for all subsequent characters.
	 * It continues to read until a non-matching character is encountered.
	 * @author TeamworkGuy2
	 * @since 2015-12-13
	 */
	public static class ContainsFirstSpecial extends BaseCharParserMatchable {

		public ContainsFirstSpecial(String name, CharPredicate charMatcher, char[] firstMatchChars, Inclusion includeCondMatchInRes, Object toStringSrc) {
			super(name, charMatcher, null, firstMatchChars, includeCondMatchInRes, toStringSrc);
		}


		public ContainsFirstSpecial(String name, CharPredicate charMatcher, CharParserPredicate firstCharMatcher, char[] firstMatchChars, Inclusion includeCondMatchInRes) {
			super(name, charMatcher, firstCharMatcher, firstMatchChars, includeCondMatchInRes, null);
		}


		@Override
		public boolean acceptNext(char ch, TextParser buf) {
			// fail if the condition is already complete
			if(super.anyComplete) {
				super.failed = true;
				return false;
			}

			if(super.matchCount == 0 ? super.firstCharMatcher.test(ch, buf) : super.charMatcher.test(ch)) {
				super.acceptedCompletedChar(ch, buf);

				// this condition doesn't complete until the first non-matching character
				if(!ReadIsMatching.isNext(buf, super.charMatcher, 1)) {
					super.anyComplete = true;
					super.coords.setEnd(buf); // TODO somewhat inefficient, but we can't be sure that calls to this function are sequential parser positions, so we can't move this to the failure condition
				}
				return true;
			}
			else {
				return false;
			}
		}


		@Override
		public ContainsFirstSpecial copy() {
			return new ContainsFirstSpecial(super.name, super.charMatcher, super.firstMatchChars, super.includeMatchInRes, super.toStringSrc);
		}

	}




	/** This conditions continues to read until a non-matching character is encountered.
	 * @author TeamworkGuy2
	 * @since 2015-11-27
	 */
	public static class Contains extends ContainsFirstSpecial {

		public Contains(String name, CharList chars, Inclusion includeCondMatchInRes) {
			super(name, chars::contains, null, chars.toArray(), includeCondMatchInRes);
		}


		public Contains(String name, CharPredicate charMatcher, char[] firstMatchChars, Inclusion includeCondMatchInRes, Object toStringSrc) {
			super(name, charMatcher, firstMatchChars, includeCondMatchInRes, toStringSrc);
		}


		@Override
		public Contains copy() {
			return new Contains(super.name, super.charMatcher, super.firstMatchChars, super.includeMatchInRes, super.toStringSrc);
		}

	}




	/**
	 * @author TeamworkGuy2
	 * @since 2015-2-10
	 */
	public static class End extends BaseCharParserMatchable {

		public End(String name, CharList chars, Inclusion includeCondMatchInRes) {
			super(name, chars::contains, null, chars.toArray(), includeCondMatchInRes, null);
		}


		public End(String name, CharPredicate charMatcher, char[] firstMatchChars, Inclusion includeCondMatchInRes, Object toStringSrc) {
			super(name, charMatcher, null, firstMatchChars, includeCondMatchInRes, toStringSrc);
		}


		@Override
		public boolean acceptNext(char ch, TextParser buf) {
			if(super.anyComplete || super.failed) {
				super.failed = true;
				return false;
			}
			// reverse iterate through the bag so we don't have to adjust the loop counter when we remove elements
			super.anyComplete = super.charMatcher.test(ch);

			super.acceptedMatchOrCompleteChar(ch, buf);

			return true;
		}


		@Override
		public End copy() {
			return new End(super.name, super.charMatcher, super.firstMatchChars, super.includeMatchInRes, super.toStringSrc);
		}

	}




	/**
	 * @author TeamworkGuy2
	 * @since 2015-2-21
	 */
	public static class EndNotPrecededBy extends BaseCharParserMatchable {


		public EndNotPrecededBy(String name, CharList chars, Inclusion includeCondMatchInRes, CharListReadOnly notPrecededBy) {
			super(name, chars::contains, null, chars.toArray(), includeCondMatchInRes, null);
			super.notPreceding = notPrecededBy;
		}


		public EndNotPrecededBy(String name, CharPredicate charMatcher, char[] firstMatchChars, Inclusion includeCondMatchInRes, Object toStringSrc, CharListReadOnly notPrecededBy) {
			super(name, charMatcher, null, firstMatchChars, includeCondMatchInRes, toStringSrc);
			super.notPreceding = notPrecededBy;
		}


		@Override
		public boolean acceptNext(char ch, TextParser buf) {
			if(super.anyComplete || super.failed) {
				super.failed = true;
				return false;
			}

			if(super.matchCount == 0 && buf.hasPrevChar()) {
				char prevCh = buf.prevChar();
				if(super.notPreceding.contains(prevCh)) {
					super.lastCharNotMatch = true;
				}
			}

			if(super.notPreceding.contains(ch)) {
				super.lastCharNotMatch = true;
				return true;
			}
			if(super.lastCharNotMatch) {
				super.reset();
				return true;
			}

			super.anyComplete = super.charMatcher.test(ch);

			super.acceptedMatchOrCompleteChar(ch, buf);

			return true;
		}


		@Override
		public EndNotPrecededBy copy() {
			EndNotPrecededBy copy = new EndNotPrecededBy(super.name, super.charMatcher, super.firstMatchChars, super.includeMatchInRes, super.toStringSrc, super.notPreceding);
			BaseCharParserMatchable.copyTo(this, copy);
			return copy;
		}

	}

}
