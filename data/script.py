# purification结果覆盖原test

from curses.ascii import isdigit
import os
import shutil
import collections


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


# check purification used
def check_using_purification(root_path, pro, target):
    used_list = []
    for folder in os.listdir(f'{root_path}/{pro}'):
        if not os.path.isdir(f'{root_path}/{pro}/{folder}'):
            continue
        result = f'{root_path}/{pro}/{folder}/purified_failing_tests.txt'
        flag = False
        with open(result, 'r', encoding='utf-8') as f:
            for line in f:
                if line.startswith('--- ') and 'purify' in line:
                    flag = True
                    used_list.append(int(folder[folder.find('_')+1:folder.rfind('_')]))
                    break
    used_str = ','.join([str(i) for i in sorted(used_list)])
    unused_list = []
    for id in target:
        if not os.path.exists(f'{root_path}/{pro}/{pro}_{id}_buggy'):
            continue
        if id not in used_list:
            unused_list.append(id)
    unused_str = ','.join([str(i) for i in sorted(unused_list)])
    print(f'{pro}:{used_str}')
    print(f'{pro}-unused:{unused_str}')


def purification_cover_original(root_path, pro, target):
    for folder in os.listdir(f'{root_path}/{pro}'):
        if not os.path.isdir(f'{root_path}/{pro}/{folder}'):
            continue
        version = folder[folder.find('_')+1:folder.rfind('_')]
        if int(version) not in target:
            continue
        root = f'{root_path}/{pro}/{folder}'
        loc_root = '../tracing/resources/d4j-info/src_path'
        info = f'{loc_root}/{pro}/{version}.txt'
        original = ''
        count = 0
        with open(info, 'r') as f:
            for line in f:
                count += 1
                if count == 3:
                    original = line.strip()
                    break
        purified = f'{original}_purify'
        # delete original
        shutil.rmtree(f'{root}/{original}')
        os.rename(f'{root}/{original}_ori', f'{root}/{original}_ori_bak')
        print(f'{pro} {version} successfully delete and rename')
        # rename purified
        os.rename(f'{root}/{purified}', f'{root}/{original}')
        print(f'{pro} {version} successfully cover')


def original_cover_instr(root_path, pro, target):
      for folder in os.listdir(f'{root_path}/{pro}'):
        if not os.path.isdir(f'{root_path}/{pro}/{folder}'):
            continue
        version = folder[folder.find('_')+1:folder.rfind('_')]
        if not version.isdigit() or int(version) not in target:
            continue
        root = f'{root_path}/{pro}/{folder}'
        loc_root = '../tracing/resources/d4j-info/src_path'
        info = f'{loc_root}/{pro}/{version}.txt'
        original = ''
        count = 0
        with open(info, 'r') as f:
            for line in f:
                count += 1
                if count == 1:
                    original = line.strip()
                    break
        before_instr = f'{original}_ori'
        # delete original
        os.rename(f'{root}/{original}', f'{root}/{original}_instr')
        os.rename(f'{root}/{original}_ori', f'{root}/{original}')
        print(f'{pro} {version} successfully rename')


def check_trace_exist(root_path, pro, target):
    invalid = []
    for version in target:
        if not os.path.exists(f'{root_path}/{pro}/{pro}_{version}'):
            continue
        trace_path = f'{root_path}/{pro}/{pro}_{version}/trace.out'
        if not os.path.exists(trace_path):
            invalid.append(version)
    return invalid


# write failed_tests_afterTP/pro/version.txt
def collect_failed_tests_afterTP(root_path, pro, version, writer):
    # extract from pro_version_buggy/purified_failing_tests.txt or failing_tests for Mockito
    purified_failing_tests = f'{root_path}/{pro}/{pro}_{version}_buggy/purified_failing_tests.txt'
    if pro == 'mockito':
        purified_failing_tests = f'{root_path}/{pro}/{pro}_{version}_buggy/failing_tests'
    if os.path.exists(purified_failing_tests):
        ft_list = []
        with open(purified_failing_tests, 'r', encoding='utf-8') as f:
            for line in f:
                if line.startswith('--- '):
                    ft = line.split(' ')[1].strip()
                    ft_list.append(ft)
        with open(writer, 'w') as f:
            f.write('\n'.join(ft_list))
        print(f'Finish writing {writer}')
    else:
        print(f'[ERROR] {purified_failing_tests} not found')


