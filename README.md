A Bazel Build Event Service.


Build and run the BES.
```shell
bazel run bes
```

Re-build forwarding to the BES.
```shell
bazel build --bes_backend=grpc://localhost:8888 ...
```
