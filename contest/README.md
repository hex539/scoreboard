Contest API
===========

Library for abstracting away the details of loading and parsing a contest. The
API exposes the internal implementation of the contest as a collection of flat
protocol buffer objects.

Loading a contest into memory can be as simple as:

```
ClicsProto.ClicsContest contest = new ContestDownloader(url).fetch();
```

Usually pending and frozen judgements will be hidden behind some kind of
security barrier (commonly HTTP Basic Auth).
[ContestConfig.Source](./proto/config.proto) in [proto/](./proto/) gives a few
ways to configure the client and override automatic detection of API type and
version.