# write all_tests_afterPurified/pro/version.txt
def collect_all_tests_afterPurified(root_path, pro, version, writer):
    # extract from pro_version_buggy/purified_all_tests.txt or all_tests for Mockito
    purified_all_tests = f'{root_path}/{pro}/{pro}_{version}_buggy/purified_all_tests.txt'
    if pro == 'mockito':
        purified_all_tests = f'{root_path}/{pro}/{pro}_{version}_buggy/all_tests'
    if os.path.exists(purified_all_tests):
        cmd_cp = f'cp -r {purified_all_tests} {writer}'
        os.system(cmd_cp)
        print(f'Finish writing {writer}')
    else:
        print(f'[ERROR] {purified_all_tests} not found')


def count_failing_passing_test_by_topN_method(root_path, pro, version, writer):
    mid_path = f'{root_path}/{pro}/{pro}_{version}_buggy/instrumented_method_id.txt'
    # load {mid:method} map
    method_by_mid = collections.OrderedDict()
    with open(mid_path, 'r') as f:
        for line in f:
            mid = line.split(':')[0]
            method_by_mid[mid] = line.strip()
    # load values
    values_path = f'{root_path}/{pro}/{pro}_{version}_buggy/logs/std.log'
    mid_buffer = set()
    ft_pt_count_by_mid = {}
    for mid in method_by_mid.keys():
        countmap = {'fail':0, 'pass':0}
        ft_pt_count_by_mid[mid] = countmap
    with open(values_path, 'r', encoding='utf8') as f:
        for line in f:
            line = line.strip()
            if line == 'PASS':
                for m in mid_buffer:
                    ft_pt_count_by_mid[m]['pass'] += 1
                mid_buffer.clear()
            elif line == 'FAIL':
                for m in mid_buffer:
                    ft_pt_count_by_mid[m]['fail'] += 1
                mid_buffer.clear()
            elif line[0].isdigit() and line[1] == '#':
                mid_buffer.add(line[0])
    # write test count
    with open(writer, 'w') as f:
        for mid in method_by_mid.keys():
            mname = method_by_mid[mid]
            ftcount, ptcount = 0, 0
            if mid in ft_pt_count_by_mid.keys():
                if 'fail' in ft_pt_count_by_mid[mid].keys():
                    ftcount = ft_pt_count_by_mid[mid]['fail']
                if 'pass' in ft_pt_count_by_mid[mid].keys():
                    ptcount = ft_pt_count_by_mid[mid]['pass']
            f.write(f'{mname}:{ptcount}:{ftcount}\n')
        print(f'test count finished {pro} {version}')


def check_topN_meet_test_limit(root_path, pro, version, grounttruth_root):
    testcount_path = f'{root_path}/{pro}/{version}.txt'
    grounttruth_path = f'{grounttruth_root}/{pro}/{version}.txt'
    gt_list = []
    with open(grounttruth_path, 'r') as f:
        for line in f:
            gt_list.append(line.strip())
    with open(testcount_path, 'r') as f:
        for line in f:
            mname = line.strip().split(':')[1]
            mid = int(line.strip().split(':')[0])
            ptcount = int(line.strip().split(':')[2])
            ftcount = int(line.strip().split(':')[3])
            if mname in gt_list:
                if ftcount > 0 and ptcount > 0 and (ftcount + ptcount > 2 ):
                    return True
                #if mid==0 and not (ftcount > 0 and ptcount > 0 and (ftcount + ptcount > 2 )):
                #    return True
    return False


