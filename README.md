# VARDT : An Automated Fault Localization Tool

This is the codebase of the bug detector VARDT.

## Usage

### Configuration

Change the following configurations according to your system.

* `/src/fl/utils/Constant.java` 
  * D4J_HOME
  * ALL_TESTS_AFTER_TP : folder contains all_tests_name after test purification
  * FAIL_TESTS : folder contains failed_tests_name after test purification

* `/src/pda/core/dependency/Config.java`
  * graphPath : output path for generated PDG

* `/resources/conf/system.properties`

### Running

To run the purification,

    $> unzip /data/purification.zip
    $> cd /data/purification
    $> java -jar SimFix-1.0-SNAPSHOT-runnable.jar -home ${bugsrc_base} -proj ${pro} -id ${i}

Before the instrumentation, `ALL_TESTS_AFTER_TP/` and `FAIL_TESTS/` need to be prepared.

To run the instrumentation,

    $> java fl.Runner ${project_path} ${project_id} ${bug_id} ${slice_path} PDAtrace ${TOP_N}

To run the slicing,

    $> java pda.core.slice.SlicerMain ${project_path} ${project_id} ${bug_id} ${trace_path} ${trace_line_path} ${output_path}

To run the tree generation,

    $> java fl.weka.IntraGenTree ${collect_values_dir} ${output_path} ${project_id} ${bug_id} ${project_path} ${trace_path} ${linescore_base}


