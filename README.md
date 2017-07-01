# compiler
## Description
This is a compiler for a small programming language (LL(1) grammar). The compiler is written in Java. The target language is java byte code and the ASM byte code framework is used for code generation. Implemented a LeBlanc-Cook symbol table. The lexical structure, context-free grammar, abstract syntax of the programming language are provided in the ProgrammingLanguage.txt file

## Requirements
This project requires asm and asm-util jars to generate the bytecode. They are to be kept in the classpath of the project. They can be downloaded from the links provided below <br/>
https://mvnrepository.com/artifact/org.ow2.asm/asm/5.1 (Links to an external site.) <br/>
https://mvnrepository.com/artifact/org.ow2.asm/asm-util/5.1 (Links to an external site.)
