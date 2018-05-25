name := "org.softlang.dscor"

scalaVersion := "2.11.11"
resolvers += Resolver.sonatypeRepo("releases")
resolvers += "repo.jenkins-ci.org" at "http://repo.jenkins-ci.org/public"

unmanagedBase <<= baseDirectory { base => base / "libs" }
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.8.7"
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.7"
dependencyOverrides += "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.8.7"

dependencyOverrides += "org.apache.hadoop"%"hadoop-mapreduce-client-core"%"2.7.2"
dependencyOverrides += "org.apache.hadoop"%"hadoop-common"%"2.7.2"
dependencyOverrides += "commons-io"%"commons-io"%"2.4"

libraryDependencies += "org.eclipse.jdt" % "org.eclipse.jdt.core" % "3.12.3"
libraryDependencies += "org.eclipse.platform" % "org.eclipse.equinox.app" % "1.3.400"
libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "2.2.0"
libraryDependencies += "org.apache.spark" % "spark-mllib_2.11" % "2.2.0"
libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "3.8.0"
libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.11.11"
libraryDependencies += "org.scala-lang" % "scala-library" % "2.11.11"
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.6"
libraryDependencies += "org.apache.lucene" % "lucene-analyzers" % "3.6.2"
libraryDependencies += "com.google.code.javaparser" % "javaparser" % "1.0.11"
libraryDependencies += "com.google.guava" % "guava" % "22.0"
libraryDependencies += "org.apache.commons" % "commons-csv" % "1.5"
libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.20.1"
libraryDependencies += "org.jsoup" % "jsoup" % "1.11.2"
libraryDependencies += "org.json4s" % "json4s-native_2.11" % "3.5.2"

libraryDependencies += "org.eclipse.aether" % "aether-api" % "1.1.0"
libraryDependencies += "org.eclipse.aether" % "aether-util" % "1.1.0"
libraryDependencies += "org.eclipse.aether" % "aether-api" % "1.1.0"
libraryDependencies += "org.eclipse.aether" % "aether-util" % "1.1.0"
libraryDependencies += "org.eclipse.aether" % "aether-impl" % "1.1.0"
libraryDependencies += "org.eclipse.aether" % "aether-connector-basic" % "1.1.0"
libraryDependencies += "org.eclipse.aether" % "aether-transport-file" % "1.1.0"
libraryDependencies += "org.eclipse.aether" % "aether-transport-http" % "1.1.0"
libraryDependencies += "org.eclipse.aether" % "aether-transport-wagon" % "1.1.0"
libraryDependencies += "org.eclipse.aether" % "aether-spi" % "1.1.0"
libraryDependencies += "org.eclipse.aether" % "aether-test-util" % "1.1.0"
libraryDependencies += "org.apache.maven" % "maven-aether-provider" % "3.3.9"
libraryDependencies += "org.eclipse.aether" % "aether" % "1.1.0"

//libraryDependencies += "org.apache.bcel" % "bcel" % "6.0"
//slibraryDependencies += "org.ow2.asm" % "asm" % "5.0.3"

//libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.3"
//libraryDependencies += "org.kohsuke" % "github-api" % "1.86"
//libraryDependencies += "org.apache.pdfbox" % "pdfbox" % "2.0.6"
//libraryDependencies += "org.apache.xmlgraphics" % "fop" % "2.1"
//libraryDependencies += "org.apache.avalon.framework" % "avalon-framework-api" % "4.3.1"
//libraryDependencies += "org.apache.avalon.framework" % "avalon-framework-impl" % "4.3.1"
//libraryDependencies += "org.apache.xmlgraphics" % "batik-codec" % "1.9"
//libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.4" % Test
//libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % Test
//libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "4.9.0.201710071750-r"


//libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.6"

