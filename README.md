# VARDT : An Automated Fault Localization Tool

This is the codebase of the bug detector VARDT. Please feel free to contact us, if you have any questions.

## Usage

You can run VARDT through the [docker image](https://hub.docker.com/repository/docker/anonymous0901/vardt) directly. All environments needed have been set up in the docker. Here is an example. Results are generated to `/home/results/`.

```shell
$> bash
$> cd /home
$> python3 script.py Lang 28
```

### Repository content

* `src/` contains the source code.
* `res/` and `resources` contain some essential files for running.

In `data/`,
* `final_ranks/` contains the experiment results of final variable rankings.
* `groundtruth/` is the groudtruth we collected to check the results.
* `results/` contains the experiment results of generated trees.
* `statistics/` contains some statistic figures and tables.
* Other zip files are some intermediate results.

### Configure

Setting following configurations is required before executing `mvn package` if you'd like to run it in a local environment.
* Change Defects4J settings.

`$DEBUG=0 => $DEBUG=1 in DEFECTS4J_HOME/framework/core/Constant.pm`

* Run 'defects4j checkout ...' command to get project folder path `PROJECT_ROOT/PROJECT_FOLDER`.

* Package purification folder in `data/purification.tar.gz` and run it using options '-home $PROJECT_ROOT -pro $PRO -id $VERSION'.

* Unzip files in `data/` and change `src/fl/utils/Constant.java` according to your local path.
```java
D4J_HOME = 
ALL_TESTS_AFTER_TP = 
FAIL_TESTS = 
```

* Change the configurations in `/resources/conf/system.properties` according to your system.
```txt
COMMAND.TIMEOUT =
COMMAND.JAVA_HOME =
COMMAND.D4J =
```


* Package tracing module `src/pda/core/trace/` and run it using options 'trace -dir $PROJECT_ROOT -name $PRO -id $VERSION'.
```shell
$> timeout 15m java -jar PDA-1.0-SNAPSHOT-runnable.jar trace -dir /home/d4jsrc -name {pro} -id {version}
```

* Package slicing module `src/pda/core/slice/` and run it using options '$PRO $VERSION'.
```shell
$> java -jar fl-slicer.jar {pro} {version}
```

* Package instrumentation module `src/fl/` and run it using options '-dir $PROJECT_FOLDER -name $PRO -id $VERSION -slice $SLICE_PATH -mode PDAtrace -range 10'.
```shell
$> java -jar fl-runner.jar -dir /home/d4jsrc/{pro}/{pro}_{version}_buggy -name {pro} -id {version} -slice /home/topN_traceLineNo_noOrder/{pro}/{pro}_{version}/traceLineByTopN.txt -mode PDAtrace -range 10
```

* Package tree module `src/fl/weka/` and run it using options '$PRO $VERSION 0.8'.
```shell
$> java -jar fl-gentree.jar {pro} {version} 0.8
```

### Example

Each project folder will be checkout to `d4jsrc/`.

```shell
$> cd /home/d4jsrc/lang/lang_28_buggy
```

Then test purification is executed, after which both original and purified test files will be saved to the corresponding path. `all_tests_afterPurified/` and `failed_tests_afterTP/` list test name after purification, which are required to instrumentation.

Next, a tracing job is applied. Tracing results are saved to `tracing/info/`.

```shell
$> ls /home/tracing/info/lang/lang_28
coveredMethods.txt  failedTest.txt  identifier.txt  original_test.log  trace.out
```

`identifier.txt` contains the mapping between id and identifier.

    0       org.apache.commons.lang3.text.translate.NumericEntityUnescaperTest#void#testSupplementaryUnescaping#?
    1       org.apache.commons.lang3.BooleanUtils#?#BooleanUtils#?
    ...

`coveredMethods.txt` lists the id of all covered methods.

`failedTest.txt` lists the id of all failing tests.

`trace.out` lists the id#line_number for each failing test.

    ---------0---------
    224#54
    ...

Slicing results are saved to `topN_traceLineNo_afterSlicing/lang/lang_28/traceLineByTopN.txt`, in a format like MethodID:MethodLongName:LineNumberList. '?' is used as a spaceholder.

    1.0:org.apache.commons.lang3.text.translate.NumericEntityUnescaper#int#translate#?,CharSequence,int,Writer:?,35,37,38,39,41,42,47,48,49,52,54,57,63,64

Each top-10 method are instrumented and retested. Values collected are saved to `collected_values/lang/lang_28/`.

    testStandaloneAmphersand
    input{PRED}.isNULL-35/25:false
    input{PRED}.TYPE-35/25:java.lang.String
    input{PRED}.length-35/25:11
    ...
    PASS
    ...

Each top-10 method will build its trees separately. Trees generated are saved to `results/`. The first level is matched to the instrumented method id.

```shell
$> ls /home/results/gen_trees/gen_trees-cp_dd0.8_rp_slicing/lang/lang_28
0 1 2
$> ls 0
attrMap  tree  values.csv
```

For each method, 

`attrMap` contains mapping betwwen tree attributes and variable names.

    A2:input{PRED}.isNULL-35/25
    A3:input{PRED}.TYPE-35/25
    A4:input{PRED}.length-35/25
    ...

`tree` contains the generated tree for several rounds. Variables are reranked in the 'Reorder:' part, in which the first number is its suspicious score in this single method.

    Round1
    A88 < 34401 : PASS (8/0)
    A88 >= 34401 : FAIL (1/0)
    ...
    Reorder:
    A88 entityValue-{57/20;63/26} 3.8 0.8 3.04
    ...

Running `/home/code/orderVars.py` to considering all top-10 methods, final score of all variables will be saved to `ordered_vars3.txt`. The first number is its final suspicous score for all top-10 method.

```shell
$> python3 /home/code/orderVars.py lang 28
$> vim /home/results/gen_trees/gen_trees-cp_dd0.8_rp_slicing/lang/lang_28/ordered_vars3.txt
```

    0#entityValue-{57/20;63/26} 1.5673384185480062 3.8 0.8 0.6422285251880866 0.46545182989144973
    0#(isHex)-54/19/5 1.5037173932377093 3.6457513110645907 1.0 0.6422285251880866 0.5386751345948129
    ...

# Docker Link

https://hub.docker.com/repository/docker/anonymous0901/vardt

