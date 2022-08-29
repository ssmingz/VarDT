import os
import re
import shutil

tracelines_base="../topN_traceLineNo"
topN_methods_base="../ochiai_method_results_topN"
collectedvalues_base="../collected_values"

realpath=os.path.realpath(__file__)

id_map = {"compress": [3,4,5,6,9,12,15,41,46],
          "gson": [1,4,5,6,12,15,17],
          "codec": [2,3,4,5,6,7,8,9,10,11],
          "csv": [3,5,8,11,16],
          "lang": [6,7,10,11,12,13,16,17,19,28,44,46,47,52,53,54,55,58,59,61,62,63,64],
          "jacksonxml": [1,2,3,5],
          #"closure": [1,4,5,6,7,8,14,16,19,20,24,29,32,33,35,42,46,50,52,54,56,58,68,71,74,75,76,78,83,86,87,88,90,91,94,96,99,100,104,105,109,113,115,116,117,118,121,122,123,124,126,128,130,131,132,135,136,137,138,140,141,142,143,145,146,147,148,150,151,153,154,155,157,158,162,163,164,165,166,168,169,172,173,174,175],
          "chart": [1,3,4,5,7,8,11,12,17,20,21,22],
          "jacksoncore": [1,2,3,5,6,7,8,10,12,14,15,17,18],
          "jsoup": [3,6,11,12,27,29,30,32,36,41,43,45,46,52,56,58,61,70],
          "jxpath": [2,3,4,6,8,9,20,21],
          "math": [19,23,24,31,32,33,38,42,43,53,60,61,64,69,76,79,85,88,94,96,97,103,105],
          "jacksondatabind": [1,3,4,5,6,8,9,10,11,14,15,18,19,22,24,25,27,29,30,32,35,37,38,45,54,74,88,107],
          "time": [4,5,7,8,10,14,15,16,17,18,23,25],
          "collections": [],
          "cli": [5,8,26,28,29,30,32,39],
          "mockito":[1,2,3,6,11,12,13,17,18,19,21,22,24,25,29,30,34,35,36,37,38]
}


def unit_onlycollect_noslicing(bugs, pro):
    collected_values_dir = f"{collectedvalues_base}/{pro}"
    if not os.path.exists(collected_values_dir):
        os.makedirs(collected_values_dir)
    failed_gen_stdlog = []
    for i in bugs:
        result_path = f"{collected_values_dir}/{pro}_{i}"
        if os.path.exists(result_path):
            os.system(f"rm -r {result_path}")
        os.mkdir(result_path)

        # collect result files
        tmp1 = f"/home/wymspace/d4jsrc/{pro}/{pro}_{i}_buggy/logs/std.log"
        tmp2 = f"/home/wymspace/d4jsrc/{pro}/{pro}_{i}_buggy/instrumented_method_id.txt"
        if not os.path.exists(tmp2):
            print(f"[ERROR] instrumented_method_id.txt not found in : {pro} {i}")
            continue
        if not os.path.exists(tmp1):
            failed_gen_stdlog.append(str(i))
            print(f"[LOG-ERROR] std.log not found in : {pro} {i}")
            continue
        original_result = f"{result_path}/original_delSpace.txt"
        method_id = f"{result_path}/instrumented_method_id.txt"
        shutil.copyfile(tmp1, original_result)
        shutil.copyfile(tmp2, method_id)

        mid_list = []
        with open(method_id, "r") as f:
            for l in f:
                if len(l.strip()) != 0:
                    mid_list.append(l[:l.find(":")])
        split_count = 0
        for mid in mid_list:
            split_count += 1
            values_of_mid = []
            linecount = 0
            cur_mid = ""
            with open(original_result, "r", encoding='utf8') as f:
                buffer = ""
                for l in f:
                    l = l.strip()
                    if len(l) == 0:
                        continue
                    if '#' in l:
                        cur_mid = l[:l.index("#")]
                        if l.startswith(f"{mid}#"):
                            l = l[l.index("#") + 1:]
                            if '-' in l and ':' in l:
                                if buffer == "":
                                    values_of_mid.append(f"{l}")
                                    cur_mid = ""
                            else:
                                # escape chars
                                l = l.replace('\n','\\n')
                                l = l.replace('\t','\\t')
                                l = l.replace('\b','\\b')
                                l = l.replace('\r','\\r')
                                l = l.replace('\f','\\f')
                                buffer += l
                        if '-' in l and ':' in l:
                            cur_mid = ""
                    else:
                        if '-' in l and ':' in l:
                            if buffer != "":
                                buffer += l
                                values_of_mid.append(f"{buffer}")
                            cur_mid = ""
                            buffer = ""
                        else:
                            if buffer != "":
                                # escape chars
                                l = l.replace('\n', '\\n')
                                l = l.replace('\t', '\\t')
                                l = l.replace('\b', '\\b')
                                l = l.replace('\r', '\\r')
                                l = l.replace('\f', '\\f')
                                buffer += l
                            else:
                                if cur_mid == "":
                                    values_of_mid.append(f"{l}")
                                if cur_mid == mid:
                                    values_of_mid.append(f"{l}")
                                    cur_mid = ""

                    linecount += 1
                    #if linecount % 1000 == 0:
                        #print(f"processed {linecount} lines in std.log")

            # write to new file
            print(f"---------------------------- total split : {split_count}/{len(mid_list)} ----------------------------")
            dir = f"{result_path}/{mid}"
            if os.path.exists(dir):
                os.remove(dir)
            os.mkdir(dir)
            mid_result = f"{dir}/std.log"
            if linecount == 0:
                failed_gen_stdlog.append(str(i))
                print(f"[LOG-ERROR] std.log is empty for : {pro} {i}")
                break
            with open(mid_result, "w", encoding='utf-8') as w:
                w.write('\n'.join(values_of_mid))
                print(f"split {mid} original std.log finished : {mid_result}")
                w.close()

        print("split finished")

        print(f"Finished collecting values : {pro} {i}")

    if len(failed_gen_stdlog) == 0:
        failed = "null"
    else:
        failed = ",".join(failed_gen_stdlog)
    print(f"Failed collecting for no std.log: {failed}")
    with open(f"{collected_values_dir}/error.log", "w", encoding='utf-8') as w:
        w.write(f"Failed collecting for no std.log : {failed}")
        w.close()


