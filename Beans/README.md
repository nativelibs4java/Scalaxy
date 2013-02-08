Syntactic sugar to set Java beans properties with a very Scala-friendly syntax. 

The following expression:
  
    import scalaxy.beans
    
    new MyBean().set(foo = 10, bar = 12)
    
Gets replaced (and fully type-checked) at compile time by:
  
    {
      val bean = new MyBean()
      bean.setFoo(10)
      bean.setBar(12)
      bean
    }
    
Doesn't bring any runtime dependency (macro is self-erasing).
Don't expect code completion from your IDE as of yet.
