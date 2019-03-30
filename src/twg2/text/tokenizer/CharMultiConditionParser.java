package twg2.text.tokenizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;

import twg2.parser.condition.text.CharParser;
import twg2.parser.textFragment.TextConsumer;
import twg2.parser.textFragment.TextFragmentRef;
import twg2.parser.textParser.TextParser;
import twg2.text.tokenizer.analytics.ParserAction;
import twg2.text.tokenizer.analytics.TypedLogger;

/** Build a token tree from text characters using multiple parser factories.<br>
 * Given multiple char parser factories, this maintains a list of in progress parsers and accepts characters if they are accepted by any in-progress parsers or
 * by any of the factories' {@link CharParserFactory#isMatch(char, TextParser)} methods.<br>
 * This parser also ensures that in progress parsers get first chance to accept new input characters and that tokens can't complete parsing inside in-progress none-compound parsers.
 * Compound parsers can contain nested tokens, the end result of the parsing process is a token tree.
 * @author TeamworkGuy2
 * @since 2015-5-29
 */
public class CharMultiConditionParser {


	/**
	 * @author TeamworkGuy2
	 * @since 2016-09-08
	 */
	static class MatcherState {
		int startOff;
		CharParser parser;
		TextConsumer consumer;

		public MatcherState(int startOff, CharParser parser, TextConsumer consumer) {
			this.startOff = startOff;
			this.parser = parser;
			this.consumer = consumer;
		}

	}




	private CharParserFactory[] conditionParserFactories;
	private TextConsumer[] conditionConsumers;
	private ArrayList<MatcherState> curCompoundMatchers;
	private TypedLogger<ParserAction, ?> parseLog;


	/**
	 * @param parseLog optional performance tracker, can be null
	 */
	@SafeVarargs
	public CharMultiConditionParser(TypedLogger<ParserAction, ?> parseLog, Entry<CharParserFactory, TextConsumer>... conditions) {
		var cpfs = new CharParserFactory[conditions.length];
		var ccs = new TextConsumer[conditions.length];
		int compoundCnt = 0;
		for(int i = 0, size = conditions.length; i < size; i++) {
			var cond = conditions[i];
			cpfs[i] = cond.getKey();
			ccs[i] = cond.getValue();
			compoundCnt += (cond.getKey().isCompound() ? 1 : 0);
		}
		this.conditionParserFactories = cpfs;
		this.conditionConsumers = ccs;
		this.curCompoundMatchers = new ArrayList<>(compoundCnt < 10 ? compoundCnt : 10);
		this.parseLog = parseLog;
	}


	public CharMultiConditionParser(TypedLogger<ParserAction, ?> parseLog, Collection<? extends Entry<CharParserFactory, TextConsumer>> conditions) {
		var condCount = conditions.size();
		var cpfs = new CharParserFactory[condCount];
		var ccs = new TextConsumer[condCount];
		int compoundCnt = 0;
		int i = 0;
		for(var cond : conditions) {
			cpfs[i] = cond.getKey();
			ccs[i] = cond.getValue();
			compoundCnt += (cond.getKey().isCompound() ? 1 : 0);
			i++;
		}
		this.conditionParserFactories = cpfs;
		this.conditionConsumers = ccs;
		this.curCompoundMatchers = new ArrayList<>(compoundCnt < 10 ? compoundCnt : 10);
		this.parseLog = parseLog;
	}


	/** Given a starting character and a {@code TextParser} read ahead to parse a non-compound token, also keeps track of compound parsers
	 * @param ch
	 * @param buf
	 * @return the number of characters read if a non-compound parser completed a token, else returns
	 */
	public int parse(char ch, TextParser buf) {
		int addedCondCount = 0;
		int charsRead = 0;
		TextFragmentRef completedToken = null;
		var conds = this.conditionParserFactories;
		var consumers = this.conditionConsumers;

		// add parsers that match
		outer:
		for(int i = 0, size = conds.length; i < size; i++) {
			CharParserFactory cond = conds[i];

			// when possible parse encountered (based on one char), try continuing parsing it
			if(cond.isMatch(ch, buf)) {
				if(cond.isCompound()) {
					CharParser parserCond = cond.createParser();
					this.curCompoundMatchers.add(new MatcherState(buf.getPosition(), parserCond, consumers[i]));
					addedCondCount++;
				}
				else {
					CharParser parser = cond.createParser();
					buf.unread(1); // unread the current character so the parser can re-accept it, although isMatch() already confirmed that it will be accepted
					while(buf.hasNext()) {
						char ch2 = buf.nextChar();
						charsRead++;

						parser.acceptNext(ch2, buf);

						boolean complete = parser.isComplete();
						boolean failed = parser.isFailed();

						if(complete) {
							completedToken = consumeToken(parser, consumers[i]);
							break outer;
						}
						else if(failed) {
							buf.unread(charsRead - 1); // since we reread the start char before the loop
							charsRead = 0;
							break;
						}
					}
				}
			}
		}

		if(parseLog != null) {
			parseLog.logCount(ParserAction.PARSER_CONDITIONS_ADDED, addedCondCount);
		}

		if(completedToken == null) {
			passCompletedCharsToCompoundParsers(ch, buf, this.parseLog, this.curCompoundMatchers);
		}

		return charsRead;
	}


