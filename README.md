This project is a quick hack to try and provide a tag cloud of Scala technologies to be used at Scala eXchange 2015. 

It is far from being production ready (so far that it should probably be re-written if that was the case).

It uses the [D3 Word Cloud JavaScript library](https://github.com/jasondavies/d3-cloud/). Since version 1.2.1 is not available on [WebJars](http://www.webjars.org/), this is how to install it in the local Maven repository:

```
git clone -b v1.2.1 https://github.com/paoloambrosio/d3-cloud.git; cd d3-cloud; mvn install
```

To run the server on port 9000:

```
sbt server/run
```

The tag cloud is in the root path, the voting page is `/vote`.
