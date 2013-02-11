Syntactic sugar to set Java beans properties with a very Scala-friendly syntax. 

The following expression:
```scala
    import scalaxy.beans._
    
    new MyBean().set(foo = 10, bar = 12)
```
Gets replaced (and fully type-checked) at compile time by:
```scala
{
  val bean = new MyBean()
  bean.setFoo(10)
  bean.setBar(12)
  bean
}
```
    
Doesn't bring any runtime dependency (macro is self-erasing).
Don't expect code completion from your IDE as of yet.

# Usage

If you're using `sbt` 0.12.2+, just put the following lines in `build.sbt`:
```scala
    // Only works with 2.10.0+
    scalaVersion := "2.10.0"
    
    // Dependency at compilation-time only (not at runtime).
    libraryDependencies += "com.nativelibs4java" %% "scalaxy-beans" % "0.3-SNAPSHOT" % "provided"
    
    // Scalaxy/Beans snapshots are published on the Sonatype repository.
    resolvers += Resolver.sonatypeRepo("snapshots")
```
