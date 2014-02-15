
repositories.remote << 'http://repo1.maven.org/maven2'

THIS_VERSION = "0.1.0-SNAPSHOT"

define 'proctour' do
  project.version = THIS_VERSION
  compile.with 'com.google.guava:guava:jar:16.0.1', 'log4j:log4j:jar:1.2.16'
  package :jar
end