def check_gt_in_topN_method(root_path, pro, version, grounttruth_root):
    testcount_path = f'{root_path}/{pro}/{version}.txt'
    grounttruth_path = f'{grounttruth_root}/{pro}/{version}.txt'
    gt_list = []
    with open(grounttruth_path, 'r') as f:
        for line in f:
            gt_list.append(line.strip())
    with open(testcount_path, 'r') as f:
        for line in f:
            mname = line.strip().split(':')[1]
            mid = int(line.strip().split(':')[0])
            if mname in gt_list:
                return int(mid+1)
    return -1


if __name__ == '__main__':
    root_path = '../d4jsrc'

    pros = idmap.keys()

### cover instr src
    for pro in pros:
        target = idmap[pro]
        original_cover_instr(root_path, pro, target)
    

### check gt in topn method
#    total_top1 = 0
#    total_top10 = 0
#    for pro in pros:
#        intop10_list = []
#        for version in idmap[pro]:
#            testcount_root = f'../testcount'
#            gt_method_root = f'../faulty_method_groundtruth'
#            if check_gt_in_topN_method(testcount_root, pro, version, gt_method_root) == 1:
#                total_top1 += 1
#            if check_gt_in_topN_method(testcount_root, pro, version, gt_method_root) != -1:
#                total_top10 += 1
#                intop10_list.append(str(version))
#        intop10_str = ','.join(intop10_list)
#        print(f'\'{pro}\':[{intop10_str}],')
#    print(f'top1 total:{total_top1}')
#    print(f'top10 total:{total_top10}')

### check test limit by topn method
#    total = 0
    #total_invalid = 0
#    total_valid = 0
#    for pro in pros:
#        top1_invalid_list = []
#        top10_valid_list = []
#        for version in idmap[pro]:
#            testcount_root = f'../testcount'
#            gt_method_root = f'../faulty_method_groundtruth'
#            if check_topN_meet_test_limit(testcount_root, pro, version, gt_method_root):
                #top1_invalid_list.append(str(version))
#                top10_valid_list.append(str(version))
#        total += len(idmap[pro])
#        #total_invalid += len(top1_invalid_list)
#        total_valid += len(top10_valid_list)
        #invalid_str = ','.join(top1_invalid_list)
#        valid_str = ','.join(top10_valid_list)
        #print(f'{pro}({len(top1_invalid_list)}):{invalid_str}')
#        print(f'\'{pro}\':[{valid_str}],')
#    print(f'valid/total:{total_valid}/{total}')

### count fail/pass test by topn method
#    for pro in pros:
#        for version in idmap[pro]:
#            testcount_root = f'../testcount/{pro}'
#            if not os.path.exists(testcount_root):
#                os.makedirs(testcount_root)
#            testcount_path = f'{testcount_root}/{version}.txt'
#            count_failing_passing_test_by_topN_method(root_path, pro, version, testcount_path)

### collect all_tests and failed_tests
#    for pro in pros:
#        all_tests_root = f'../all_tests_afterPurified/{pro}'
#        failing_tests_root = f'../failed_tests_afterTP/{pro}'
#        if not os.path.exists(all_tests_root):
#            os.makedirs(all_tests_root)
#        if not os.path.exists(failing_tests_root):
#            os.makedirs(failing_tests_root)
#        for version in idmap[pro]:
#            root_path = '../d4jsrc'
#            all_tests_by_version = f'{all_tests_root}/{version}.txt'
#            collect_all_tests_afterPurified(root_path, pro, version, all_tests_by_version)
#            failing_tests_by_version = f'{failing_tests_root}/{version}.txt'
#            collect_failed_tests_afterTP(root_path, pro, version, failing_tests_by_version)


    #    trace_root = 'D:\expdata\PDA-trace\info'
    #    invalid_str = ','.join([str(i) for i in sorted(check_trace_exist(trace_root, pro, target))])
    #    print(f'{pro}:{invalid_str}')
