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
    )* EOF
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
    (textChunk | block)*
    (closeDelim = TEXT_CLOSE | closeDelim = WTEXT_CLOSE | '|' closeDelim = TEXT_CLOSE | '/' closeDelim = TEXT_CLOSE | '|' closeDelim = WTEXT_CLOSE | '/' closeDelim = WTEXT_CLOSE) 
    ;

textChunk
    : TEXT_CHUNK
    ;

literalBlock
    : (LITERAL_OPEN | LITERAL_REOPEN) LITERAL_BODY? LITERAL_CLOSE
    ;

topDefinition
    : doc = DOC_COMMENT? keep = topKeepPrefix? access = PUBLIC? dur = topDurability?
    ( collectionElementDefinition
    | collectionDefinition
    | namedElementDefinition
    | blockDefinition
    )
    ;

definition
    : doc = DOC_COMMENT? keep = keepPrefix? access = LOCAL? dur = durability?
    ( collectionElementDefinition
    | collectionDefinition
    | namedElementDefinition
    | blockDefinition
    )
    ;

topKeepPrefix
    : KEEP keepAs? keepIn SEMICOLON
    ;

keepPrefix
    : KEEP keepAs? keepIn? SEMICOLON
    ;

keepAs
    : AS identifier
    ;
    
keepIn
    : IN instantiation
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


namedElementDefinition
    : simpleType? identifier params? ASSIGN expression SEMICOLON?
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
    : ((cond = IF expression) | (cond = (WITH | WITHOUT) identifier)) block (elseIfPart)* (elsePart)?
    ;

elseIfPart
    : doc = DOC_COMMENT? ELSE ((cond = IF expression) | (cond = (WITH | WITHOUT) identifier)) block
    ;

elsePart
    : doc = DOC_COMMENT? ELSE block
    ; 

loop
    : FOR iterator ((connector = AND iterator)* | (connector = OR iterator)*) block
    ;
    
iterator
    : collectionIterator
    | stepIterator
    ;

collectionIterator
    : simpleType? identifier IN expression (where = WHERE expression)? (until = UNTIL expression)?
    ;

stepIterator
    : simpleType? identifier FROM expression ((to = TO | through = THROUGH) expression)? (BY expression)?
    ;

collectionSuffix
    : dim+
    ;
    
multiParams
    : params (COMMA params)+
    ;

collectionDefName
    : collectionType identifier params?
    | simpleType? identifier params? collectionSuffix
    ;
    
blockDefName
    : multiType identifier (params | multiParams)?
    | simpleType? identifier (params | multiParams)?
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

identifier
   : IDENTIFIER
   ;

qualifiedName
    : identifier (DOT identifier)*
    ;

any
    : STAR
    ;
anyany
    : STARSTAR
    ;
        
nameRange
    : identifier (DOT (identifier | any))* (DOT anyany)?
    ;

typeName
    : identifier (DOT identifier)*
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
    
instantiation
    : nameComponent (DOT nameComponent)*
    ;

nameComponent
    : (specialName | identifier) args? (index)*
    ;

expression
    : LPAREN simpleType RPAREN expression                       #TypeExpression
    | LPAREN expression RPAREN                                  #NestedExpression
    | op = (PLUS | MINUS | TILDE | BANG) expression             #UnaryExpression
    | expression op = ISA simpleType                            #IsaExpression
    | expression op = (STAR | SLASH | MOD) expression           #MulDivExpression
    | expression op = (PLUS | MINUS) expression                 #AddSubExpression
    | expression op = (LSHIFT | RUSHIFT | RSHIFT) expression    #ShiftExpression
    | expression op = (LE | GE | LT | GT) expression            #RelExpression
    | expression op = (EQ | NE) expression                      #EqExpression
    | expression op = BITAND expression                         #BitAndExpression
    | expression op = CARET expression                          #BitXorExpression
    | expression op = BITOR expression                          #BitOrExpression
    | expression op = ANDAND expression                         #LogicalAndExpression
    | expression op = OROR expression                           #LogicalOrExpression
    | <assoc = right> expression op = QMARK
       expression COLON expression                              #ChoiceExpression 
    | <assoc = right> identifier op = QQ
       expression COLON expression                              #ChoiceWithExpression 
    | literal                                                   #LiteralExpression
    | instantiation                                             #InstantiationExpression
    ;

