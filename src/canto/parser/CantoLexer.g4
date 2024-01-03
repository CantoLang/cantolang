/* Canto Language Implementation
 * 
 * Lexer for Canto
 *
 * Copyright (c) 2023 by cantolang.org
 * All rights reserved.
 */

// $antlr-format alignTrailingComments true, columnLimit 150, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine true, allowShortBlocksOnASingleLine true, minEmptyLines 0, alignSemicolons ownLine
// $antlr-format alignColons trailing, singleLineOverrulesHangingColon true, alignLexerCommands true, alignLabels true, alignTrailers true

lexer grammar CantoLexer;

@lexer::header {package canto.parser;}

// Keywords
ADOPT        : 'adopt';
AND          : 'and';
AS           : 'as';
BOOLEAN      : 'boolean';
BY           : 'by';
BYTE         : 'byte';
CATCH        : 'catch';
CHAR         : 'char';
CONTAINER    : 'container';
COSMIC       : 'cosmic';
COUNT        : 'count';
CORE         : 'core';
DEF          : 'def';
DOUBLE       : 'double';
DYNAMIC      : 'dynamic';
ELSE         : 'else';
EXTERN       : 'extern';
FLOAT        : 'float';
FOR          : 'for';
FROM         : 'from';
GLOBAL       : 'global';
HERE         : 'here';
IF           : 'if';
IN           : 'in';
IMPORT       : 'import';
INT          : 'int';
ISA          : 'isa';
KEEP         : 'keep';
KEYS         : 'keys';
LOCAL        : 'local';
LONG         : 'long';
NEXT         : 'next';
NUMBER       : 'number';
ON           : 'on';
OR           : 'or';
OWNER        : 'owner';
PUBLIC       : 'public';
REDIRECT     : 'redirect';
SITE         : 'site';
SOURCE       : 'source';
STATIC       : 'static';
STRING       : 'string';
SUB          : 'sub';
SUPER        : 'super';
THIS         : 'this';
THROUGH      : 'through';
TO           : 'to';
TYPE         : 'type';
UNTIL        : 'until';
WHERE        : 'where';
WITH         : 'with';
WITHOUT      : 'without';


// Literals
DECIMAL_LITERAL : ('0' | [1-9] (Digits? | '_'+ Digits)) [lL]?;
HEX_LITERAL     : '0' [xX] [0-9a-fA-F] ([0-9a-fA-F_]* [0-9a-fA-F])? [lL]?;
OCT_LITERAL     : '0' '_'* [0-7] ([0-7_]* [0-7])? [lL]?;
BINARY_LITERAL  : '0' [bB] [01] ([01_]* [01])? [lL]?;

FLOAT_LITERAL:
    (Digits '.' Digits? | '.' Digits) ExponentPart? [fFdD]?
    | Digits (ExponentPart [fFdD]? | [fFdD])
;

HEX_FLOAT_LITERAL: '0' [xX] (HexDigits '.'? | HexDigits? '.' HexDigits) [pP] [+-]? Digits [fFdD]?;

BOOL_LITERAL: 'true' | 'false';

