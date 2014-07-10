Nice.javaProject

javaVersion := "1.8"

organization := "bio4j"

name := "bio4j"

description := "Bio4j abstract model"

libraryDependencies += "ohnosequences" % "typed-graphs" % "0.3.0-SNAPSHOT"

bucketSuffix := "era7.com"

dependencyOverrides ++= Set(
	"net.sf.opencsv" % "opencsv" % "2.3"
)
