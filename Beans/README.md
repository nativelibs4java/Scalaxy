# Scalaxy/Beans

Syntactic sugar to set Java beans properties with a very Scala-friendly syntax.
(does not depend on the rest of Scalaxy)

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
    
Works with all Java beans and doesn't bring any runtime dependency.

Only downside: code completion won't work in IDE (unless someone adds a special case for `Scalaxy/Beans` :-)).

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
    
# Hacking

If you want to build / test / hack on this project:
- Make sure to use [paulp's sbt script](https://github.com/paulp/sbt-extras) with `sbt` 0.12.2+
- Use the following commands to checkout the sources and build the tests continuously: 

    ```
    git clone git://github.com/ochafik/Scalaxy.git
    cd Scalaxy
    sbt "project scalaxy-beans" "; clean ; ~test"
    ```

