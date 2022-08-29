package fl.utils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author:
 * @time: 2021/7/30 16:49
 */
public class SearchFile {
    private String _name = "@SearchFile ";

    static int countFiles = 0;
    static int countFolders = 0;

    public static File[] searchFileByName(File projectDir, final String fileName) {
        File[] subFolders = projectDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isFile()) { // is a file
                    countFiles++;
                }
                else { // is a folder
                    countFolders++;
                }
                if (pathname.isDirectory()
                        || (pathname.isFile() && pathname.getName().equals(fileName))) { // file.getName() returns the file name without path info
                    return true;
                }
                return false;
            }
        });
        List<File> result = new ArrayList<>();
        for (int i = 0; i < subFolders.length; i++) {
            if (subFolders[i].isFile()) {
                result.add(subFolders[i]);
            } else {
                File[] foldResult = searchFileByName(subFolders[i], fileName);
                for (int j = 0; j < foldResult.length; j++) {
                    result.add(foldResult[j]);
                }
            }
        }
        File files[] = new File[result.size()];
        result.toArray(files);
        return files;
    }

    public static File[] searchDirByName(File searchDir, final String targetName) {
        File[] subFolders = searchDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isFile()) { // is a file
                    countFiles++;
                }
                else { // is a folder
                    countFolders++;
                }
                if (pathname.isDirectory()
                        || (pathname.isDirectory() && pathname.getName().equals(targetName))) { // file.getName() returns the file name without path info
                    return true;
                }
                return false;
            }
        });
        List<File> result = new ArrayList<>();
        for (int i = 0; subFolders != null && i < subFolders.length; i++) {
            if (subFolders[i].getName().equals(targetName)) {
                result.add(subFolders[i]);
            } else {
                File[] foldResult = searchDirByName(subFolders[i], targetName);
                for (int j = 0; j < foldResult.length; j++) {
                    result.add(foldResult[j]);
                }
            }
        }
        File files[] = new File[result.size()];
        result.toArray(files);
        return files;
    }
}
