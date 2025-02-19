/* Canto Language Implementation
 * 
 * Parser for Canto
 *
 * Copyright (c) 2023-2025 by cantolang.org
 * All rights reserved.
 */


// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging

parser grammar CantoParser;

options {
    tokenVocab = CantoLexer;
}

@header {
    package canto.parser;
}

cosmosDefinition
    : COSMOS siteBlock
    ;

globeDefinition
    : GLOBE siteBlock
    ;

compilationUnit
    :
    ( siteDefinition
    | coreDefinition
    | domainDefinition
    | defaultSiteDefinition
    )*
    ;

siteDefinition
    : SITE identifier siteBlock
    ;

coreDefinition
    : CORE siteBlock
    ;

domainDefinition
    : identifier identifier siteBlock
    ;
    
defaultSiteDefinition
    : siteBlock
    ;

siteBlock
    : LCURLY directive* topDefinition* RCURLY
    | CODE_OPEN directive* topDefinition* CODE_CLOSE
    ;

directive
    : doc = DOC_COMMENT?
    ( externDirective
    | adoptDirective
    )
    ;


externDirective
    : EXTERN identifier nameRange
    ;
    
adoptDirective
    : ADOPT nameRange
    ;

block
    : codeBlock
    | textBlock
    | literalBlock
    ;

emptyBlock
    : EMPTY_BLOCK
    ;

abstractBlock
    : ABSTRACT_BLOCK
    ;

externalBlock
    : EXTERNAL_BLOCK
    ;

codeBlock
    : LCURLY (definition | construction)* RCURLY
    | (CODE_OPEN | CODE_REOPEN) (definition | construction)* CODE_CLOSE
    ;

textBlock
    : openDelim = (TEXT_OPEN | WTEXT_OPEN | TEXT_REOPEN | WTEXT_REOPEN)
    (TEXT_CHUNK | block)*
    (closeDelim = TEXT_CLOSE | closeDelim = WTEXT_CLOSE | '|' closeDelim = TEXT_CLOSE | '/' closeDelim = TEXT_CLOSE | '|' closeDelim = WTEXT_CLOSE | '/' closeDelim = WTEXT_CLOSE) 
    ;

literalBlock
    : (LITERAL_OPEN | LITERAL_REOPEN) LITERAL_BODY? LITERAL_CLOSE
    ;

topDefinition
    : doc = DOC_COMMENT? pub = PUBLIC? dur = topDurability?
    ( def = collectionElementDefinition
    | def = collectionDefinition
    | def = elementDefinition
    | def = blockDefinition
    )
    ;

definition
    : doc = DOC_COMMENT? loc = LOCAL? dur = durability?
    ( def = collectionElementDefinition
    | def = collectionDefinition
    | def = elementDefinition
    | def = blockDefinition
    )
    ;

topDurability
    : COSMIC
    | GLOBAL
    | STATIC
    | DYNAMIC
    ;

durability
    : STATIC
    | DYNAMIC
    ;

collectionElementDefinition
    : collectionDefName ASSIGN expression SEMICOLON?
    ;
    
collectionDefinition
    : collectionDefName ASSIGN collectionInitBlock
    ;

collectionInitBlock
    : arrayInitBlock
    | tableInitBlock
    ;

arrayInitBlock
    : EMPTY_ARRAY
    | LBRACKET arrayElementList? RBRACKET
    ;

arrayElementList
    : arrayElement (COMMA arrayElement)* COMMA?
    ;

arrayElement
    : expression | collectionInitBlock
    ;

tableInitBlock
    : EMPTY_TABLE
    | LCURLY tableElementList? RCURLY
    ;
 
tableElementList
    : tableElement (COMMA tableElement)* COMMA?
    ;

tableElement
    : expression COLON (expression | collectionInitBlock)
    ;


elementDefinition
    : elementDefName ASSIGN expression SEMICOLON?
    ;

blockDefinition
    : blockDefName (block (CATCH block)? | emptyBlock | abstractBlock | externalBlock)
    ;

construction
    : doc = DOC_COMMENT?
    ( expression SEMICOLON
    | block SEMICOLON?
    | conditional
    | loop
    )
    ;
    
conditional
    : condType = (IF | WITH | WITHOUT) expression block (elseIfPart)* (elsePart)?
    ;

