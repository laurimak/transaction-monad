# transaction-monad

Monadic(ish) transaction management in Java 8.

## Benefits over declarative transaction management
* The service layer is no longer the transaction boundary allowing you to add operations to the transaction outside the service.
* Performing operations transactionally no longer implies dependencies between services, instead transactions can be combined by calling `flatMap` on the returned `Transaction` object.
* If an object returned by a transactional service method needs to projected to a different one, this can be done within the same transaction by simply mapping the returned `Transaction` object.
* One could give up the service layer all together and build completely different architectures.

## Example and motivation
In a typical three-layer Java webapp you write something like to following to perform two db operations in transaction.

```java
//Service2.java
@Service
public class Service2 {
  @Inject
  private Dao dao;

  @Transactional
  public String getFoo() {
    return dao.getFoo();
  }
}

//Service1.java
@Service
public class Service1 {
  @Inject
  private Dao dao;

  @Inject
  private Service2 service2;

  @Transactional
  public void putFoo() {
    String foo = service2.getFoo();
    dao.put(foo);
  }
}

//MyController.java
@Controller
public class MyController {

  @Inject
  private Service1 service1;

  @RequestMapping("/foo")
  public void foo() {
    service1.putFoo();
  }
}
```

In the "monadic" model you could do away with the dependency between `Service1` and `Service2` and combine the two operations into on transaction one the controller level.

```java
//Service2.java
@Service
public class Service2 {
  @Inject
  private Dao dao;

  @Transactional
  public Transaction<String> getFoo() {
    return dao.getFoo();
  }
}

//Service1.java
@Service
public class Service1 {
  @Inject
  private Dao dao;

  @Transactional
  public Transaction<Void> putFoo(String foo) {
    return dao.put(foo);
  }
}

//MyController.java
@Controller
public class MyController {

  @Inject
  private Service1 service1;

  @Inject
  private Service2 service2;

  @RequestMapping("/foo")
  public void foo() {
    service2.getFoo()
      .flatMap(foo -> service1.putFoo(foo))
      .commit();
  }
}
```