def unit_onlysplit_noslicing(bugs, pro):
    collected_values_dir = f"{collectedvalues_base}/{pro}"
    if not os.path.exists(collected_values_dir):
        os.makedirs(collected_values_dir)
    failed_gen_stdlog = []
    for i in bugs:
        result_path = f"{collected_values_dir}/{pro}_{i}"

        # collect result files
        original_result = f"{result_path}/original_delSpace.txt"
        method_id = f"{result_path}/instrumented_method_id.txt"
        if not os.path.exists(method_id):
            continue

        mid_list = []
        with open(method_id, "r") as f:
            for l in f:
                if len(l.strip()) != 0:
                    mid_list.append(l[:l.find(":")])
        split_count = 0
        for mid in mid_list:
            split_count += 1
            values_of_mid = []
            linecount = 0
            cur_mid = ""
            with open(original_result, "r") as f:
                buffer = ""
                for l in f:
                    l = l.strip()
                    if len(l) == 0:
                        continue
                    if '#' in l:
                        cur_mid = l[:l.index("#")]
                        if l.startswith(f"{mid}#"):
                            l = l[l.index("#") + 1:]
                            if '-' in l and ':' in l:
                                if buffer == "":
                                    values_of_mid.append(f"{l}")
                                    cur_mid = ""
                            else:
                                # escape chars
                                l = l.replace('\n','\\n')
                                l = l.replace('\t','\\t')
                                l = l.replace('\b','\\b')
                                l = l.replace('\r','\\r')
                                l = l.replace('\f','\\f')
                                buffer += l
                        if '-' in l and ':' in l:
                            cur_mid = ""
                    else:
                        if '-' in l and ':' in l:
                            if buffer != "":
                                buffer += l
                                values_of_mid.append(f"{buffer}")
                            cur_mid = ""
                            buffer = ""
                        else:
                            if buffer != "":
                                # escape chars
                                l = l.replace('\n', '\\n')
                                l = l.replace('\t', '\\t')
                                l = l.replace('\b', '\\b')
                                l = l.replace('\r', '\\r')
                                l = l.replace('\f', '\\f')
                                buffer += l
                            else:
                                if cur_mid == "":
                                    values_of_mid.append(f"{l}")
                                if cur_mid == mid:
                                    values_of_mid.append(f"{l}")
                                    cur_mid = ""

                    linecount += 1
                    #if linecount % 1000 == 0:
                        #print(f"processed {linecount} lines in std.log")

            # write to new file
            print(f"---------------------------- total split : {split_count}/{len(mid_list)} ----------------------------")
            dir = f"{result_path}/{mid}"
            if os.path.exists(dir):
                os.system(f'rm -r {dir}')
            os.mkdir(dir)
            mid_result = f"{dir}/std.log"
            if linecount == 0:
                failed_gen_stdlog.append(str(i))
                print(f"[LOG-ERROR] std.log is empty for : {pro} {i}")
                break
            with open(mid_result, "w", encoding='utf-8') as w:
                w.write('\n'.join(values_of_mid))
                print(f"split {mid} original std.log finished : {mid_result}")
                w.close()

        print("split finished")

        print(f"Finished collecting values : {pro} {i}")

    if len(failed_gen_stdlog) == 0:
        failed = "null"
    else:
        failed = ",".join(failed_gen_stdlog)
    print(f"Failed collecting for no std.log: {failed}")
    with open(f"{collected_values_dir}/error.log", "w", encoding='utf-8') as w:
        w.write(f"Failed collecting for no std.log : {failed}")
        w.close()