	private static void passCompletedCharsToCompoundParsers(char ch, TextParser buf, TypedLogger<ParserAction, ?> parseLog, ArrayList<MatcherState> compoundMatchers) {
		int acceptedFragCount = 0;
		int acceptedCount = 0;

		// for each in-progress compound parser, check if it accepts the next token, if not, remove it from the current set of matching parsers
		// IMPORTANT: we loop backward so that more recently started parser can consume input first (this ensures that things like matching quote or parentheses are matched in order)
		for(int i = compoundMatchers.size() - 1; i > -1; i--) {
			MatcherState condEntry = compoundMatchers.get(i);
			CharParser cond = condEntry.parser;

			cond.acceptNext(ch, buf);
			acceptedCount++;

			boolean complete = cond.isComplete();
			boolean failed = cond.isFailed();

			if(complete || failed) {
				// call the consumer when the token is done being parsed AND all in-flight conditions are compound OR there are no other conditions being parsed
				// (a non-compound conditions that started parsing before this condition may or may not complete successfully)
				if(complete) {
					TextFragmentRef frag = consumeToken(cond, condEntry.consumer);
					acceptedFragCount++;

					// TODO if all remaining matchers on the curMatchers stack are compound, allow them to accept this char (which already completed a token),
					// but throw an error if any of these matchers use the char to complete or fail),
					// this fixes an issue where the first char in a compound block (e.g. '-' in "(-1)") is a token and since its parser completes in one char, the compound parser's
					// closing ")" parser never gets called and never sets the start position of its 'coords'
					if(frag.getOffsetEnd() - frag.getOffsetStart() == 1) {
						for(int k = i - 1; k > -1; k--) {
							MatcherState condEntryTmp = compoundMatchers.get(k);
							CharParser condTmp = condEntryTmp.parser;

							condTmp.acceptNext(ch, buf);

							boolean completeTmp = condTmp.isComplete();
							boolean failedTmp = condTmp.isFailed();

							if(completeTmp || failedTmp) {
								throw new IllegalStateException("compound parser '" + condTmp.name() + "' (started at " + TextFragmentRef.toStartPositionDisplayText(condTmp.getMatchedTextCoords()) + ")" +
										" used '" + ch + "' (" + buf.getPositionDisplayText() + ") to " + (completeTmp ? "complete" : "fail") +
										" but the char had already been used by '" + cond.name() + "' (started at " + TextFragmentRef.toStartPositionDisplayText(cond.getMatchedTextCoords()) + ") to complete a token");
							}
						}
					}

					compoundMatchers.remove(i);
					// IMPORTANT: this ensures that a character can only be used to complete 1 token
					break;
				}
				compoundMatchers.remove(i);
			}
		}

		if(parseLog != null) {
			parseLog.logCount(ParserAction.CHAR_CHECKS, acceptedCount);
			parseLog.logCount(ParserAction.TEXT_FRAGMENTS_CONSUMED, acceptedFragCount);
		}
	}


	private static TextFragmentRef consumeToken(CharParser parser, TextConsumer consumer) {
		CharSequence text = parser.getParserDestination();
		TextFragmentRef frag = parser.getMatchedTextCoords();
		int off = frag.getOffsetStart();

		consumer.accept(text, off, frag.getOffsetEnd() - off, frag.getLineStart(), frag.getColumnStart(), frag.getLineEnd(), frag.getColumnEnd());
		return frag;
	}

}
