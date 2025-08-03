A Bazel Build Event Service.

[![Bazel CI](https://github.com/nickbreen/bes/actions/workflows/bazel.yml/badge.svg)](https://github.com/nickbreen/bes/actions/workflows/bazel.yml)

---

Run tests.
```shell
bazel test --test_output all ...
```

Build and run the server (`bes`) and generate binary, json, and text journals.
```shell
truncate --size 0 src/test-support/resources/jnl.{bin,jsonl,text}
bazel run bes -- \
      --binary-journal $PWD/src/test-support/resources/jnl.bin \
      --json-journal $PWD/src/test-support/resources/jnl.jsonl \
      --text-journal $PWD/src/test-support/resources/jnl.text &
sleep 2
bazel build --bes_backend=grpc://localhost:8888 ...
sleep 2
kill -2 %1
```

Simulate using the client (`bec`) and server (`bes`).
```shell
truncate --size 0 /tmp/journal.txt
bazel run bes -- --text-journal /tmp/journal.txt &
sleep 2
bazel run bec grpc://localhost:8888 < src/test-support/resources/jnl.bin
sleep 2
kill -2 %1
diff -yW$COLUMNS --suppress-common-lines src/test-support/resources/jnl.text /tmp/journal.txt
```

Regenerate test fixtures.
```shell
bazel build --config bes ...
```
