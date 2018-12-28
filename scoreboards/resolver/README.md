Desktop resolver
================

A graphical tool for revealing the results of the final hour from running ACM
ICPC style programming contests. Uses JWGL's OpenGL and GLFW bindings for
hardware accelerated animations.

To replay the final hour of an example contest try running:

```
bazel run scoreboards/resolver -- \
    --url https://www.domjudge.org/demoweb/api/ \
    --groups=Participants \
  resolver
```

If your contest is still frozen, you will need to give credentials so that the
resolver can see the results of pending submissions. You can provide them with
the `--username` and `--password` options (use `--help` to see others):

```
bazel run scoreboards/resolver -- \
    --url https://www.domjudge.org/demoweb/api/ \
    --groups=Participants \
    --username=admin \
    --password=admin \
  resolver
```

![Screenshot of the scoreboard for NWERC 2018](../../docs/images/screenshot-resolver.png)
