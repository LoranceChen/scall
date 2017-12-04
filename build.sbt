name := "scall"

version := "0.4.0"

organization := "com.scalachan"

scalaVersion := "2.12.4"
crossScalaVersions := Seq("2.12.4", "2.11.11")

libraryDependencies ++= Seq(
  "com.jcraft" % "jsch" % "0.1.54",
  "io.reactivex" %% "rxscala" % "0.26.5",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "junit" % "junit" % "4.12" % Test,

)

sonatypeProfileName := "com.scalachan"

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}


lazy val commonPublishSettings = Seq (
  pgpSecretRing := file(Path.userHome + "/.sbt/gpg/secring.asc"),
  pgpPublicRing := file(Path.userHome + "/.sbt/gpg/pubring.asc"),
  publishArtifact in (Compile, packageDoc) := true,
  publishArtifact in Test := false,
  publishMavenStyle := true,
  credentials += Credentials(Path.userHome / ".ivy2" / ".new_card"),

)

lazy val root = (project in file(".")).
  settings(commonPublishSettings).
  settings(
    resolvers ++= Seq(
      "main" at "http://repo1.maven.org/maven2",

      //  "Sonatype Nexus" at "http://localhost:7070/nexus/repository/maven-releases/"//"https://oss.sonatype.org/content/repositories/snapshots"
      "Sonatype Nexus" at "https://oss.sonatype.org/content/repositories/snapshots"
    ),
    publishArtifact in (Compile, packageDoc) := true,
    publishArtifact in Test := false
  ).
  settings(
    pomExtra in Global :=
      <url>https://github.com/LoranceChen/scall</url>
        <licenses>
          <license>
            <name>Apache License, Version 2.0</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0</url>
          </license>
        </licenses>
        <scm>
          <url>git@github.com/LoranceChen/scall.git</url>
          <connection>scm:git:git@github.com/LoranceChen/scall.git</connection>
        </scm>
        <developers>
          <developer>
            <id>lorancechen</id>
            <name>UnlimitedCode Inc.</name>
            <url>http://www.scalachan.com/</url>
          </developer>
        </developers>

  )
