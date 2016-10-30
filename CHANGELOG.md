# Change Log
All notable changes to this project will be documented in this file.
This project does its best to adhere to [Semantic Versioning](http://semver.org/).


--------
###[0.1.0](N/A) - 2016-10-29
#### Added
* Initial commit of text tokenization code moved from the JParseCode library into this one.
* Includes:
  * twg2.text.tokenizer - previously named twg2.parser.text in JParseCode
    * (CharConditions, CharConditionPipe, StringConditions, CharParserFactory, and other classes)
  * twg2.text.tokenizer.analytics - tokenizer stats tracking
    * ParserAction - previously nested in TokenizeStepDetails from JParserCode library
	* TypedLogger - a logger like interface meant to be a parent interface of TokenizeStepDetails and other parser analytic loggers in future
  * twg2.text.tokenizer.test - JUnit tests for twg2.text.tokenizer from JParseCode