elseIfPart
    : doc = DOC_COMMENT? ELSE condType = (IF | WITH | WITHOUT) expression block
    ;

elsePart
    : doc = DOC_COMMENT? ELSE block
    ; 

loop
    : FOR iterator (connector = (AND | OR) iterator)* block
    ;
    
iterator
    : collectionIterator
    | stepIterator
    ;

collectionIterator
    : simpleType? identifier IN expression (where = WHERE expression)? (until = UNTIL expression)?
    ;

stepIterator
    : simpleType? identifier FROM expression (TO expression)? (BY expression)?
    ;

collectionSuffix
    : dim+
    ;
    
paramSuffix
    : params+
    ;

multiParamSuffix
    : params (COMMA params)+
    ;

collectionDefName
    : collectionType identifier paramSuffix?
    | simpleType? identifier paramSuffix? collectionSuffix
    ;
    
elementDefName
    : simpleType? identifier paramSuffix? 
    ;

blockDefName
    : multiType identifier (paramSuffix | multiParamSuffix)?
    | simpleType? identifier (paramSuffix | multiParamSuffix)?
    ;

simpleType
    : qualifiedName
    | identifier
    | BOOLEAN
    | BYTE
    | CHAR
    | DOUBLE
    | FLOAT
    | INT
    | LONG
    | NUMBER
    | STRING
    ;

multiType
    : simpleType (COMMA simpleType)+
    ;

collectionType
    : simpleType dim+
    ;

param
    : simpleType? identifier 
    ;

params
    : LPAREN (param (COMMA param)*)? RPAREN
    ;

dim
    : arrayDim
    | tableDim
    ;

arrayDim
    : EMPTY_ARRAY
    | STREAM_ARRAY
    | LBRACKET expression? RBRACKET
    ;

tableDim
    : EMPTY_TABLE
    | LCURLY RCURLY
    ;

index
    : STREAM_ARRAY
    | LBRACKET expression RBRACKET
    ;


specialName
    : CONTAINER
    | CORE
    | COUNT
    | DEF
    | HERE
    | KEYS
    | NEXT
    | OWNER
    | SITE
    | SOURCE
    | SUB
    | SUPER
    | THIS
    | TYPE args?
    ;

name
   : specialName
   | qualifiedName
   | identifier
   ;

identifier
   : IDENTIFIER
   ;

qualifiedName
    : IDENTIFIER (DOT IDENTIFIER)+
    ;
    
nameRange
    : IDENTIFIER (DOT (IDENTIFIER | STAR))* (DOT STARSTAR)?
    ;

args
    : LPAREN (expression (COMMA expression)*)? RPAREN
    ;    
    
literal
    : integerLiteral
    | floatLiteral
    | BOOL_LITERAL
    | STRING_LITERAL
    | NULL_LITERAL
    ;
    
integerLiteral
    : DECIMAL_LITERAL
    | HEX_LITERAL
    | OCT_LITERAL
    | BINARY_LITERAL
    ;

floatLiteral
    : FLOAT_LITERAL
    | HEX_FLOAT_LITERAL
    ;
    
primary
    : LPAREN expression RPAREN
    | instantiation
    | literal
    ;

instantiation
    : name (index)* (args)?
    ;

expression
    : primary
    | unaryExpression
    | castExpression
    | isaExpression
    | binaryExpression
    | trinaryExpression
    ;

unaryExpression    
    : prefix = (PLUS | MINUS | TILDE | BANG) expression
    ;

castExpression
    : LPAREN simpleType RPAREN expression
    ;
    
isaExpression
    :| expression ISA simpleType
    ;

binaryExpression
    : expression bop = (STAR | SLASH | MOD) expression
    | expression bop = (PLUS | MINUS) expression
    | expression bop = (LSHIFT | RUSHIFT | RSHIFT) expression
    | expression bop = (LE | GE | LT | GT) expression
    | expression bop = (EQ | NE) expression
    | expression bop = BITAND expression
    | expression bop = CARET expression
    | expression bop = BITOR expression
    | expression bop = ANDAND expression
    | expression bop = OROR expression
    ;

trinaryExpression
    : <assoc = right> expression top = (QMARK | QQ) expression COLON expression
    ;

    
