Syntactic sugar to instantiate Java beans with a very Scala-friendly syntax. 

The following expression:
  
    import scalaxy.beans
    
    beans.create[MyBean](
      foo = 10, 
      bar = 12
    )
    
Gets replaced (and type-checked) at compile time by:
  
    {
      val bean = new MyBean
      bean.setFoo(10)
      bean.setBar(12)
      bean
    }
    
Doesn't bring any runtime dependency (macro is self-erasing).
Don't expect code completion from your IDE as of yet.
