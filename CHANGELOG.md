# Change Log
All notable changes to this project will be documented in this file.
This project does its best to adhere to [Semantic Versioning](http://semver.org/).


--------
### [0.5.0](N/A) - 2020-11-21
#### Changed
* Change `CharConditionPipe.peekOptionalConditionSet()` return type from `S` to `int`
* Update to jtext-parser@0.17.0 - added `getFirstChars()` to `CharCondions`, `StringConditions`, and `CharConditionPipe`


--------
### [0.4.0](https://github.com/TeamworkGuy2/JTextTokenizer/commit/ae2d2272a8e7891a94f9f2e450d616302d2f708d) - 2020-05-23
#### Added
* `CharConditions.Identifier` added to greatly simplify code parser edge cases in `jparse-code`
* Moved `Inclusion` enum from `JTextParser` into this library

#### Changed
* Merged `StringBoundedParserBuilder` into `StringParserBuilder`
* `CharConditionPipe.createPipeAllRequired()` now only supports a single list of parser conditions rather than multiple lists; switch to uses `OptionalSuffix`
* `CharCompoundConditions` `StartFilter` consolidated into `Filter`
* `CharConditions` `Start` consolidated into `Literal`
* `StringConditions` `Start` consolidated into `Literal`
* Added and improved unit tests

#### Fixed
* Several parser issues with `CharConditionPipe`, added read-ahead for optional conditions so that conditional pipes finish once required conditions are done and future input does not match any optional conditions

#### Removed
* `StringBoundedParserBuilder` (merged with `StringParserBuilder`)
* Removed `CharConditionPipe` `AllRequired` and `AllRequiredPlain` and `createPipePlainAllRequired()` since an `OptionalSuffix` constructed with a single list of parser conditions is equivalent
* Removed `CharConditionPipe` `OptionalSuffixesAny` and `createPipeOptionalSuffixesAny()` since it was unused


--------
### [0.3.0](https://github.com/TeamworkGuy2/JTextTokenizer/commit/26b81037937805dcb1a76731f50926b037fa9eb9) - 2019-03-30
Performance refactor, reduced average real world JParseCode run times by ~10%
#### Changed
* Extensive constructor parameter changes across multiple classes to allow for more efficient field types, copy() calls, and predicates for char tests
* Switched from List, Bag, and PairList for many internal fields to raw arrays where possible. Found that many classes don't modify these fields outside the constructor; adjusted constructor code to calculate size and allocate arrays directly
* `CharConditionPipe` sub-class constructors and copy() behavior optimized to prevent unnecessary copies and use arrays instead of ArrayLists


--------
### [0.2.3](https://github.com/TeamworkGuy2/JTextTokenizer/commit/793231f9412171b0c0f02c882c21476e5632148c) - 2017-11-11
#### Fixed
* Another fix for `StringConditions.End` corner case when one character has been matched and the next character isn't a match, but is a match if we restart matching


--------
### [0.2.2](https://github.com/TeamworkGuy2/JTextTokenizer/commit/5ffff76d35775c494897e82dad1b57050a82ebb5) - 2017-11-11
#### Changed
* Removed lombok dependency
* Updated README with notes about each class

#### Fixed
* `StringConditions.End` was failing when a sequence of matching characters was found followed by another valid character that required the existing sequence to be re-parsed and some characters potentially dropped from the current match (i.e. parser '-->' when parsing '<!-- comment --->')


--------
### [0.2.1](https://github.com/TeamworkGuy2/JTextTokenizer/commit/75540ad7a40e512371cbb51902ff6309f77cf11e) - 2017-08-20
#### Changed
* Update dependency `jfunc@0.3.0` (`Predicates.Char` -> `CharPredicate`)


--------
### [0.2.0](https://github.com/TeamworkGuy2/JTextTokenizer/commit/16eb6e19532be6cb692f996edfdc465f8e1f28dc) - 2016-12-03
#### Changed
* Updated jtext-parser dependency to latest 0.11.0 version (`getCompleteMatchedTextCoords()` -> `getMatchedTextCoords()`)
* Refactored `CharConditionPipe`, `CharConditions`, and `CharMultiConditionParser` to use new parsing strategy:
  * Given a character, test each non-compound parser and when a parser is found, keep reading and passing input to that parser until it completes or fails (rewind the TextParser if it fails), this fixes issues where two or more parsers were pasing the same input or compound parsers were using input from the middle of non-compound tokens
  * Required switching from `TextFragmentRef.merge()` to new `TextFragmentRef.span()` in jtext-parser@0.11.0 to create correct text fragments for compound conditions
* `CharConditions.EndNotPrecededBy` no longer supports a minimum number of characters previous to the ending character being encountered condition


--------
### [0.1.0](https://github.com/TeamworkGuy2/JTextTokenizer/commit/d012885b65b3cd044e280dde4fdd0a1231d1be3a) - 2016-10-29
#### Added
* Initial commit of text tokenization code moved from the JParseCode library into this one.
* Includes:
  * twg2.text.tokenizer - previously named twg2.parser.text in JParseCode
    * (CharConditions, CharConditionPipe, StringConditions, CharParserFactory, and other classes)
  * twg2.text.tokenizer.analytics - tokenizer stats tracking
    * ParserAction - previously nested in TokenizeStepDetails from JParserCode library
	* TypedLogger - a logger like interface meant to be a parent interface of TokenizeStepDetails and other parser analytic loggers in future
  * twg2.text.tokenizer.test - JUnit tests for twg2.text.tokenizer from JParseCode
