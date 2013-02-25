# Prequisites

- Install [paulp's sbt launch script](https://github.com/paulp/sbt-extras)
- Install [git](http://git-scm.com/download/) 
- Install [JDK 7 with JavaFX](http://www.oracle.com/technetwork/java/javafx/downloads/index.html), and make sure it's the default version of Java in "Java Preferences":
  ```
    java -version
  ``` 
  Should give a version equal or superior to:
  ```
    java version "1.7.0_13"
    Java(TM) SE Runtime Environment (build 1.7.0_13-b20)
    Java HotSpot(TM) 64-Bit Server VM (build 23.7-b01, mixed mode)
  ```

# Running

You can run the plotting utils with:

    git clone git://github.com/ochafik/Scalaxy.git
    cd Scalaxy/Fx/Example/Plots
    sbt "~run first.csv second.csv"
    
Then modify the file format by editing `src/main/scala/Contents.scala`.

The plot will be updated every time you close the JavaFX window.
