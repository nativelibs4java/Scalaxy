Prequisite:

- Install [paulp's sbt launch script](https://github.com/paulp/sbt-extras)
- Install git.

Test with:

    git clone git://github.com/ochafik/Scalaxy.git
    cd Scalaxy/Fx/Example/Plots
    sbt "~run first.csv second.csv"
    
Then modify the file format by editing `src/main/scala/Contents.scala`.

The plot will be updated every time you close the JavaFX window.
