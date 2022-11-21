import sys
import os

cap_pro = sys.argv[1]
pro = cap_pro.lower()
version = sys.argv[2]

id_map = {"compress": [3,4,5,6,9,12,15,41,46],
          "gson": [1,4,5,6,12,15,17],
          "codec": [2,3,4,5,6,7,8,9,10,11],
          "csv": [3,5,8,11,16],
          "lang": [6,7,10,11,12,13,16,17,19,28,44,46,47,52,53,54,55,58,59,61,62,63,64],
          "jacksonxml": [1,2,3,5],
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

if int(version) not in id_map[pro]:
    print(f'Please enter the legal version id.')
    exit(1)

# get project folder
if not os.path.exists(f'/home/d4jsrc/{pro}'):
	os.system(f'mkdir /home/d4jsrc/{pro}')
os.system(f'defects4j checkout -p {cap_pro} -v {version}b -w /home/d4jsrc/{pro}/{pro}_{version}_buggy')
os.system(f'cd /home/d4jsrc/{pro}/{pro}_{version}_buggy && defects4j test')

# purification
os.system(f'cd /home/purification && java -jar SimFix-1.0-SNAPSHOT-runnable.jar -home /home/d4jsrc/ -proj {pro} -id {version}')

# cover original file and colllect tests
os.system(f'python3.6 /home/d4jsrc/purifCover.py {pro} {version}')
os.system(f'cd /home/d4jsrc/{pro}/{pro}_{version}_buggy && defects4j test')
os.system(f'python3.6 /home/d4jsrc/collectTests.py {pro} {version}')

# tracing
os.system(f'cd /home/tracing && timeout 15m java -jar PDA-1.0-SNAPSHOT-runnable.jar trace -dir /home/d4jsrc -name {pro} -id {version}')

# slicing
os.system(f'cd /home/code/VBFL && java -jar fl-slicer.jar {pro} {version}')

# instrumentation
os.system(f'cd /home/code/VBFL && java -jar fl-runner.jar -dir /home/d4jsrc/{pro}/{pro}_{version}_buggy -name {pro} -id {version} -slice /home/topN_traceLineNo_noOrder/{pro}/{pro}_{version}/traceLineByTopN.txt -mode PDAtrace -range 10')
# test to collect values
os.system(f'cd /home/d4jsrc/{pro}/{pro}_{version}_buggy && defects4j test')

# split values
os.system(f'python3.6 /home/d4jsrc/collectValues.py {pro} {version}')

# build tree
os.system(f'python3.6 /home/d4jsrc/orgiCover.py {pro} {version}')
os.system(f'cd /home/code/VBFL && java -jar fl-gentree.jar {pro} {version} 0.8')
exit(0)


