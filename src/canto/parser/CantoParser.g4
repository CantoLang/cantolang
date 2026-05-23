/* Canto Language Implementation
 * 
 * Parser for Canto
 *
 * Copyright (c) 2023-2026 by cantolang.org
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
    ( doc = DOC_COMMENT?
    ( cosmosDefinition
    | globeDefinition
    | siteDefinition
    | coreDefinition
    | domainDefinition
    | defaultSiteDefinition
    ))* EOF
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
    | EMPTY_TABLE
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
    : codeBlock catchBlock?
    | textBlock catchBlock?
    | literalBlock
    | emptyBlock
    ;

catchBlock
    : CATCH block
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
    : LCURLY (definition | construction)* DOC_COMMENT? RCURLY
    | (CODE_OPEN | CODE_REOPEN) (definition | construction)* DOC_COMMENT? CODE_CLOSE
    | EMPTY_TABLE
    ;

textBlock
    : openDelim = (TEXT_OPEN | WTEXT_OPEN | TEXT_REOPEN | WTEXT_REOPEN)
    (textChunk | block)*
    (closeDelim = TEXT_CLOSE | closeDelim = WTEXT_CLOSE | '|' closeDelim = TEXT_CLOSE | '/' closeDelim = TEXT_CLOSE | '|' closeDelim = WTEXT_CLOSE | '/' closeDelim = WTEXT_CLOSE) 
    ;

unnestableTextBlock
    : openDelim = (TEXT_OPEN | WTEXT_OPEN | TEXT_REOPEN | WTEXT_REOPEN)
    textChunk
    (closeDelim = TEXT_CLOSE | closeDelim = WTEXT_CLOSE | '|' closeDelim = TEXT_CLOSE | '/' closeDelim = TEXT_CLOSE | '|' closeDelim = WTEXT_CLOSE | '/' closeDelim = WTEXT_CLOSE) 
    ;

textChunk
    : TEXT_CHUNK
    ;

literalBlock
    : (LITERAL_OPEN | LITERAL_REOPEN) body = LITERAL_BODY? LITERAL_CLOSE
    ;

topDefinition
    : doc = DOC_COMMENT? keep = topKeepPrefix? access = PUBLIC? dur = topDurability?
    ( collectionElementDefinition
    | collectionDefinition
    | externalCollectionDefinition
    | namedElementDefinition
    | blockDefinition
    | externalDefinition
    )
    ;

definition
    : doc = DOC_COMMENT? keep = keepPrefix? access = LOCAL? dur = durability?
    ( collectionElementDefinition
    | collectionDefinition
    | externalCollectionDefinition
    | dimlessCollectionDefinition
    | namedElementDefinition
    | blockDefinition
    | externalDefinition
    )
    ;

topKeepPrefix
    : KEEP keepAs? keepIn COLON
    ;

keepPrefix
    : KEEP keepAs? keepIn? COLON
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
    
externalCollectionDefinition
    : collectionDefName ASSIGN? externalBlock
    ;

collectionDefinition
    : collectionDefName ASSIGN collectionInitBlock
    ;
    
dimlessCollectionDefinition
    : dimlessCollectionName ASSIGN collectionInitBlock
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
    : expression | arrayDynamicInitExpression | collectionInitBlock | textBlock | literalBlock
    ;

arrayDynamicInitExpression
    : arrayConditional
    | arrayLoop
    | arrayBlock
    ;

arrayBlock
    : CODE_OPEN arrayElementList CODE_CLOSE
    | LCURLY arrayElementList RCURLY
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
    | tableDynamicInitExpression
    ;

tableDynamicInitExpression
    : tableConditional
    | tableLoop
    | tableBlock
    ;

tableBlock
    : CODE_OPEN tableElementList CODE_CLOSE
    | LCURLY tableElementList RCURLY
    ;
    
namedElementDefinition
    : defName ASSIGN expression SEMICOLON?
    ;

blockDefinition
    : defName (block | abstractBlock)
    ;

externalDefinition
    : defName externalBlock
    ;

construction
    : doc = DOC_COMMENT?
    ( expression SEMICOLON
    | block SEMICOLON?
    | conditional
    | loop
    | redirect
    )
    ;
    
conditional
    : ((cond = IF expression) | (cond = (WITH | WITHOUT) (identifier | LPAREN identifier RPAREN))) block (elseIfPart)* (elsePart)?
    ;

elseIfPart
    : doc = DOC_COMMENT? ELSE ((cond = IF expression) | (cond = (WITH | WITHOUT) (identifier | LPAREN identifier RPAREN))) block
    ;

elsePart
    : doc = DOC_COMMENT? ELSE block
    ; 

arrayConditional
    : ((cond = IF expression) | (cond = (WITH | WITHOUT) (identifier | LPAREN identifier RPAREN))) arrayBlock (arrayElseIfPart)* (arrayElsePart)?
    ;

arrayElseIfPart
    : doc = DOC_COMMENT? ELSE ((cond = IF expression) | (cond = (WITH | WITHOUT) (identifier | LPAREN identifier RPAREN))) arrayBlock
    ;

arrayElsePart
    : doc = DOC_COMMENT? ELSE arrayBlock
    ; 

tableConditional
    : ((cond = IF expression) | (cond = (WITH | WITHOUT) (identifier | LPAREN identifier RPAREN))) tableBlock (tableElseIfPart)* (tableElsePart)?
    ;

tableElseIfPart
    : doc = DOC_COMMENT? ELSE ((cond = IF expression) | (cond = (WITH | WITHOUT) (identifier | LPAREN identifier RPAREN))) tableBlock
    ;

tableElsePart
    : doc = DOC_COMMENT? ELSE tableBlock
    ; 

loop
    : FOR iterator ((connector = AND iterator)* | (connector = OR iterator)*) block
    ;
    
arrayLoop
    : FOR iterator ((connector = AND iterator)* | (connector = OR iterator)*) arrayBlock
    ;
    
tableLoop
    : FOR iterator ((connector = AND iterator)* | (connector = OR iterator)*) tableBlock
    ;
    
iterator
    : collectionIterator
    | stepIterator
    ;

collectionIterator
    : simpleType? identifier IN expression (where = WHERE expression)? (until = UNTIL expression)?
    ;

stepIterator
    : simpleType? identifier FROM expression ((to = TO | through = THROUGH) expression)? (BY expression)? (where = WHERE expression)? (until = UNTIL expression)?
    ;

redirect
    : REDIRECT instantiation
    ;

collectionSuffix
    : dim+
    ;
    
multiParams
    : params (COMMA params)+
    ;

collectionDefName
    : collectionType identifier (multiParams | params)? collectionSuffix?
    | typeWithArgs identifier (multiParams | params)? collectionSuffix
    | simpleType? identifier (multiParams | params)? collectionSuffix
    ;

dimlessCollectionName
    : (typeWithArgs | simpleType) identifier (multiParams | params)?
    ;
    
defName
    : multiType identifier (multiParams | params)?
    | typeWithArgs identifier (multiParams | params)?
    | simpleType? identifier (multiParams | params)?
    ;

typeWithArgs
    : (qualifiedName | identifier) typeArgs
    ;

simpleType
    : BOOLEAN
    | BYTE
    | CHAR
    | DOUBLE
    | FLOAT
    | INT
    | LONG
    | NUMBER
    | STRING
    | qualifiedName
    | identifier
    ;

multiType
    : (typeWithArgs | simpleType) (COMMA (typeWithArgs | simpleType))+
    ;

collectionType
    : simpleType dim+
    ;

param
    : (collectionType | simpleType)? identifier dim*
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
    | TYPE
    ;

identifier
   : IDENTIFIER | BACK_QUOTE_IDENTIFIER
   ;

qualifiedName
    : identifier (DOT identifier)+
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

complexName
    : identifier (DOT identifier)*
    ;

args
    : LPAREN (expression (COMMA expression)*)? RPAREN
    ;    
    
typeArgs
    : LPAREN (any | (expression (COMMA expression)*))? RPAREN
    ;    
    
dynamicArgs
    : LDYNAMICPAREN (expression (COMMA expression)*)? RDYNAMICPAREN
    ;    
    
literal
    : integerLiteral
    | floatLiteral
    | BOOL_LITERAL
    | STRING_LITERAL
    | NULL_LITERAL
    | unnestableTextBlock
    | literalBlock
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
    : (specialName | identifier) (args | dynamicArgs)? (index)*
    ;

expression
    : LPAREN simpleType RPAREN expression                       #TypeExpression
    | LPAREN expression RPAREN                                  #NestedExpression
    | op = (PLUS | MINUS | TILDE | BANG) expression             #UnaryExpression
    | expression op = ISA simpleType                            #IsaExpression
    | expression op = STARSTAR expression                       #PowerExpression
    | expression op = (STAR | SLASH | MOD) expression           #MulDivExpression
    | expression op = (PLUS | MINUS) expression                 #AddSubExpression
    | expression op = (LSHIFT | RUSHIFT | RSHIFT) expression    #ShiftExpression
    | expression op = BITAND expression                         #BitAndExpression
    | expression op = CARET expression                          #BitXorExpression
    | expression op = BITOR expression                          #BitOrExpression
    | expression op = (LE | GE | LT | GT) expression            #RelExpression
    | expression op = (EQ | NE) expression                      #EqExpression
    | expression op = ANDAND expression                         #LogicalAndExpression
    | expression op = OROR expression                           #LogicalOrExpression
    | <assoc = right> expression op = QMARK
       expression COLON expression                              #ChoiceExpression 
    | <assoc = right> identifier op = QQ
       expression COLON expression                              #ChoiceWithExpression 
    | literal                                                   #LiteralExpression
    | instantiation                                             #InstantiationExpression
    ;

