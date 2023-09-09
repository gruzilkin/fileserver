system design excercise for a fileserver that's backed by a distributed hashtable based block storage

cassandra takes some time to start

upload a file:
curl -F -file=@filename http://localhost:8080/file 
preferably an image or something that a browser can open

open a file:
use browser to open http://localhost:8080/file/id

traces can be seen in jaeger:
http://localhost:16686/