STRING_LITERAL:
    '\'' (~['\\\r\n] | EscapeSequence) '\''
    | '"' (~["\\\r\n] | EscapeSequence)* '"'
;

NULL_LITERAL: 'null';

// Dimensions
EMPTY_ARRAY  : '[]';
STREAM_ARRAY : '[^]';
EMPTY_TABLE  : '{}';

// Empty blocks
EMPTY_BLOCK    : '[/]';
ABSTRACT_BLOCK : '[?]';
EXTERNAL_BLOCK : '[&]';

// Nestable Blocks
TEXT_OPEN     : '[|'   -> pushMode(TEXT_BLOCK_MODE);
WTEXT_OPEN    : '[/'   -> pushMode(TEXT_BLOCK_MODE);
LITERAL_OPEN  : '[``'  -> pushMode(LITERAL_BLOCK_MODE);
CODE_OPEN     : '{='   -> pushMode(DEFAULT_MODE);
CODE_CLOSE    : '=}'   -> popMode;


// Separators
LCURLY        : '{';
RCURLY        : '}';
LCONCURPAREN  : '(+';
RCONCURPAREN  : '+)';
LDYNAMICPAREN : '(:';
RDYNAMICPAREN : ':)';
LPAREN        : '(';
RPAREN        : ')';
LBRACKET      : '[';
RBRACKET      : ']';
SEMICOLON     : ';';
COMMA         : ',';
DOT           : '.';

// Operators
ELLIPSIS  : '...';
RUSHIFT   : '>>>';
LSHIFT    : '<<';
RSHIFT    : '>>';
EQ        : '==';
LE        : '<=';
GE        : '>=';
NE        : '!=';
ANDAND    : '&&';
OROR      : '||';
QQ        : '??';
STARSTAR  : '**';
ASSIGN    : '=';
GT        : '>';
LT        : '<';
BANG      : '!';
TILDE     : '~';
QMARK     : '?';
COLON     : ':';
PLUS      : '+';
MINUS     : '-';
STAR      : '*';
SLASH     : '/';
BITAND    : '&';
BITOR     : '|';
CARET     : '^';
MOD       : '%';
AT        : '@';
HASH      : '#';

// Whitespace and comments
WS                  : [ \t\r\n\u000C]+ -> channel(HIDDEN);
LINE_COMMENT        : '//' ~[\r\n]*    -> channel(HIDDEN);
DOC_COMMENT         : '/*' .*? '*/';
NONDOC_COMMENT_OPEN : '/--'            -> pushMode(NONDOC_COMMENT), channel(HIDDEN);


// Identifiers
IDENTIFIER: Letter LetterOrDigit*;

// Fragment rules

fragment ExponentPart: [eE] [+-]? Digits;

fragment EscapeSequence:
    '\\' 'u005c'? [btnfr"'\\]
    | '\\' 'u005c'? ([0-3]? [0-7])? [0-7]
    | '\\' 'u'+ HexDigit HexDigit HexDigit HexDigit
;

fragment HexDigits: HexDigit ((HexDigit | '_')* HexDigit)?;

fragment HexDigit: [0-9a-fA-F];

fragment Digits: [0-9] ([0-9_]* [0-9])?;

fragment LetterOrDigit: Letter | [0-9];

fragment Letter:
    [a-zA-Z$_]                        // ascii letters plus dollar sign and underscore
    | ~[\u0000-\u007F\uD800-\uDBFF]   // covers all characters above 0x7F which are not a surrogate
    | [\uD800-\uDBFF] [\uDC00-\uDFFF] // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
;

// Text block mode, supports nested blocks
mode TEXT_BLOCK_MODE;
TEXT_CLOSE             : '|]'    -> popMode;
WTEXT_CLOSE            : '/]'    -> popMode;
TEXT_REOPEN            : '[|'    -> pushMode(TEXT_BLOCK_MODE);
WTEXT_REOPEN           : '[/'    -> pushMode(TEXT_BLOCK_MODE);
CODE_REOPEN            : '{='    -> pushMode(DEFAULT_MODE);
LITERAL_REOPEN         : '[``'   -> pushMode(LITERAL_BLOCK_MODE);
TEXT_CHUNK             : SafeText+;


fragment OrphanDelim: '|' | '/' | '[' | '{';

fragment NonDelim: ~[|/[{}\]]+;

fragment SafeDelim:
    ('|' ~[\]])
    | ('/' ~[\]])
    | ('[' ~[|/`])
    | ('[' '`' ~[`])
    | ('{' ~[=])
    ;

fragment SafeText: (NonDelim | SafeDelim) SafeDelim*;

fragment LiteralChunk:
    ~[`]+
    | ('`' ~[`]+)
    | ('`' '`' ~[\]]+)
    ;

// Literal block mode, no nesting
mode LITERAL_BLOCK_MODE;
LITERAL_CLOSE           : '``]'  -> popMode;
LITERAL_BODY            : LiteralChunk+;

// Non-documenting comment mode
mode NONDOC_COMMENT;
CLOSE       : '--/'         -> popMode, channel(HIDDEN);
NONDOC_OPEN : '/--'         -> pushMode(NONDOC_COMMENT), channel(HIDDEN);
IGNORE      : .             -> more;
   
