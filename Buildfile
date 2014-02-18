
repositories.remote << 'http://repo1.maven.org/maven2'

THIS_VERSION = "1.0.0"

define 'proctour' do
  project.version = THIS_VERSION
  project.group = 'com.mikehelmick'
  manifest['Copyright'] = 'Mike Helmick (C) 2014'
  
  desc 'The ProcTour distributed algorithm simulator.'
   
  compile.with 'com.google.guava:guava:jar:16.0.1', 'log4j:log4j:jar:1.2.16'
  package :jar
  
  run.using :main => "com.mikehelmick.proc.example.Example"
end
