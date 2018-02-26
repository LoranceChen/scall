# Scall - introduce SSH shell to Scala
Shell is worse to organize command together. Scall enhance the shell command by make it organizable in Scala environment.

## Usage
`libraryDependencies += "com.scalachan" %% "scall" % "0.7.2"`

## Dependency
- JSch
- SSHJ

## Example
- [TerminalTest.scala](https://github.com/LoranceChen/scall/blob/master/src/test/scala/lorance/scall/TerminalTest.scala)
- [SessionTerminalTest.scala](https://github.com/LoranceChen/scall/blob/master/src/test/scala/lorance/scall/SessionTerminalTest.scala)
- [SessionPoolTest.scala](https://github.com/LoranceChen/scall/blob/master/src/test/scala/lorance/scall/SessionPoolTest.scala)


## Updates
- version 0.7.2
  - append heart beat fot SessionTerminal
- version 0.7.1
  - fix only accept some keys with sshj SessionPool
- version 0.7.0
  - support concurrent session execute pool by SSHJ
- version 0.6.1
  - support jsch channel in `SessionTerminal` class