def unit_onlycollect_slicing(bugs, pro):
    collected_values_dir = f"{collectedvalues_base}/{pro}"
    if not os.path.exists(collected_values_dir):
        os.makedirs(collected_values_dir)
    failed_gen_stdlog = []
    for i in bugs:
        result_path = f"{collected_values_dir}/{pro}_{i}"

        # collect result files
        original_result = f"{result_path}/original_delSpace.txt"
        method_id = f"{result_path}/instrumented_method_id.txt"
        if not os.path.exists(original_result):
            print(f"There is no original_delSpace.txt for {pro} {i}")
            failed_gen_stdlog.append(str(i))
            continue

        mid_list = []
        with open(method_id, "r") as f:
            for l in f:
                if len(l.strip()) != 0:
                    mid_list.append(l[:l.find(":")])
        split_count = 0
        for mid in mid_list:
            split_count += 1
            values_of_mid = []
            linecount = 0
            with open(original_result, "r") as f:
                for l in f:
                    l = l.strip()
                    if len(l) == 0:
                        continue
                    if '#' in l:
                        if l.startswith(f"{mid}#"):
                            l = l[l.index("#") + 1:]
                            values_of_mid.append(f"{l}")
                    else:
                        values_of_mid.append(f"{l}\n")
                    linecount += 1
                    #if linecount % 1000 == 0:
                        #print(f"processed {linecount} lines in std.log")

            # write to new file
            print(f"---------------------------- total split : {split_count}/{len(mid_list)} ----------------------------")
            dir = f"{result_path}/{mid}"
            if not os.path.exists(dir):
                os.makedirs(dir)
            mid_result = f"{dir}/std_slicing.log"
            if os.path.exists(mid_result):
                os.system(f'rm -f {mid_result}')
            if linecount == 0:
                failed_gen_stdlog.append(str(i))
                print(f"[LOG-ERROR] original_delSpace.txt is empty for : {pro} {i}")
                break
            with open(mid_result, "w", encoding='utf-8') as w:
                w.write('\n'.join(values_of_mid))
                print(f"split {mid} original_delSpace.txt finished : {mid_result}")
                w.close()

        print("split finished")

        print(f"Finished collecting values : {pro} {i}")

    if len(failed_gen_stdlog) == 0:
        failed = "null"
    else:
        failed = ",".join(failed_gen_stdlog)
    print(f"Failed collecting for no std.log: {failed}")
    with open(f"{collected_values_dir}/error.log", "w", encoding='utf-8') as w:
        w.write(f"Failed collecting for no std.log : {failed}")
        w.close()


def unit_onlycollect_noslicing_nosplit(bugs, pro):
    collected_values_dir = f"{collectedvalues_base}/{pro}"
    if not os.path.exists(collected_values_dir):
        os.makedirs(collected_values_dir)
    for i in bugs:
        result_path = f"{collected_values_dir}/{pro}_{i}"
        # collect result files
        old_result = f"{result_path}/original.txt"
        original_result = f"{result_path}/original_delSpace.txt"
        if os.path.exists(original_result):
            os.system(f'rm -f {original_result}')
        os.system(f"cp {old_result} {original_result}")

        values_of_mid = []
        with open(original_result, "r") as f:
            buffer = ""
            for l in f:
                l = l.strip()
                if len(l) == 0:
                    continue
                if '#' in l:
                    if re.match("[0-9][0-9]*#", l) is not None:
                        if '-' in l and ':' in l:
                            if buffer == "":
                                values_of_mid.append(f"{l}")
                        else:
                            # escape chars
                            l = l.replace('\n', '\\n')
                            l = l.replace('\t', '\\t')
                            l = l.replace('\b', '\\b')
                            l = l.replace('\r', '\\r')
                            l = l.replace('\f', '\\f')
                            buffer += l
                else:
                    if '-' in l and ':' in l:
                        if buffer != "":
                            buffer += l
                            values_of_mid.append(f"{buffer}")
                        buffer = ""
                    else:
                        if buffer != "":
                            # escape chars
                            l = l.replace('\n', '\\n')
                            l = l.replace('\t', '\\t')
                            l = l.replace('\b', '\\b')
                            l = l.replace('\r', '\\r')
                            l = l.replace('\f', '\\f')
                            buffer += l
                        else:
                            values_of_mid.append(f"{l}")

        # write to new file
        with open(original_result, "w", encoding='utf-8') as w:
            w.write('\n'.join(values_of_mid))
            w.close()
        print(f"Finished collecting values : {pro} {i}")


if __name__ == '__main__':
    #pros = idmap.keys()
    pros = idmap.keys()
    for pro in pros:
        unit_onlycollect_noslicing(idmap[pro], pro)
        #unit_onlysplit_noslicing(idmap[pro], pro)
        #unit_onlycollect_noslicing_nosplit(bugs,pro)
        #unit_onlycollect_slicing(bugs,pro)