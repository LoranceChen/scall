package lorance.scall

trait Key
case class Password(value: String) extends Key
case class IdentityFile(path: String) extends Key
case class NonKey() extends Key

case class Auth(host: String, name: Option[String], port: Int, key: Key)

case class Cmd(content: String)

case class Error(code: Int, msg: String)
class ShellLock //Shell public method should be mutex

case class Config(connectTimeout: Int, //second
                  serverAliveInterval: Int, //half test value
                  serverAliveCountMax: Int //recommend this value > 1, 确保最少的等待时间
                 )