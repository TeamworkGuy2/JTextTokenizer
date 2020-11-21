JTextTokenizer
==============

`CharParser` and `CharParserFactory` implementations ([jtext-parser](https://github.com/TeamworkGuy2/JTextParser) library) for text tokenization.
These classes contain the logic for handling greedy and non-greedy matching, nested parsers, and collections of optional and required parsers.

* __CharConditions__ and __StringConditions__: Subclasses: `Start`, `End`, and `Literal`.<br>
`Start` and `Literal` are the same and check for begin matching immediately, once a mismatching character is encountered, they fail. `End` parses non-greedily and keep searching for a sequence of characters, restarting whenever a partial match fails. These CharParser implementations are the basis for all other conditions.

* __CharCompoundConditions__: Subclasses: `StartFilter`, `EndFilter`, `Filter`.<br>
Collections of `CharParser`s which provide the same starts-with, ends-with, and exact matching logic as string and char conditions. This allows for char and string conditions to be combined and nested.

* __CharConditionPipe__: Subclasses: `AcceptMultiple`, `AllRequired`, `AllRequiredPlain` `OptionalSuffix`, `OptionalSuffixAny`, and `RepeatableSeparator`.<br>
These are `CharParser` implementations similar to `CharCompoundConditions`.  They provide the ability to chain conditions together.  For example a RepeatableSeparator condition could match the string `[10, 20, 30, 40, 50]` by using a numeric parser for the numbers and a string parser for the comma and space.

* __StringBoundedParserBuilder__ and __StringParserBuilder__: `CharParserFactory` builders, they provide methods for adding string/char start, end, and literal matchers as well as `CharParser` conditions.

* __CharParserFactory__ and __CharParserMatchableFactory__: interface and implementations for managing a set of `CharParserPredicate`s and a compound `CharParser` (i.e. with an `isMatch()` and `createParser()` method).

* __CharMultiConditionParser__: the final bit of logic that takes a set of `CharParserFactory` instances and a set of `TextConsumer` functions to process individual text characters via the `parse()` method.  Checks for completed/failed parsers after each character is processed, handles retrieving completed text buffers from parsers and passing that text to the corresponding `TextConsumer`.
