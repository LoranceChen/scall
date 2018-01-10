# Scall - introduce SSH shell to Scala
Shell is worse to organize command together. Scall enhance the shell command by make it organizable in Scala environment.

## Usage
`libraryDependencies += "com.scalachan" %% "scall" % "0.6.1"`

## Dependency
- JSch

## Example
[TerminalTest.scala](https://github.com/LoranceChen/scall/blob/master/src/test/scala/lorance/scall/TerminalTest.scala)
[SessionTerminalTest.scala](https://github.com/LoranceChen/scall/blob/master/src/test/scala/lorance/scall/SessionTerminalTest.scala)


## Updates
- version 0.6.1
  - support jsch channel in `SessionTerminal` class