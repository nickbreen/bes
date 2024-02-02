A Bazel Build Event Service.


Build and run the BES.
```shell
bazel run bes file:$PWD/src/test/resources/jnl.bin
```

Re-build forwarding to the BES.
```shell
bazel build --bes_backend=grpc://localhost:8888 --bes_results_url=http://localhost:8080 ...
```

Generate test fixtures.
```shell
bazel build --config bes ...
```

Run tests.
```shell
bazel test --test_output all ...
```