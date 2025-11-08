Grammar for the *Mica* language:

```
symbol                  = [a-zA-Z_] [a-zA-Z0-9_]*
type                    = 'int' | 'real' | 'char' | 'string' | 'bool'
                            | 'intRange' | 'realRange' | 'any'
                            | 'type'
                            | ( '[' type ']' ) | ( '{' type '}' )
                            | ( '{' type ':' type '}' )

boolLiteral             = 'true' | 'false'
charLiteral             = '\'' . '\''
stringLiteral           = '"' ( interpolatedExpression | .* )* '"'
interpolatedExpression  = '$(' expression ')'
intLiteral              = [0-9] [0-9_]*
realLiteral             = intLiteral '.' intLiteral
hexLiteral              = '0x' [0-9a-fA-F]+
binaryLiteral           = '0b' [0-1]+
exponentLiteral         = ( intLiteral | realLiteral ) 'e' '-'? intLiteral
intRangeLiteral         = intLiteral '..' intLiteral
realRangeLiteral        = realLiteral '..' realLiteral
arrayLiteral            = '[' ( expression ( ',' expression )* ','? )? ']'
setLiteral              = '{' ( expression ( ',' expression )* ','? )? '}'
mapLiteral              = '{' ( ( expression ':' expression ) ( ',' ( expression ':' expression ) )* ','? )? '}'

functionCallExpression  = symbol ( '@' type ) '(' ( expression ( ',' expression )* ','? )? ')'

expressionBlockBody     = expressionStatement | ( '{' statement* expressionStatement '}' )
ifConditionExpression   = 'if' expression expressionBlockBody ( 'else if' expressionBlockBody )? 'else' expressionBlockBody
affixationExpression    = ( symbol ( '++' | '--' ) ) | ( ( '++' | '--' ) symbol )
memberAccessExpression  = expression '.' ( symbol | functionCallExpression )
typeCoercionExpression  = expression 'as' type

expression              = boolLiteral | charLiteral | stringLiteral
                            | intLiteral | realLiteral | hexLiteral
                            | binaryLiteral | exponentLiteral | intRangeLiteral
                            | realRangeLiteral | arrayLiteral | setLiteral | mapLiteral
                            | functionCallExpression
                            | ifConditionExpression
                            | affixationExpression
                            | ( expression '[' expression ']' )
                            | ( '(' expression ')' )
                            | ( ( '-' | '+' | '!' ) expression )
                            | ( expression ( '+' | '-' | '*' | '/' | '^' | '&' | '|' ) expression )

declarationStatement    = symbol ( ':' type )? '=' expression
assignmentStatement     = symbol ( '=' | '+=' | '-=' | '*=' | '/=' | '^=' | '&=' | '|=' ) expression
returnStatement         = 'return' expression?
breakStatement          = 'break'

blockBody               = statement | ( '{' statement* '}' )

ifConditionStatement    = 'if' expression blockBody ( 'else if' blockBody )? ( 'else' blockBody )?
loopIfStatement         = 'loop' ( 'if' expression )? blockBody ( 'else' blockBody )?
loopInStatement         = 'loop' symbol ( ',' symbol )? 'in' expression blockBody
expressionStatement     = expression
userInputStatement      = '<' symbol
userOutputStatement     = '>' expression

statement               = declarationStatement | assignmentStatement
                            | returnStatement | breakStatement
                            | ifConditionStatement | loopIfStatement | loopInStatement
                            | expressionStatement
                            | userInputStatement | userOutputStatement

functionDeclaration     = symbol ( '@' type ) '(' ( symbol ':' type ( '=' expression )? ( ',' symbol ':' type ( '=' expression )? ','? )* )? ')' ( ':' type )? '{' statement* '}'
typeDeclaration         = type symbol '{' ( symbol ':' type )* functionDeclaration* '}'

rootLevelStatement      = statement | functionDeclaration | typeDeclaration
```
